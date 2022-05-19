package com.eugene.wc.protocol.crypto;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.eugene.wc.protocol.api.crypto.AuthenticatedCipher;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.CryptoExecutor;
import com.eugene.wc.protocol.api.crypto.DHKeyExchange;
import com.eugene.wc.protocol.api.crypto.KeyExchangeCrypto;
import com.eugene.wc.protocol.api.crypto.PasswordBasedKdf;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public class CryptoModule {

    private static final int MAX_POOL_SIZE = 8;

    private final ExecutorService cryptoExecutor;

    public CryptoModule() {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        RejectedExecutionHandler policy = new ThreadPoolExecutor.DiscardPolicy();
        cryptoExecutor = new ThreadPoolExecutor(0, MAX_POOL_SIZE,
                60, SECONDS, queue, policy);
    }

    @Provides
    @Singleton
    @CryptoExecutor
    public Executor provideCryptoExecutor(LifecycleManager lifecycleManager) {
        lifecycleManager.registerForShutdown(cryptoExecutor);
        return cryptoExecutor;
    }

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
