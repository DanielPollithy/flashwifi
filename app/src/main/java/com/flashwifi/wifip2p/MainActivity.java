package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

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

    boolean doubleBackToExitPressedOnce = false;

    BroadcastReceiver updateUIReceiver;

    private void subscribeToBroadcasts() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.start_roaming");

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    // hide progress bar
                    ProgressBar progressConnection = (ProgressBar) findViewById(R.id.progressConnection);
                    progressConnection.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                }
                if (intent.getAction().equals("com.flashwifi.wifip2p.start_roaming")) {
                    startRoamingView(intent.getStringExtra("peer_mac_address"),
                            intent.getStringExtra("ssid"),
                            intent.getStringExtra("key"));
                }
            }
        };
        registerReceiver(updateUIReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unbindWifiBroadcast();
        unsubscribeFromBroadcast();
    }

    private void unbindWifiBroadcast() {
        try {unregisterReceiver(updateUIReceiver);
            unbindService(mConnection);
        }
        catch(IllegalArgumentException e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initEverything();
    }

    private void unsubscribeFromBroadcast() {
        try {
            unregisterReceiver(updateUIReceiver);
            updateUIReceiver = null;
        } catch (Exception e){
        }
    }

    private void initUi() {
        final Switch switch_ = (Switch) findViewById(R.id.wifiSwitch);
        switch_.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mBound) {
                        switch_.setChecked(false);
                        Toast.makeText(getApplicationContext(), "Wifi Broadcast not bound", Toast.LENGTH_SHORT).show();
                    } else {
                        mService.enableService();
                    }
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

        // get the secrets from the login screen
        Intent intent = getIntent();
        password = intent.getStringExtra("password");
        seed = intent.getStringExtra("seed");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Intent intent2 = new Intent(this, WiFiDirectBroadcastService.class);
        bindService(intent2, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initEverything() {
        subscribeToBroadcasts();
        initUi();
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {unregisterReceiver(updateUIReceiver);
            unbindService(mConnection);
        }
        catch(IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
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
//        } else if (id == R.id.nav_itp) {
//
        } else if (id == R.id.nav_fund) {
            startFundWalletFragment();
        } else if (id == R.id.nav_withdraw) {
            startWithdrawWalletFragment();
//        } else if (id == R.id.nav_conditions) {

        } else if (id == R.id.nav_settings) {
            startSettingsFragment();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean settingsAreReady(){
        // check whether all necessary settings were set by the user
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String[] intVariables = new String[]{
                "edit_text_sell_min_minutes",
                "edit_text_sell_price",
                "edit_text_sell_max_minutes",
                "edit_text_buy_price",
                "edit_text_client_minutes"
        };
        String val;
        for (String variable: intVariables) {
            val = prefs.getString(variable, null);
            if (val == null) {
                Log.d("Variable not set: ", "settingsAreReady: " + variable);
                return false;
            }
        }
        return true;
    }

    public void startSearchFragment() {
        if (mBound && mService.isInRoleHotspot()) {
            Toast.makeText(this, "Can't start search because you are hotspot", Toast.LENGTH_SHORT).show();
        } else if (!settingsAreReady()) {
            Toast.makeText(this, "Navigate to settings and assign all variables", Toast.LENGTH_SHORT).show();
        } else {
            Fragment fragment = new SearchFragment();

            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }

    }

    private void startRoamingView(String macAddress, String ssid, String key){
        // disable WiFi P2P
        if (mBound) {
            mService.stopDiscovery(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("MainAct", "discovery stopped");
                }

                @Override
                public void onFailure(int i) {
                    Log.d("MainAct", "discovery could not be stopped");
                }
            });
        }

        Intent intent = new Intent(this, RoamingActivity.class);
        intent.putExtra("address", macAddress);
        intent.putExtra("key", key);
        intent.putExtra("ssid", ssid);
        startActivity(intent);
    }

    public void startHotspotFragment() {
        if (mBound && mService.isInRoleConsumer()) {
            Toast.makeText(this, "Can't start hotspot because you are searching", Toast.LENGTH_SHORT).show();
        } else if (!settingsAreReady()) {
            Toast.makeText(this, "Navigate to settings and assign all variables", Toast.LENGTH_SHORT).show();
        } else {
            Fragment fragment = new HotspotFragment();
            Bundle args = new Bundle();

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

    private void startSettingsFragment() {
        Fragment fragment = new SettingsFragment();
        fragment.setRetainInstance(true);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce=false;
                }
            }, 2000);
        }
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