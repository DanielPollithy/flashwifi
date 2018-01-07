package com.flashwifi.wifip2p;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;

import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;


public class ChatActivity  extends AppCompatActivity {

    ArrayList<String> arrayList;
    ArrayAdapter<String> listAdapter;
    ListView listView;

    String name;
    String address;

    WiFiDirectBroadcastService mService;
    //AccessPointService apService;
    boolean mBound = false;
    InetAddress groupOwnerAddress;

    BroadcastReceiver updateUIReceiver;
    private boolean hotspot_running = false;

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi(intent);
            }
        };
        registerReceiver(updateUIReceiver, filter);

        // Bind to LocalService
        Intent intent = new Intent(this, WiFiDirectBroadcastService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Bind to
        //Intent intent2 = new Intent(this, AccessPointService.class);
        //bindService(intent2, apConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateUi(Intent intent) {
        NetworkInfo network_info = mService.getNetwork_info();
        WifiP2pInfo p2p_info = mService.getP2p_info();

        TextView connection_status = (TextView) findViewById(R.id.connection_status);
        final View activity_view = findViewById(R.id.chatView);

        connection_status.setText(network_info.toString());

    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        address = intent.getStringExtra("address");

        initUI();

    }



    private void connectToPeer(String address) {
        if (mBound) {
            mService.connect(address, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Connected to peer", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getApplicationContext(), "Error connecting to peer", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Service not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void initUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(name);
        setSupportActionBar(toolbar);

        final EditText input = (EditText) findViewById(R.id.chat_input);
        Button button = (Button) findViewById(R.id.btn_send);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                addMessageRight(name, input.getText().toString());
                // send the message to the peer
                //mService.sendMessageToSocketServer(groupOwnerAddress, input.getText().toString());
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                toggleHotspot();
            }
        });

        listView = (ListView) findViewById(R.id.peer_list);
        arrayList = new ArrayList<>();

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(listAdapter);
    }

    public void toggleHotspot() {
        if (hotspot_running) {
            stopHotspot();
        } else {
            startHotspot();
        }
    }

    private void startHotspot() {
        //apService.startAP();
        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setImageResource(R.drawable.icon_tethering_on);
        //hotspot_running = true;
    }

    private void stopHotspot() {
        //apService.stopAP();
        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setImageResource(R.drawable.icon_tethering_off);
        //hotspot_running = false;
    }

    public void addMessageLeft(String name, String text) {
        listAdapter.notifyDataSetInvalidated();
        listAdapter.add(name + ": " + text);
        listAdapter.notifyDataSetChanged();

        listView.setSelection(listAdapter.getCount() - 1);
    }

    public void addMessageRight(String name, String text) {
        listAdapter.notifyDataSetInvalidated();
        listAdapter.add(name + ": " + text);
        listAdapter.notifyDataSetChanged();
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
            // start connection
            // connectToPeer(address);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
