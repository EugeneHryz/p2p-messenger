package com.eugene.wc.signup;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator.Strength;
import com.eugene.wc.protocol.api.crypto.exception.EncryptionException;
import com.eugene.wc.protocol.api.io.IoExecutor;

import java.util.concurrent.Executor;

import javax.inject.Inject;

public class SignUpViewModel extends ViewModel {

    private static final String TAG = SignUpViewModel.class.getName();

    private final AccountManager accountManager;
    private final PasswordStrengthEstimator passwordStrengthEstimator;
    private final Executor ioExecutor;

    private String nickname;

    enum State {
        SET_NICKNAME,
        SET_PASSWORD,
        CREATED,
        FAILED
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();

    @Inject
    public SignUpViewModel(AccountManager accountManager, PasswordStrengthEstimator psEstimator,
                           @IoExecutor Executor executor) {
        this.accountManager = accountManager;
        ioExecutor = executor;
        passwordStrengthEstimator = psEstimator;

//        ioExecutor.execute(() -> {
//            if (!accountManager.accountExists()) {
//                state.postEvent(State.SET_NICKNAME);
//            }
//        });
    }

    public void createAccount(String password) {
        if (nickname == null) {
            throw new AssertionError("Nickname and password aren't set");
        }
        ioExecutor.execute(() -> {
            try {
                accountManager.createAccount(nickname, password);
                state.postValue(State.CREATED);

            } catch (EncryptionException e) {
                Log.e(TAG, "Unable to create account", e);
                state.postValue(State.FAILED);
            }
        });
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        state.setValue(State.SET_PASSWORD);
    }

    public Strength estimatePasswordStrength(String password) {
        // need to run on bg thread?
        return passwordStrengthEstimator.estimateStrength(password);
    }

    public MutableLiveData<State> getState() {
        return state;
    }
}
