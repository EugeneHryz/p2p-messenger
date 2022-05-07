package com.eugene.wc.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Map;

import javax.inject.Inject;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Map<Class<?>, ViewModel> viewModels;

    @Inject
    public ViewModelFactory(Map<Class<?>, ViewModel> viewModels) {
        this.viewModels = viewModels;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        ViewModel viewModel = viewModels.get(modelClass);

        if (viewModel == null) {
            throw new IllegalArgumentException("Unable to find " + modelClass.getName());
        }
        return (T) viewModel;
    }
}
