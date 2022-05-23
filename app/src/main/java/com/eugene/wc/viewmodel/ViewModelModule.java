package com.eugene.wc.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.contact.ContactListViewModel;
import com.eugene.wc.contact.add.AddContactViewModel;
import com.eugene.wc.home.HomeViewModel;
import com.eugene.wc.login.StartupViewModel;
import com.eugene.wc.signup.SignUpActivity;
import com.eugene.wc.signup.SignUpViewModel;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

    // The main idea is to add all ViewModels to the graph, so their dependencies can be satisfied.
    // And then bind all these ViewModels into a map, where Class<?> is a key and actual ViewModel
    // implementation is a value.

    @Binds
    @IntoMap
    @ClassKey(StartupViewModel.class)
    public abstract ViewModel bindStartupViewModel(StartupViewModel viewModel);

    @Binds
    @IntoMap
    @ClassKey(SignUpViewModel.class)
    public abstract ViewModel bindSignUpViewModel(SignUpViewModel viewModel);

    @Binds
    @IntoMap
    @ClassKey(HomeViewModel.class)
    public abstract ViewModel bindHomeViewModel(HomeViewModel viewModel);

    @Binds
    @IntoMap
    @Singleton
    @ClassKey(AddContactViewModel.class)
    public abstract ViewModel bindAddContactViewModel(AddContactViewModel viewModel);

    @Binds
    @IntoMap
    @ClassKey(ContactListViewModel.class)
    public abstract ViewModel bindContactListViewModel(ContactListViewModel viewModel);

    @Binds
    public abstract ViewModelProvider.Factory bindViewModelProvider(ViewModelFactory factory);
}
