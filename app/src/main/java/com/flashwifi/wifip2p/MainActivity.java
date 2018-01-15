package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{

    private String password;
    private String seed;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private WiFiDirectBroadcastService mService;
    private boolean mBound;

    BroadcastReceiver updateUIReceiver;

    private void subscribeToBroadcasts() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.start_roaming");

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.flashwifi.wifip2p.start_roaming")) {
                    startRoamingView(intent.getStringExtra("peer_mac_address"));
                }
            }
        };
        registerReceiver(updateUIReceiver, filter);
    }

    private void initUi() {
        Switch switch_ = (Switch) findViewById(R.id.wifiSwitch);
        switch_.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mService.enableService();
                } else {
                    mService.disableService();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // get the secrets from the login screen
        Intent intent = getIntent();
        password = intent.getStringExtra("password");
        seed = intent.getStringExtra("seed");

        // Bind to Service
        Intent intent2 = new Intent(this, WiFiDirectBroadcastService.class);
        bindService(intent2, mConnection, Context.BIND_AUTO_CREATE);

        subscribeToBroadcasts();
        initUi();

    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(updateUIReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_search) {
            // Handle the camera action
            startSearchFragment();
        } else if (id == R.id.nav_start) {
            startHotspotFragment();
        } else if (id == R.id.nav_itp) {

        } else if (id == R.id.nav_fund) {
            startFundWalletFragment();
        } else if (id == R.id.nav_withdraw) {
            startWithdrawWalletFragment();
        } else if (id == R.id.nav_conditions) {

        } else if (id == R.id.nav_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startSearchFragment() {
        if (mBound && mService.isInRoleHotspot()) {
            Toast.makeText(this, "Can't start search because you are hotspot", Toast.LENGTH_SHORT).show();
        } else {
            Fragment fragment = new SearchFragment();

            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }

    }

    private void startRoamingView(String macAddress){
        Intent intent = new Intent(this, RoamingActivity.class);
        intent.putExtra("address", macAddress);
        startActivity(intent);
    }

    public void startHotspotFragment() {
        if (mBound && mService.isInRoleConsumer()) {
            Toast.makeText(this, "Can't start hotspot because you are searching", Toast.LENGTH_SHORT).show();
        } else {
            Fragment fragment = new HotspotFragment();
            Bundle args = new Bundle();
            //args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
            //fragment.setArguments(args);

            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
    }

    public void startFundWalletFragment() {
        Fragment fragment = new FundWalletFragment();

        Bundle bundle = new Bundle();
        bundle.putString("seed", seed);
        fragment.setArguments(bundle);
        fragment.setRetainInstance(true);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    private void startWithdrawWalletFragment() {
        Fragment fragment = new WithdrawWalletFragment();

        Bundle bundle = new Bundle();
        bundle.putString("seed", seed);
        fragment.setArguments(bundle);
        fragment.setRetainInstance(true);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WiFiDirectBroadcastService.LocalBinder binder = (WiFiDirectBroadcastService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}