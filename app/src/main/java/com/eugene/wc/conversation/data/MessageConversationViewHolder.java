package com.eugene.wc.conversation.data;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.eugene.wc.R;
import com.vanniktech.emoji.EmojiTextView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

public class MessageConversationViewHolder extends ConversationViewHolder {

    private final TextView timeView;
    private final EmojiTextView textView;

    public MessageConversationViewHolder(@NonNull View itemView) {
        super(itemView);

        timeView = itemView.findViewById(R.id.time);
        textView = itemView.findViewById(R.id.text);
    }

    @Override
    public void bind(ConversationItem item) {
        if (!(item instanceof MessageConversationItem)) {
            throw new IllegalArgumentException("Must pass MessageConversationItem as a parameter");
        }
        MessageConversationItem messageItem = (MessageConversationItem) item;

        LocalDateTime truncatedTime = messageItem.getTime().truncatedTo(ChronoUnit.MINUTES);
        String formattedTime = truncatedTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));

        timeView.setText(formattedTime);
        textView.setText(messageItem.getMessageText());
    }
}
