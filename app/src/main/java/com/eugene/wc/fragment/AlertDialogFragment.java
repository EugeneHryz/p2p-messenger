package com.eugene.wc.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eugene.wc.R;

public class AlertDialogFragment extends DialogFragment {

    public static final String TAG = AlertDialogFragment.class.getName();

    public static final String DIALOG_MESSAGE_KEY = "dialog_message_key";
    public static final String DIALOG_TITLE_KEY = "dialog_title_key";

    private final DialogResultListener listener;

    public AlertDialogFragment(DialogResultListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int messageResId = R.string.dialog_generic_error_msg;
        int titleResId = R.string.dialog_title_alert;
        Bundle args = getArguments();
        if (args != null) {
            messageResId = args.getInt(DIALOG_MESSAGE_KEY);
            titleResId = args.getInt(DIALOG_TITLE_KEY);
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(R.string.dialog_ok_button, (dialog, which) -> {});

        return dialogBuilder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        listener.onDialogDismissed();
        super.onDismiss(dialog);
    }
}
