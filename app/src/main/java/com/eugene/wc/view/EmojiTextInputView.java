package com.eugene.wc.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import com.eugene.wc.R;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

public class EmojiTextInputView extends LinearLayout {

    private static final String TAG = EmojiTextInputView.class.getName();

    private final EmojiPopup emojiPopup;

    private final EmojiEditText emojiEditText;
    private final AppCompatImageButton emojiToggleBtn;

    public EmojiTextInputView(Context context) {
        this(context, null);
    }

    public EmojiTextInputView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmojiTextInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.emoji_text_input, this, true);

        emojiEditText = findViewById(R.id.emoji_edit_text);
        emojiToggleBtn = findViewById(R.id.emoji_toggle);

        emojiPopup = EmojiPopup.Builder
                .fromRootView(getRootView())
                .setOnEmojiPopupShownListener(this::onEmojiPopupShown)
                .setOnEmojiPopupDismissListener(this::onEmojiPopupDismiss)
                .setKeyboardAnimationStyle(com.vanniktech.emoji.R.style.emoji_fade_animation_style)
                .build(emojiEditText);

        emojiToggleBtn.setOnClickListener(v -> emojiPopup.toggle());
        emojiEditText.setOnClickListener(v -> {
            if (emojiPopup.isShowing()) emojiPopup.dismiss();
        });
    }

    public Editable getText() {
        return emojiEditText.getText();
    }

    public void addTextWatcher(TextWatcher watcher) {
        emojiEditText.addTextChangedListener(watcher);
    }

    private void onEmojiPopupShown() {
        emojiToggleBtn.setImageResource(R.drawable.ic_keyboard);
    }

    private void onEmojiPopupDismiss() {
        emojiToggleBtn.setImageResource(R.drawable.ic_emoji);
    }
}
