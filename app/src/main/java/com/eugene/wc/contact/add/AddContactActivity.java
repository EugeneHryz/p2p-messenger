package com.eugene.wc.contact.add;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;
import com.eugene.wc.activity.RequestCode;

import javax.inject.Inject;

public class AddContactActivity extends BaseActivity {

    private static final String TAG = AddContactActivity.class.getName();

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AddContactViewModel viewModel;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(AddContactViewModel.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);

        showInitialFragment(new IntroFragment());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCode.REQUEST_DISCOVERABLE) {
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User cancelled the request");
            } else {
                Log.d(TAG, "Everything is ok, we can start");

                showNextFragment(new QrCodeFragment());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
        }
        return true;
    }

}