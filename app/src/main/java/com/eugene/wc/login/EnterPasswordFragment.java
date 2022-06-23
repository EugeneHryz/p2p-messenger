package com.eugene.wc.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.fragment.BaseFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import static com.eugene.wc.login.StartupViewModel.*;

import javax.inject.Inject;

public class EnterPasswordFragment extends BaseFragment implements TextWatcher {

    private static final String TAG = EnterPasswordFragment.class.getName();

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private StartupViewModel viewModel;

    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordInput;

    private Button signInButton;

    @Override
    protected void injectFragment(ActivityComponent activityComponent) {
        activityComponent.inject(this);
        viewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(StartupViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.enter_password_fragment, container, false);

        passwordInputLayout = view.findViewById(R.id.password_entry_input_layout);
        passwordInput = view.findViewById(R.id.password_entry);
        passwordInput.addTextChangedListener(this);

        signInButton = view.findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(v -> signIn());

        viewModel.getState().observe(getViewLifecycleOwner(), this::handleStateChange);

        return view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (passwordInputLayout.getError() != null) {
            passwordInputLayout.setError(null);
        }
    }

    private void signIn() {
        String password = passwordInput.getText().toString();
        passwordInput.getText().clear();

        viewModel.signIn(password);
    }

    private void handleStateChange(State state) {
        if (state == State.SIGN_IN_FAILED) {
            signInButton.setEnabled(true);
            passwordInputLayout.setEnabled(true);

            passwordInputLayout.setError(getString(R.string.sign_in_failed));
        } else if (state == State.SIGNING_IN) {
            signInButton.setEnabled(false);
            passwordInputLayout.setEnabled(false);
        }
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }
}
