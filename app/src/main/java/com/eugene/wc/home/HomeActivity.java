package com.eugene.wc.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;
import com.eugene.wc.contact.ContactListFragment;
import com.eugene.wc.protocol.api.plugin.BluetoothConstants;
import com.eugene.wc.protocol.api.plugin.LanTcpConstants;
import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.TransportId;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HomeActivity extends BaseActivity {

    private static final String TAG = HomeActivity.class.getName();

    private HomeViewModel viewModel;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private GridView transportsView;
    private BaseAdapter transportsAdapter;

    private List<Transport> transports;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        drawerLayout = findViewById(R.id.drawer_layout);
        transportsView = findViewById(R.id.transports_grid);
        Toolbar toolbar = setupToolbar();

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer_desc,
                R.string.close_drawer_desc) {
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        initializeTransports();
        transportsView.setAdapter(transportsAdapter);

        showInitialFragment(new ContactListFragment());
    }

    private Toolbar setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        return toolbar;
    }

    private void initializeTransports() {
        transports = new ArrayList<>();
        transports.add(createTransport(BluetoothConstants.ID));
        transports.add(createTransport(LanTcpConstants.ID));

        transportsAdapter = new TransportsAdapter();

        viewModel.getBluetoothState().observe(this, state -> {
            Log.d(TAG, "bluetooth state changed in activity");
            handleTransportStateChange(BluetoothConstants.ID, state);
        });
        viewModel.getLanState().observe(this, state -> {
            Log.d(TAG, "lan state changed in activity");
            handleTransportStateChange(LanTcpConstants.ID, state);
        });
    }

    private void handleTransportStateChange(TransportId id, Plugin.State newState) {
        Transport transport = null;
        for (Transport t : transports) {
            if (t.getId().equals(id)) {
                transport = t;
            }
        }
        if (transport != null) {
            transport.setIconColorId(getIconColor(newState));
            transportsAdapter.notifyDataSetChanged();
        }
    }

    private Transport createTransport(TransportId id) {
        Transport transport = null;
        if (id.equals(BluetoothConstants.ID)) {
            int iconId = R.drawable.ic_bluetooth;
            int iconColor = R.color.wc_gray;
            String name = getString(R.string.bluetooth_transport);

            transport = new Transport(id, iconId, iconColor, name);
        } else if (id.equals(LanTcpConstants.ID)) {
            int iconId = R.drawable.ic_wifi;
            int iconColor = R.color.wc_gray;
            String name = getString(R.string.wifi_transport);

            transport = new Transport(id, iconId, iconColor, name);
        }
        return transport;
    }

    private int getIconColor(Plugin.State state) {
        int colorId = R.color.wc_gray;
        if (state == Plugin.State.ENABLING) {
            colorId = R.color.wc_orange;
        } else if (state == Plugin.State.ACTIVE) {
            colorId = R.color.secondary_dark_color;
        }
        return colorId;
    }

    private class TransportsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return transports.size();
        }

        @Override
        public Transport getItem(int position) {
            return position < transports.size() ? transports.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.transport_list_item,
                        parent, false);
            } else {
                view = convertView;
            }

            ImageView icon = view.findViewById(R.id.transport_icon);
            TextView name = view.findViewById(R.id.transport_name);

            Transport transport = transports.get(position);
            icon.setImageResource(transport.iconId);
            icon.setColorFilter(ContextCompat.getColor(
                    HomeActivity.this, transport.iconColorId));
            name.setText(transport.name);

            return view;
        }
    }

    private static class Transport {

        private TransportId id;
        private int iconId;
        private int iconColorId;
        private String name;

        public Transport(TransportId id, int iconId, int iconColor, String name) {
            this.id = id;
            this.iconId = iconId;
            this.iconColorId = iconColor;
            this.name = name;
        }

        public TransportId getId() {
            return id;
        }

        public void setId(TransportId id) {
            this.id = id;
        }

        public int getIconId() {
            return iconId;
        }

        public void setIconId(int iconId) {
            this.iconId = iconId;
        }

        public int getIconColorId() {
            return iconColorId;
        }

        public void setIconColorId(int iconColorId) {
            this.iconColorId = iconColorId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}