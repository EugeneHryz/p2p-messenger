package com.eugene.wc.login;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.io.IoExecutor;

import java.util.concurrent.Executor;

import javax.inject.Inject;

public class StartupViewModel extends ViewModel {

    private static final String TAG = StartupViewModel.class.getName();

    private final AccountManager accountManager;
    private final Executor ioExecutor;

    enum State {
        SIGN_IN_FAILED,
        SIGNED_IN,
        SIGNED_OUT
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();

    @Inject
    public StartupViewModel(AccountManager accountManager, @IoExecutor Executor ioExecutor) {
        this.accountManager = accountManager;
        this.ioExecutor = ioExecutor;
    }

    public boolean accountExists() {
        return accountManager.accountExists();
    }

    public void signIn(String password) {
        ioExecutor.execute(() -> {
            try {
                accountManager.signIn(password);
                state.postValue(State.SIGNED_IN);

            } catch (DecryptionException e) {
                Log.i(TAG, "Unable to decrypt key", e);
                state.postValue(State.SIGN_IN_FAILED);
            }
        });

    }

    public MutableLiveData<State> getState() {
        return state;
    }
}
