package com.eugene.wc.view;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.widget.AppCompatImageButton;

public class TextSendController implements TextWatcher {

    public interface SendListener {
        void onSendMessage(String text);
    }

    private final AppCompatImageButton sendButton;
    private final EmojiTextInputView textInputView;

    private final SendListener listener;

    public TextSendController(AppCompatImageButton sendButton,
                              EmojiTextInputView textInputView,
                              SendListener listener) {
        this.sendButton = sendButton;
        this.textInputView = textInputView;
        this.listener = listener;

        textInputView.addTextWatcher(this);
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(v -> handleSendClick());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean enableSend = !isTextEmpty(s.toString());
        sendButton.setEnabled(enableSend);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void handleSendClick() {
        Editable editable = textInputView.getText();
        if (editable != null) {
            String inputText = editable.toString();

            listener.onSendMessage(inputText);
            editable.clear();
            sendButton.setEnabled(false);
        }
    }

    private boolean isTextEmpty(String text) {
        return text.isEmpty() || text.trim().isEmpty();
    }
}
