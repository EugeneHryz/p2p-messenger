package com.eugene.wc.conversation.data;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ConversationViewHolder extends RecyclerView.ViewHolder {

    protected final View view;

    public ConversationViewHolder(@NonNull View itemView) {
        super(itemView);
        view = itemView;
    }

    public abstract void bind(ConversationItem item);

    public View getView() {
        return view;
    }
}
