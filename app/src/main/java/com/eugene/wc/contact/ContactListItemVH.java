package com.eugene.wc.contact;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ContactListItemVH extends RecyclerView.ViewHolder {

    private final View contactItem;

    public ContactListItemVH(@NonNull View itemView) {
        super(itemView);
        contactItem = itemView;
    }

    public View getView() {
        return contactItem;
    }
}
