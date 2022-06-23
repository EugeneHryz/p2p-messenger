package com.eugene.wc.conversation.data;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SortedList;

import java.util.Collection;

public abstract class MessengerAdapter<T, V extends ViewHolder> extends RecyclerView.Adapter<V> {

    protected final SortedList<T> items;

    public MessengerAdapter(Class<T> clazz) {

        items = new SortedList<T>(clazz, new SortedList.Callback<T>() {
            @Override
            public int compare(T o1, T o2) {
                return MessengerAdapter.this.compare(o1, o2);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(T oldItem, T newItem) {
                return MessengerAdapter.this.areContentsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areItemsTheSame(T item1, T item2) {
                return MessengerAdapter.this.areItemsTheSame(item1, item2);
            }

            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }
        }, 0);
    }

    public abstract int compare(T t1, T t2);

    public abstract boolean areContentsTheSame(T t1, T t2);

    public abstract boolean areItemsTheSame(T item1, T item2);

    public void replaceAll(Collection<T> newItems) {
        items.replaceAll(newItems);
    }

    public void addItem(T item) {
        items.add(item);
    }
}
