package com.eugene.wc.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.eugene.wc.R;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;

public class PasswordStrengthTextView extends AppCompatTextView {

    public PasswordStrengthTextView(Context context) {
        super(context);
    }

    public PasswordStrengthTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PasswordStrengthTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPasswordStrengthText(PasswordStrengthEstimator.Strength strength) {

        switch (strength) {
            case VERY_WEAK:
                setText(R.string.very_weak_password);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.MessengerRedText);
                } else {
                    setTextColor(getResources().getColor(R.color.wc_red));
                }
                break;
            case WEAK:
                setText(R.string.weak_password);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.MessengerOrangeText);
                } else {
                    setTextColor(getResources().getColor(R.color.wc_orange));
                }
                break;
            case MEDIUM:
                this.setText(R.string.medium_password);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.MessengerYellowText);
                } else {
                    setTextColor(getResources().getColor(R.color.wc_yellow));
                }
                break;
            case STRONG:
                setText(R.string.strong_password);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.MessengerLightGreenText);
                } else {
                    setTextColor(getResources().getColor(R.color.wc_light_green));
                }
                break;
            case VERY_STRONG:
                this.setText(R.string.very_strong_password);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.MessengerGreenText);
                } else {
                    setTextColor(getResources().getColor(R.color.wc_green));
                }
                break;
            default:
        }
    }
}
