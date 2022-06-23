package com.eugene.wc.conversation;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.R;
import com.eugene.wc.conversation.data.ConversationItem;
import com.eugene.wc.conversation.data.MessageConversationItem;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.conversation.ContactConversation;
import com.eugene.wc.protocol.api.conversation.ConversationManager;
import com.eugene.wc.protocol.api.conversation.ConversationMessageListener;
import com.eugene.wc.protocol.api.conversation.ConversationTextMessage;
import com.eugene.wc.protocol.api.conversation.exception.ConversationException;
import com.eugene.wc.protocol.api.db.DbExecutor;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.plugin.event.ContactDisconnectedEvent;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class ConversationViewModel extends ViewModel implements ConversationMessageListener,
        EventListener {

    private static final String TAG = ConversationViewModel.class.getName();

    private final ConversationManager conversationManager;
    private final ContactManager contactManager;
    private final Executor ioExecutor;
    private final Executor dbExecutor;
    private final EventBus eventBus;

    private volatile ContactId contactId;
    private volatile Contact contact;
    private volatile ContactConversation conversation;

    enum State {
        CONTACT_LOADED,
        CONTACT_DISCONNECTED
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();
    // todo:
    private final MutableLiveData<ConversationItem> incomingMessage = new MutableLiveData<>();

    private final MutableLiveData<List<ConversationItem>> storedMessages = new MutableLiveData<>();

    @Inject
    public ConversationViewModel(ContactManager contactManager,
                                 @DbExecutor Executor dbExecutor,
                                 @IoExecutor Executor ioExecutor,
                                 ConversationManager conversationManager,
                                 EventBus eventBus) {
        this.contactManager = contactManager;
        this.dbExecutor = dbExecutor;
        this.ioExecutor = ioExecutor;
        this.conversationManager = conversationManager;
        this.eventBus = eventBus;

        eventBus.addListener(this);
    }

    @Override
    protected void onCleared() {
        eventBus.removeListener(this);
    }

    public void loadContact(ContactId contactId) {
        this.contactId = contactId;
        dbExecutor.execute(() -> {
            try {
                contact = contactManager.getContactById(contactId);
                conversation = conversationManager.getContactConversation(contactId);
                if (conversation == null) {
                    conversation = conversationManager.registerConversation(contact);
                }
                conversation.setMessageListener(this);

                state.postValue(State.CONTACT_LOADED);
            } catch (DbException e) {
                Log.e(TAG, "Unable to load contact");
            }
        });
    }

    public void loadInitialMessages() {
        dbExecutor.execute(() -> {
            try {
                List<ConversationTextMessage> messages = conversation.getMessageHistory();
                List<ConversationItem> msgItems = messages.stream()
                        .map(this::convert)
                        .collect(Collectors.toList());
                storedMessages.postValue(msgItems);

            } catch (ConversationException e) {
                Log.e(TAG, "Unable to load message history", e);
            }
        });
    }

    public LiveData<MessageConversationItem> sendTextMessage(String text) {
        MutableLiveData<MessageConversationItem> result = new MutableLiveData<>();

        ioExecutor.execute(() -> {
            try {
                ConversationTextMessage textMsg = conversation.sendTextMessage(text);

                int layoutRes = textMsg.isIncoming() ? R.layout.in_msg_list_item :
                        R.layout.out_msg_list_item;
                MessageConversationItem msgItem = new MessageConversationItem(layoutRes, textMsg.getId(),
                        textMsg.getTime(), textMsg.isIncoming(), text);
                result.postValue(msgItem);

            } catch (ConversationException e) {
                Log.w(TAG, "Unable to send text message");
            }
        });
        return result;
    }

    @Override
    public void messageReceived(ConversationTextMessage msg) {
        MessageConversationItem msgItem = convert(msg);

        incomingMessage.postValue(msgItem);
    }

    @Override
    public void failedToReceiveMessage(Exception e) {
        Log.w(TAG, "Failed to receive message from contact: " +
                contact.getIdentity().getName(), e);
    }

    @Override
    public void onEventOccurred(Event e) {
        if (e instanceof ContactDisconnectedEvent) {
            ContactDisconnectedEvent event = (ContactDisconnectedEvent) e;
            if (event.getContactId().equals(contactId)) {
                state.postValue(State.CONTACT_DISCONNECTED);
            }
        }
    }

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<ConversationItem> getIncomingMessage() {
        return incomingMessage;
    }

    public LiveData<List<ConversationItem>> getStoredMessages() {
        return storedMessages;
    }

    public Contact getContact() {
        return contact;
    }

    private MessageConversationItem convert(ConversationTextMessage msg) {
        int layoutRes = msg.isIncoming() ? R.layout.in_msg_list_item :
                R.layout.out_msg_list_item;

        return new MessageConversationItem(layoutRes, msg.getId(), msg.getTime(),
                msg.isIncoming(), msg.getText());
    }
}
