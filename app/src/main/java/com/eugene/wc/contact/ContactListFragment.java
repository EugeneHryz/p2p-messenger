package com.eugene.wc.contact;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.RequestCode;
import com.eugene.wc.contact.add.AddContactActivity;
import com.eugene.wc.fragment.BaseFragment;
import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.view.MessengerRecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import javax.inject.Inject;

public class ContactListFragment extends BaseFragment {

    private static final String TAG = ContactListFragment.class.getName();

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private ContactListViewModel viewModel;

    private MessengerRecyclerView contactList;
    private ContactListAdapter adapter;

    @Override
    protected void injectFragment(ActivityComponent activityComponent) {
        activityComponent.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ContactListViewModel.class);

        viewModel.loadContacts();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list_fragment, container, false);

        FloatingActionButton fab = view.findViewById(R.id.add_contact_button);
        fab.setOnClickListener(v -> startAddContactActivityForResult());

        contactList = view.findViewById(R.id.contact_list);
        setupRecyclerView();

        viewModel.getContacts().observe(getViewLifecycleOwner(), contactItems ->
                adapter.setItems(contactItems));

        viewModel.getShouldUpdate().observe(getViewLifecycleOwner(), this::updateContactItems);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCode.ADD_NEW_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.loadContacts();
            } else {
                // todo show failed message
            }
        }
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }

    private void setupRecyclerView() {
        contactList.setNoItemsIcon(R.drawable.ic_contacts);
        contactList.setNoItemsDescription(R.string.no_contacts_description);

        contactList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactListAdapter();

        contactList.setAdapter(adapter);
    }

    private void updateContactItems(Predicate<ContactItem> shouldUpdate) {
        Log.d(TAG, "About to update contact status...");
        List<ContactItem> allItems = viewModel.getContacts().getValue();
        if (allItems != null) {
            for (ContactItem c : allItems) {
                if (shouldUpdate.test(c)) {
                    adapter.updateItem(c);
                }
            }
        }
    }

    private void startAddContactActivityForResult() {
        Intent intent = new Intent(requireContext(), AddContactActivity.class);
        startActivityForResult(intent, RequestCode.ADD_NEW_CONTACT);
    }
}
