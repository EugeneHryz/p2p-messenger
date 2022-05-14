package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public class CryptoModule {

    @Provides
    public PasswordBasedKdf providePasswordBasedKdf() {
        return new HmacPasswordBasedKdf();
    }

    @Provides
    public AuthenticatedCipher provideAuthenticatedCipher() {
        return new AesHmacAuthenticatedCipher();
    }

    @Provides
    public DHKeyExchange provideDHKeyExchange() {
        return new DHKeyExchangeImpl();
    }

    @Singleton
    @Provides
    public CryptoComponent provideCryptoComponent(AuthenticatedCipher authCipher,
                                                  PasswordBasedKdf pbkdf,
                                                  DHKeyExchange dhKeyExchange) {
        return new CryptoComponentImpl(authCipher, pbkdf, dhKeyExchange);
    }

    @Provides
    public KeyExchangeCrypto provideKeyExchangeCrypto(KeyExchangeCryptoImpl kec) {
        return kec;
    }
}
