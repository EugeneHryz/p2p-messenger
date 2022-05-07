package com.eugene.wc.signup;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator.Strength;
import com.eugene.wc.protocol.api.account.UserConstants;
import com.eugene.wc.view.PasswordStrengthTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

public class SetPasswordFragment extends BaseSignUpFragment implements TextWatcher {

    private static final String TAG = SetPasswordFragment.class.getName();

    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordInput;

    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText confirmPasswordInput;

    private PasswordStrengthTextView passwordStrengthTV;
    private Button nextButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.set_password_fragment, container, false);

        passwordInputLayout = view.findViewById(R.id.password_entry_input_layout);
        passwordInput = view.findViewById(R.id.password_entry);
        confirmPasswordInputLayout = view.findViewById(R.id.confirm_password_input_layout);
        confirmPasswordInput = view.findViewById(R.id.confirm_password);

        passwordStrengthTV = view.findViewById(R.id.password_strength_text);

        passwordInput.addTextChangedListener(this);
        confirmPasswordInput.addTextChangedListener(this);

        nextButton = view.findViewById(R.id.next_btn);
        nextButton.setOnClickListener(v -> setPassword());

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
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        boolean enoughLength = password.length() >= UserConstants.MIN_PASSWORD_LENGTH;

        Strength strength = viewModel.estimatePasswordStrength(password);
        boolean enoughStrength = strength.compareTo(Strength.WEAK) >= 0;
        boolean passwordsAreEqual = password.equals(confirmPassword);

        passwordInputLayout.setError(enoughLength ? null : getString(R.string.password_too_short));
        if (enoughLength) {
            passwordInputLayout.setError(enoughStrength ? null : getString(R.string.password_too_weak));
        }
        confirmPasswordInputLayout.setError(passwordsAreEqual ? null : getString(R.string.passwords_not_equal));
        passwordStrengthTV.setPasswordStrengthText(strength);

        boolean enabled = enoughLength && enoughStrength && passwordsAreEqual;
        nextButton.setEnabled(enabled);
    }

    private void setPassword() {
        String password = passwordInput.getText().toString();
        viewModel.createAccount(password);
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }
}