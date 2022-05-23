package com.eugene.wc.contact;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eugene.wc.R;

import java.util.ArrayList;
import java.util.List;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListItemVH> {

    private List<ContactItem> items = new ArrayList<>();

    @NonNull
    @Override
    public ContactListItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_list_item, parent, false);

        return new ContactListItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactListItemVH holder, int position) {
        View view = holder.getView();
        TextView name = view.findViewById(R.id.contact_name);

        if (position < items.size()) {
            ContactItem item = items.get(position);

            name.setText(item.getName());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<ContactItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }
}
