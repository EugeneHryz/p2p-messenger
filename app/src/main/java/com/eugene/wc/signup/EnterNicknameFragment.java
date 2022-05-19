package com.eugene.wc.signup;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eugene.wc.R;
import com.eugene.wc.protocol.api.account.UserConstants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EnterNicknameFragment extends BaseSignUpFragment implements TextWatcher {

    private static final String TAG = EnterNicknameFragment.class.getName();

    private TextInputLayout textInputLayout;
    private TextInputEditText editText;

    private Button nextButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.enter_nickname_fragment, container, false);
        editText = view.findViewById(R.id.nickname_entry);
        textInputLayout = view.findViewById(R.id.nickname_entry_input_layout);
        nextButton = view.findViewById(R.id.next_btn);

        editText.addTextChangedListener(this);
        nextButton.setOnClickListener(v -> saveNickname());

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
        if (s.length() >= UserConstants.MIN_NICKNAME_LENGTH) {
            textInputLayout.setError(null);
            nextButton.setEnabled(true);
        } else {
            textInputLayout.setError(getString(R.string.nickname_too_short));
            nextButton.setEnabled(false);
        }
    }

    private void saveNickname() {
        EditText editText = textInputLayout.getEditText();
        if (editText != null) {
            viewModel.setNickname(editText.getText().toString());
        }
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }
}
