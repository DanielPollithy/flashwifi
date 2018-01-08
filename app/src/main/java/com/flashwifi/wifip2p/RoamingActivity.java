package com.flashwifi.wifip2p;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;

import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;


public class RoamingActivity extends AppCompatActivity {

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

        //TextView connection_status = (TextView) findViewById(R.id.connection_status);
        //final View activity_view = findViewById(R.id.chatView);

        //connection_status.setText(network_info.toString());

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
        setContentView(R.layout.activity_roaming);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        address = intent.getStringExtra("address");

        initUI();
        showNotification();

    }


    private void showNotification() {
        // The id of the channel.
        String CHANNEL_ID = "com.flashwifi.wifip2p.roaming_1";

        // ToDo: make this work on high API version
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon_tethering_on)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, RoamingActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your app to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(RoamingActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(1, mBuilder.build());
    }


    private void initUI() {

        //final EditText input = (EditText) findViewById(R.id.chat_input);
        //Button button = (Button) findViewById(R.id.btn_send);
        /*button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                addMessageRight(name, input.getText().toString());
                // send the message to the peer
                //mService.sendMessageToSocketServer(groupOwnerAddress, input.getText().toString());
            }
        });*/

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                toggleHotspot();
            }
        });

        listView = (ListView) findViewById(R.id.peer_list);
        arrayList = new ArrayList<>();

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(listAdapter);*/
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
