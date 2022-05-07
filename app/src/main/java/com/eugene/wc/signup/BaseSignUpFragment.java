package com.eugene.wc.signup;

import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.fragment.BaseFragment;

public abstract class BaseSignUpFragment extends BaseFragment {

    protected SignUpViewModel viewModel;

    @Override
    protected void injectFragment(ActivityComponent activityComponent) {
        activityComponent.inject(this);

        viewModel = new ViewModelProvider(requireActivity()).get(SignUpViewModel.class);
    }
}
