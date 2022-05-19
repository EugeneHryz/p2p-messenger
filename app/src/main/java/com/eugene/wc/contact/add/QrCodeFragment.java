package com.eugene.wc.contact.add;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.fragment.BaseFragment;
import com.eugene.wc.qrcode.QrCodeDecoder;
import com.eugene.wc.qrcode.QrCodeUtils;
import com.eugene.wc.view.CameraView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.zxing.WriterException;
import com.eugene.wc.contact.add.AddContactViewModel.State;

import javax.inject.Inject;

public class QrCodeFragment extends BaseFragment {

    private static final String TAG = QrCodeFragment.class.getName();

    private CameraView cameraView;

    private CircularProgressIndicator progressIndicator;
    private TextView status;
    private ImageView qrCodeView;

    private Vibrator vibrator;

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AddContactViewModel viewModel;

    QrCodeDecoder qrCodeDecoder;

    @Override
    protected void injectFragment(ActivityComponent activityComponent) {
        activityComponent.inject(this);
        viewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
                .get(AddContactViewModel.class);
        qrCodeDecoder = new QrCodeDecoder(viewModel);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qr_code_fragment, container, false);

        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        cameraView = view.findViewById(R.id.camera_view);
        cameraView.setImageConsumer(qrCodeDecoder);

        progressIndicator = view.findViewById(R.id.progress_bar);
        status = view.findViewById(R.id.status);
        qrCodeView = view.findViewById(R.id.qr_code);

        viewModel.getEncodedLocalPayload().observe(getViewLifecycleOwner(), this::displayQrCode);
        viewModel.startAddingContact();

        viewModel.getState().observe(getViewLifecycleOwner(), this::handleStateChange);

        return view;
    }

    private void handleStateChange(State state) {
        if (state == State.STARTED) {
            qrCodeView.setVisibility(View.INVISIBLE);
            progressIndicator.setVisibility(View.VISIBLE);
            progressIndicator.animate();

        } else if (state == State.WAITING) {
            vibrate(500);
        }
    }

    private void displayQrCode(String data) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        try {
            Bitmap qrCode = QrCodeUtils.createQrCode(dm, data);
            qrCodeView.setImageBitmap(qrCode);

        } catch (WriterException e) {
            // how to handle exceptions in fragments/activities?
            Log.d(TAG, "Error while creating QR-code bitmap", e);
        }
    }

    private void vibrate(long duration) {
        int amp = 255;
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amp));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }
}
