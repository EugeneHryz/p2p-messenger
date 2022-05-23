package com.eugene.wc.protocol.contact;

import com.eugene.wc.protocol.api.contact.ContactManager;

import dagger.Module;
import dagger.Provides;

@Module
public class ContactModule {

    @Provides
    public ContactManager provideContactManager(ContactManagerImpl contactManager) {
        return contactManager;
    }
}
