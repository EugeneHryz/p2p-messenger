package com.eugene.wc.protocol.crypto;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;

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
        return new AesWithHmacAuthenticatedCipher();
    }

    @Provides
    public CryptoComponent provideCryptoComponent(AuthenticatedCipher authCipher,
                                                  PasswordBasedKdf pbkdf) {
        return new CryptoComponentImpl(authCipher, pbkdf);
    }
}
