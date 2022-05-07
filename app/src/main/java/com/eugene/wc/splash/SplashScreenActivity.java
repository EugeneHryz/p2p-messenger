package com.eugene.wc.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;
import com.eugene.wc.login.StartupActivity;
import com.eugene.wc.protocol.api.account.AccountManager;

import javax.inject.Inject;

public class SplashScreenActivity extends BaseActivity {

    @Inject
    AccountManager accountManager;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen_activity);
        getWindow().setExitTransition(new Fade());

        new Handler().postDelayed(() -> {

            startNextActivity(StartupActivity.class);
            supportFinishAfterTransition();
        }, 1000);
    }

    private void startNextActivity(Class<? extends Activity> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }
}