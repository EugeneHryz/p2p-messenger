package com.eugene.wc.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;
import com.eugene.wc.activity.RequestCode;
import com.eugene.wc.home.HomeActivity;
import com.eugene.wc.signup.SignUpActivity;
import com.eugene.wc.login.StartupViewModel.State;

import javax.inject.Inject;

public class StartupActivity extends BaseActivity {

    private static final String TAG = StartupActivity.class.getName();

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private StartupViewModel viewModel;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(StartupViewModel.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.fragment_container);

        viewModel.getState().observe(this, this::handleStateChange);

        if (!viewModel.accountExists()) {
            Log.d(TAG, "account does not exist");
            startSignUpActivityForResult();
        } else {
            Log.d(TAG, "account does exist");
            showInitialFragment(new EnterPasswordFragment());
        }
    }

    private void handleStateChange(State state) {
        if (state == State.SIGNED_IN) {
            Log.d(TAG, "Successfully signed in :)");
            onSignedIn();
        } else {
            Log.d(TAG, "Error while trying to log in");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCode.SETUP_ACCOUNT) {
            Log.d(TAG, "recieved result: " + resultCode);

            if (resultCode == RESULT_OK) {
                onSignedIn();

            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User cancelled account creation");
                // show an error to a user
            }
        }
    }

    private void startSignUpActivityForResult() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivityForResult(intent, RequestCode.SETUP_ACCOUNT);
    }

    private void onSignedIn() {
        viewModel.startServices();
        startHomeActivity();
        supportFinishAfterTransition();
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
