package com.eugene.wc.signup;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;
import com.eugene.wc.signup.SignUpViewModel.State;

import javax.inject.Inject;

public class SignUpActivity extends BaseActivity {

    private static final String TAG = SignUpActivity.class.getName();

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private SignUpViewModel viewModel;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(SignUpViewModel.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);

        viewModel.getState().observe(this, this::handleStateChange);

        showInitialFragment(new EnterNicknameFragment());
    }

    private void handleStateChange(State state) {
        if (state == State.SET_PASSWORD) {
            showNextFragment(new SetPasswordFragment());
        } else if (state == State.CREATED) {
            setResult(RESULT_OK);
            finish();
        }
    }
}