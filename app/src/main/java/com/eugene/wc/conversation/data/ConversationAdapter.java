package com.eugene.wc.conversation.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.eugene.wc.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ConversationAdapter extends MessengerAdapter<ConversationItem, ConversationViewHolder> {

    private static final String TAG = ConversationAdapter.class.getName();

    public ConversationAdapter() {
        super(ConversationItem.class);
    }

    @Override
    public int getItemViewType(int position) {
        ConversationItem item = items.get(position);
        return item.getLayoutRes();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);

        ConversationViewHolder viewHolder;
        if (viewType == R.layout.in_msg_list_item
                || viewType == R.layout.out_msg_list_item) {

            viewHolder = new MessageConversationViewHolder(view);
        } else {
            throw new IllegalArgumentException();
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationItem item = items.get(position);
        LocalDate date = item.getTime().toLocalDate();

        View view = holder.getView();
        TextView msgDate = view.findViewById(R.id.date);

        if (position == 0 || !items.get(position - 1).getTime().toLocalDate().equals(date)) {

            msgDate.setVisibility(View.VISIBLE);
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.US);
            msgDate.setText(date.format(formatter));
        } else {
            msgDate.setVisibility(View.GONE);
        }

        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int compare(ConversationItem t1, ConversationItem t2) {
        return t1.getTime().compareTo(t2.getTime());
    }

    @Override
    public boolean areContentsTheSame(ConversationItem t1, ConversationItem t2) {
        return t1.equals(t2);
    }

    @Override
    public boolean areItemsTheSame(ConversationItem item1, ConversationItem item2) {
        return item1.getMessageId().equals(item2.getMessageId());
    }
}
