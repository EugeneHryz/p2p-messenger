package com.eugene.wc.activity;

import com.eugene.wc.ApplicationComponent;
import com.eugene.wc.contact.ContactListFragment;
import com.eugene.wc.home.HomeActivity;
import com.eugene.wc.login.EnterPasswordFragment;
import com.eugene.wc.login.StartupActivity;
import com.eugene.wc.signup.BaseSignUpFragment;
import com.eugene.wc.signup.EnterNicknameFragment;
import com.eugene.wc.signup.SetPasswordFragment;
import com.eugene.wc.signup.SignUpActivity;
import com.eugene.wc.splash.SplashScreenActivity;

import dagger.Component;

@ActivityScope
@Component(dependencies = { ApplicationComponent.class })
public interface ActivityComponent {

    void inject(SplashScreenActivity activity);

    void inject(StartupActivity activity);

    void inject(SignUpActivity activity);

    void inject(HomeActivity activity);


    void inject(BaseSignUpFragment fragment);

    void inject(EnterNicknameFragment fragment);

    void inject(SetPasswordFragment fragment);

    void inject(EnterPasswordFragment fragment);

    void inject(ContactListFragment fragment);
}
