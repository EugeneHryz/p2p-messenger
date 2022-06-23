package com.eugene.wc.protocol.conversation;

import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.conversation.ContactConversation;
import com.eugene.wc.protocol.api.conversation.ConversationManager;
import com.eugene.wc.protocol.api.conversation.ConversationMessageListener;
import com.eugene.wc.protocol.api.conversation.ConversationTextMessage;
import com.eugene.wc.protocol.api.conversation.MessageQueue;
import com.eugene.wc.protocol.api.conversation.exception.ConversationException;
import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.session.MessageId;

import static com.eugene.wc.protocol.api.conversation.ConversationConstants.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ConversationManagerImpl implements ConversationManager {

    private static final Logger logger = Logger.getLogger(ConversationManagerImpl.class.getName());

    private final Object lock = new Object();
    private final List<ContactConversation> contactConversations = new ArrayList<>();

    private final ContactGroupFactory contactGroupFactory;
    private final MessageFactory messageFactory;
    private final ClientHelper clientHelper;
    private final DatabaseComponent db;

    @Inject
    public ConversationManagerImpl(ContactGroupFactory contactGroupFactory,
                                   MessageFactory messageFactory,
                                   ClientHelper clientHelper,
                                   DatabaseComponent db) {
        this.contactGroupFactory = contactGroupFactory;
        this.messageFactory = messageFactory;
        this.clientHelper = clientHelper;
        this.db = db;
    }

    @Override
    public ContactConversation registerConversation(Contact contact) {
        Group contactGroup = contactGroupFactory.createContactGroup(CLIENT_ID, contact);
        try {
            db.runInTransaction(false, (txn) -> db.addGroup(txn, contactGroup));
        } catch (DbException e) {
            logger.warning("Unable to create contact group\n" + e);
            return null;
        }

        ContactConversation conversation = new ContactConversationImpl(contact, new MessageQueue(), contactGroup);
        synchronized (lock) {
            contactConversations.add(conversation);
        }
        return conversation;
    }

    @Override
    public ContactConversation getContactConversation(ContactId contactId) {
        for (ContactConversation c : contactConversations) {
            if (c.getContactId().equals(contactId)) {
                return c;
            }
        }
        return null;
    }

    public class ContactConversationImpl implements ContactConversation {

        private final Logger logger = Logger.getLogger(ContactConversation.class.getName());

        private final MessageQueue messagesToSend;
        private final Group contactGroup;
        private final Contact contact;

        private ConversationMessageListener messageListener;

        public ContactConversationImpl(Contact contact,
                                       MessageQueue messagesToSend,
                                       Group contactGroup) {
            this.contact = contact;
            this.contactGroup = contactGroup;
            this.messagesToSend = messagesToSend;
        }

        public ConversationTextMessage sendTextMessage(String msgText) throws ConversationException {
            ConversationTextMessage textMessage = new ConversationTextMessage(msgText);

            byte[] encodedBody;
            try {
                encodedBody = encodeTextMessage(textMessage);
            } catch (FormatException e) {
                logger.warning("Unable to encode text message\n" + e);
                throw new ConversationException("Unable to encode text message", e);
            }
            OffsetDateTime odt = OffsetDateTime.now(ZoneId.systemDefault());
            long epochTime = odt.toEpochSecond();

            Message message = messageFactory.createMessage(contactGroup.getId(), epochTime, encodedBody);
            try {
                storeTextMessage(message, false, odt);
            } catch (FormatException | DbException e) {
                logger.warning("Unable to save message in the db\n" + e);
                throw new ConversationException("Unable to save message in the db", e);
            }

            messagesToSend.addMessage(message);

            textMessage.setMessageId(message.getId());
            textMessage.setTime(odt.toLocalDateTime());
            textMessage.setIncoming(false);
            return textMessage;
        }

        @Override
        public void onMessageReceived(Message message) {
            try {
                OffsetDateTime odt = OffsetDateTime.now(ZoneId.systemDefault());

                ConversationTextMessage textMessage = decodeTextMessage(message.getBody());
                textMessage.setIncoming(true);
                textMessage.setMessageId(message.getId());
                textMessage.setTime(odt.toLocalDateTime());

                storeTextMessage(message, true, odt);

                if (messageListener != null) {
                    messageListener.messageReceived(textMessage);
                }
            } catch (FormatException | DbException e) {
                logger.warning("Unable to receive message\n" + e);
                if (messageListener != null) {
                    messageListener.failedToReceiveMessage(e);
                }
            }
        }

        @Override
        public List<ConversationTextMessage> getMessageHistory() throws ConversationException {
            List<ConversationTextMessage> messages = new ArrayList<>();
            try {
                Map<Message, WdfDictionary2> msgWithMetadata = db.runInTransactionWithResult(true,
                        (txn) -> {
                            Map<Message, WdfDictionary2> messagesWithMetadata = new HashMap<>();
                            List<MessageId> messageIds = db.getMessageIds(txn, contactGroup.getId());
                            for (MessageId id : messageIds) {
                                Message msg = db.getMessage(txn, id);
                                WdfDictionary2 metadata = clientHelper
                                        .getMessageMetadataAsDictionary(txn, id);

                                messagesWithMetadata.put(msg, metadata);
                            }
                            return messagesWithMetadata;
                        });

                for (Map.Entry<Message, WdfDictionary2> e : msgWithMetadata.entrySet()) {
                    messages.add(convert(e.getKey(), e.getValue()));
                }
            } catch (DbException | FormatException e) {
                logger.warning("Unable to get message history for a contact\n" + e);
                throw new ConversationException("Unable to get message history for a contact", e);
            }
            return messages;
        }

        private ConversationTextMessage convert(Message msg, WdfDictionary2 metadata) throws FormatException {
            boolean isIncoming = metadata.getBoolean(MSG_KEY_IS_INCOMING);

            long timestamp = msg.getTimestamp();
            if (metadata.containsKey(MSG_KEY_RECEIVED_TIME)) {
                timestamp = metadata.getLong(MSG_KEY_RECEIVED_TIME);
            }
            byte[] msgRaw = msg.getBody();
            WdfList2 bodyAsList = clientHelper.toList(msgRaw);
            String text = bodyAsList.getString(0);

            LocalDateTime time = LocalDateTime.ofEpochSecond(timestamp, 0,
                    OffsetDateTime.now().getOffset());
            ConversationTextMessage textMsg = new ConversationTextMessage(isIncoming, text, time);
            textMsg.setMessageId(msg.getId());
            return textMsg;
        }

        @Override
        public ContactId getContactId() {
            return contact.getId();
        }

        @Override
        public MessageQueue getMessageQueue() {
            return messagesToSend;
        }

        @Override
        public void setMessageListener(ConversationMessageListener l) {
            messageListener = l;
        }

        private void storeTextMessage(Message msg, boolean incoming, OffsetDateTime receivedTime)
                throws FormatException, DbException {
            WdfDictionary2 meta = new WdfDictionary2();
            meta.put(MSG_KEY_IS_INCOMING, incoming);
            if (incoming) {
                meta.put(MSG_KEY_RECEIVED_TIME, receivedTime.toEpochSecond());
            }
            clientHelper.addLocalMessage(msg, meta, true);
        }

        private byte[] encodeTextMessage(ConversationTextMessage textMsg) throws FormatException {
            WdfList2 list = new WdfList2();
            list.add(textMsg.getText());

            return clientHelper.toByteArray(list);
        }

        private ConversationTextMessage decodeTextMessage(byte[] rawBytes) throws FormatException {
            WdfList2 list = clientHelper.toList(rawBytes);
            String text = list.getString(0);

            return new ConversationTextMessage(text);
        }
    }
}
