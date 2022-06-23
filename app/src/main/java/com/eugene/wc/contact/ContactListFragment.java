package com.eugene.wc.contact;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.RequestCode;
import com.eugene.wc.contact.add.AddContactActivity;
import com.eugene.wc.conversation.ConversationActivity;
import com.eugene.wc.fragment.AlertDialogFragment;
import com.eugene.wc.fragment.BaseFragment;
import com.eugene.wc.fragment.DialogResultListener;
import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.view.MessengerRecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import javax.inject.Inject;

public class ContactListFragment extends BaseFragment implements ContactListAdapter.Callback,
        DialogResultListener {

    private static final String TAG = ContactListFragment.class.getName();

    @Inject
    ConnectionRegistry connectionRegistry;
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
    public void onContactItemClicked(ContactId contactId) {
        if (connectionRegistry.isConnected(contactId)) {
            Intent intent = new Intent(requireActivity(), ConversationActivity.class);
            intent.putExtra(ConversationActivity.CONTACT_ID_KEY, contactId.getInt());
            startActivity(intent);
        } else {
            showAlertDialog();
        }
    }

    @Override
    public void onDialogDismissed() {
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }

    private void setupRecyclerView() {
        contactList.setNoItemsIcon(R.drawable.ic_contacts);
        contactList.setNoItemsDescription(R.string.no_contacts_description);

        contactList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactListAdapter(this);

        contactList.setAdapter(adapter);
    }

    private void showAlertDialog() {
        DialogFragment dialogFragment = new AlertDialogFragment(this);
        Bundle args = new Bundle();
        args.putInt(AlertDialogFragment.DIALOG_MESSAGE_KEY, R.string.dialog_contact_offline_msg);
        args.putInt(AlertDialogFragment.DIALOG_TITLE_KEY, R.string.dialog_title_notice);
        dialogFragment.setArguments(args);

        dialogFragment.show(getChildFragmentManager(), null);
    }

    private void updateContactItems(Predicate<ContactItem> shouldUpdate) {
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
