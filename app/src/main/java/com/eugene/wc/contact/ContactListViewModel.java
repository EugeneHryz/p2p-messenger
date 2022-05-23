package com.eugene.wc.contact;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.db.DbExecutor;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

public class ContactListViewModel extends ViewModel implements EventListener {

    private final ContactManager contactManager;
    private final Executor dbExecutor;
    private final EventBus eventBus;

    private final MutableLiveData<List<ContactItem>> contacts = new MutableLiveData<>();

    @Inject
    public ContactListViewModel(ContactManager cm, @DbExecutor Executor dbExecutor,
                                EventBus eventBus) {
        contactManager = cm;
        this.dbExecutor = dbExecutor;
        this.eventBus = eventBus;

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

    }

    public LiveData<List<ContactItem>> getContacts() {
        return contacts;
    }

    private List<ContactItem> convertContacts(List<Contact> contacts) {
        List<ContactItem> items = new ArrayList<>();
        for (Contact c : contacts) {
            items.add(new ContactItem(c.getName()));
        }
        return items;
    }
}
