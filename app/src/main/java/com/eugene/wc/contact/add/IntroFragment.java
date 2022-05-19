package com.eugene.wc.contact.add;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.RequestCode;
import com.eugene.wc.fragment.BaseFragment;
import com.eugene.wc.protocol.api.keyexchange.Payload;

import javax.inject.Inject;

public class IntroFragment extends BaseFragment {

    private static final String TAG = IntroFragment.class.getName();

    @Override
    protected void injectFragment(ActivityComponent activityComponent) {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_contact_intro_fragment, container, false);

        setupActionBar(view);

        Button startButton = view.findViewById(R.id.start_btn);
        startButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkPermissions();
            }
        });
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == RequestCode.REQUEST_PERMISSIONS && grantResults.length > 1) {
            if (grantResults[0] == 0 && grantResults[1] == 0) {
                requestDiscoverability();
            }
        }
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA}, RequestCode.REQUEST_PERMISSIONS);
        } else {
            requestDiscoverability();
        }
    }

    private void requestDiscoverability() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        requireActivity().startActivityForResult(intent, RequestCode.REQUEST_DISCOVERABLE);
    }

    private void setupActionBar(View fragmentView) {
        Toolbar toolbar = fragmentView.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        ActionBar ab = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
        }
    }
}
