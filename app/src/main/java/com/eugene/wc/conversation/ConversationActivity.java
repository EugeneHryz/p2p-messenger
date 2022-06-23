package com.eugene.wc.conversation;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;
import com.eugene.wc.conversation.ConversationViewModel.State;
import com.eugene.wc.conversation.data.ConversationAdapter;
import com.eugene.wc.conversation.data.ConversationItem;
import com.eugene.wc.fragment.AlertDialogFragment;
import com.eugene.wc.fragment.DialogResultListener;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.view.EmojiTextInputView;
import com.eugene.wc.view.MessengerRecyclerView;
import com.eugene.wc.view.TextSendController;

import java.util.List;

import javax.inject.Inject;

public class ConversationActivity extends BaseActivity implements TextSendController.SendListener,
        DialogResultListener {

    private static final String TAG = ConversationActivity.class.getName();
    public static final String CONTACT_ID_KEY = "contact_id_key";

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private ConversationViewModel viewModel;

    private TextSendController sendController;

    private ConversationAdapter adapter;
    private MessengerRecyclerView messagesList;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ConversationViewModel.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_activity);
        setupActionBar();

        EmojiTextInputView emojiTextInput = findViewById(R.id.emoji_text_input);
        AppCompatImageButton sendButton = findViewById(R.id.send_button);
        sendController = new TextSendController(sendButton, emojiTextInput, this);

        messagesList = findViewById(R.id.messages_list);
        setupMessagesList();

        viewModel.getState().observe(this, this::handleStateChange);
        viewModel.getIncomingMessage().observe(this, this::displayMessage);
        viewModel.getStoredMessages().observe(this, this::displayMessages);

        int contactId = getIntent().getIntExtra(CONTACT_ID_KEY, -1);
        if (contactId != -1) {
            viewModel.loadContact(new ContactId(contactId));
        }
    }

    @Override
    public void onSendMessage(String text) {
        viewModel.sendTextMessage(text).observe(this, this::displayMessage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogDismissed() {
        supportFinishAfterTransition();
    }

    private void displayMessage(ConversationItem msgItem) {
        adapter.addItem(msgItem);
        scrollToEnd();

        // FIXME: ?
        if (adapter.getItemCount() > 0) {
            messagesList.showItems();
        }
    }

    private void displayMessages(List<ConversationItem> msgItems) {
        adapter.replaceAll(msgItems);
        scrollToEnd();

        messagesList.showItems();
    }

    private void scrollToEnd() {
        int pos = adapter.getItemCount() - 1;
        messagesList.scrollToPosition(pos);
    }

    private void setupMessagesList() {
        messagesList.setNoItemsDescription(R.string.no_messages_description);

        messagesList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ConversationAdapter();
        messagesList.setAdapter(adapter);
    }

    private void handleStateChange(State state) {
        if (state == State.CONTACT_LOADED) {
            Contact contact = viewModel.getContact();

            setTitle(contact.getIdentity().getName());
            viewModel.loadInitialMessages();
        } else if (state == State.CONTACT_DISCONNECTED) {

            showContactDisconnectedDialog();
        }
    }

    private void showContactDisconnectedDialog() {
        DialogFragment dialogFragment = new AlertDialogFragment(this);
        Bundle args = new Bundle();
        args.putInt(AlertDialogFragment.DIALOG_MESSAGE_KEY, R.string.dialog_contact_disconnected_msg);
        args.putInt(AlertDialogFragment.DIALOG_TITLE_KEY, R.string.dialog_title_notice);
        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);

        dialogFragment.show(getSupportFragmentManager(), null);
    }

    private void setupActionBar() {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }
}
