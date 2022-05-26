package com.eugene.wc.protocol.contact;

import com.eugene.wc.protocol.api.contact.ContactManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ContactModule {

    public static class EagerSingletons {
        @Inject
        ContactManager contactManager;
    }

    @Singleton
    @Provides
    public ContactManager provideContactManager(ContactManagerImpl contactManager) {
        return contactManager;
    }
}
