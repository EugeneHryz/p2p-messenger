package com.eugene.wc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.eugene.wc.R;

public class MessengerRecyclerView extends FrameLayout {

    // todo: add circular progress bar

    private RecyclerView recyclerView;
    private ImageView noItemsIcon;
    private TextView noItemsDescription;

    private RecyclerView.AdapterDataObserver noItemsObserver;

    public MessengerRecyclerView(@NonNull Context context) {
        super(context);
    }

    public MessengerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MessengerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initViews() {
        View rootView = LayoutInflater.from(getContext())
                .inflate(R.layout.messenger_list, this, true);

        recyclerView = rootView.findViewById(R.id.list);
        noItemsIcon = rootView.findViewById(R.id.no_items_icon);
        noItemsDescription = rootView.findViewById(R.id.no_items_description);

        noItemsDescription.setVisibility(INVISIBLE);
        noItemsIcon.setVisibility(INVISIBLE);

        noItemsObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                showItems();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                showItems();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                showItems();
            }
        };
    }

    public void setNoItemsIcon(@DrawableRes int resId) {
        if (recyclerView == null) {
            initViews();
        }
        noItemsIcon.setImageResource(resId);
    }

    public void setNoItemsDescription(@StringRes int resId) {
        if (recyclerView == null) {
            initViews();
        }
        noItemsDescription.setText(resId);
    }

    public void setLayoutManager(RecyclerView.LayoutManager lm) {
        if (recyclerView == null) {
            initViews();
        }
        recyclerView.setLayoutManager(lm);
    }

    public <T extends ViewHolder> void setAdapter(Adapter<T> adapter) {
        if (recyclerView == null) {
            initViews();
        }

        adapter.registerAdapterDataObserver(noItemsObserver);

        recyclerView.setAdapter(adapter);
    }

    public void showItems() {
        Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter != null) {
            int itemCount = adapter.getItemCount();
            if (itemCount == 0) {
                recyclerView.setVisibility(INVISIBLE);
                noItemsIcon.setVisibility(VISIBLE);
                noItemsDescription.setVisibility(VISIBLE);
            } else {
                recyclerView.setVisibility(VISIBLE);
                noItemsIcon.setVisibility(INVISIBLE);
                noItemsDescription.setVisibility(INVISIBLE);
            }
        }
    }

    public void scrollToPosition(int pos) {
        if (recyclerView == null) {
            initViews();
        }
        recyclerView.scrollToPosition(pos);
    }
}
