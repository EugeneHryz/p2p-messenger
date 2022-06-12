package com.eugene.wc.contact;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.db.DbExecutor;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.plugin.event.ContactConnectedEvent;
import com.eugene.wc.protocol.api.plugin.event.ContactDisconnectedEvent;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

public class ContactListViewModel extends ViewModel implements EventListener {

    private static final String TAG = ContactListViewModel.class.getName();

    private final ContactManager contactManager;
    private final ConnectionRegistry connectionRegistry;
    private final Executor dbExecutor;
    private final EventBus eventBus;

    private final MutableLiveData<List<ContactItem>> contacts = new MutableLiveData<>();

    private final MutableLiveData<Predicate<ContactItem>> shouldUpdate = new MutableLiveData<>();

    @Inject
    public ContactListViewModel(ContactManager cm, @DbExecutor Executor dbExecutor,
                                EventBus eventBus, ConnectionRegistry connectionRegistry) {
        contactManager = cm;
        this.dbExecutor = dbExecutor;
        this.eventBus = eventBus;
        this.connectionRegistry = connectionRegistry;

        eventBus.addListener(this);
    }

    @Override
    protected void onCleared() {
        eventBus.removeListener(this);
    }

    public void loadContacts() {
        dbExecutor.execute(() -> {
            List<Contact> contacts = contactManager.getAllContacts();

            List<ContactItem> contactItems = convertContacts(contacts);
            this.contacts.postValue(contactItems);
        });
    }

    @Override
    public void onEventOccurred(Event e) {
        if (e instanceof ContactConnectedEvent) {
            ContactConnectedEvent event = (ContactConnectedEvent) e;
            Contact c = contactManager.getContactById(event.getContactId());

            updateContactStatus(c.getId(), true);

        } else if (e instanceof ContactDisconnectedEvent) {
            ContactDisconnectedEvent event = (ContactDisconnectedEvent) e;
            Contact c = contactManager.getContactById(event.getContactId());

            updateContactStatus(c.getId(), false);
        }
    }

    public LiveData<List<ContactItem>> getContacts() {
        return contacts;
    }

    public LiveData<Predicate<ContactItem>> getShouldUpdate() {
        return shouldUpdate;
    }

    private void updateContactStatus(ContactId contactId, boolean newStatus) {
        ContactItem contactToUpdate = null;
        List<ContactItem> contactItems = contacts.getValue();
        if (contactItems != null) {
            for (ContactItem item : contactItems) {
                if (contactId.equals(item.getId())) {
                    contactToUpdate = item;
                    break;
                }
            }
            if (contactToUpdate != null) {
                contactToUpdate.setStatus(newStatus);
                shouldUpdate.postValue((item) -> item.getId().equals(contactId));
            }
        }
     }

    private List<ContactItem> convertContacts(List<Contact> contacts) {
        List<ContactItem> items = new ArrayList<>();
        for (Contact c : contacts) {

            boolean status = connectionRegistry.isConnected(c.getId());
            items.add(new ContactItem(c.getId(), c.getIdentity().getName(), status));
        }
        return items;
    }
}
