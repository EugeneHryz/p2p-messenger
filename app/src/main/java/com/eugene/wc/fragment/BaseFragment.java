package com.eugene.wc.fragment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;

public abstract class BaseFragment extends Fragment {

    // interface for interacting with hosting activity??

    protected abstract void injectFragment(ActivityComponent activityComponent);

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        ActivityComponent activityComponent = ((BaseActivity) context).getActivityComponent();
        injectFragment(activityComponent);
    }

    public abstract String getUniqueTag();
}
