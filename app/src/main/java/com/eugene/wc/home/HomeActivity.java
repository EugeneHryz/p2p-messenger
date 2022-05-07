package com.eugene.wc.home;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.R;
import com.eugene.wc.activity.ActivityComponent;
import com.eugene.wc.activity.BaseActivity;

import javax.inject.Inject;

public class HomeActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Override
    protected void injectActivity(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = setupToolbar();

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer_desc,
                R.string.close_drawer_desc) {
        };
        drawerLayout.addDrawerListener(drawerToggle);

        drawerToggle.syncState();
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
}