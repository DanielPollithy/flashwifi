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
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;

import com.flashwifi.wifip2p.billing.Accountant;
import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;


public class RoamingActivity extends AppCompatActivity {
    private static final String TAG = "RoamingActivity";
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

    private Button stopButton;
    private boolean endRoamingFlag = false;

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");
        filter.addAction("com.flashwifi.wifip2p.update_roaming");

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
        if (intent.getAction().equals("com.flashwifi.wifip2p.update_roaming")) {
            Log.d(TAG, "updateUi: Received change from AsyncTask");
            String message = intent.getStringExtra("message");
            if (message != null) {
                Log.d(TAG, "updateUi: message=" + message);
                CheckBox apConnected = (CheckBox)findViewById(R.id.accessPointActive);
                CheckBox flashEstablished = (CheckBox)findViewById(R.id.flashEstablished);
                if (message.equals("AP SUCCESS")) {
                    apConnected.setChecked(true);
                    // when the AP is setup we can start the server
                    startBillingProtocol();
                } else if (message.equals("AP FAILED")) {
                    apConnected.setChecked(false);
                    Toast.makeText(getApplicationContext(), "Could not create Access point", Toast.LENGTH_LONG).show();
                } else if (message.equals("AP STOPPED")) {
                    apConnected.setChecked(false);
                } else if (message.equals("Channel established")) {
                    flashEstablished.setChecked(true);
                } else if (message.equals("Billing")) {
                    updateBillingCard();
                }
            }

        }

        //TextView connection_status = (TextView) findViewById(R.id.connection_status);
        //final View activity_view = findViewById(R.id.chatView);

        //connection_status.setText(network_info.toString());

    }

    private void updateBillingCard() {
        CardView summaryView = (CardView) findViewById(R.id.card_view_overview);
        if (summaryView.getVisibility() != View.VISIBLE) {
            summaryView.setVisibility(View.VISIBLE);
        }
        String minutes = Integer.toString(Accountant.getInstance().getTotalDurance());
        String megabytes_max = Integer.toString(Accountant.getInstance().getBookedMegabytes());
        String megabytes_used = Integer.toString(Accountant.getInstance().getTotalMegabytes());
        String iotas_transferred = Integer.toString(Accountant.getInstance().getTotalIotaPrice());

        TextView summaryMinutes = (TextView) findViewById(R.id.summaryMinutes);
        summaryMinutes.setText(String.format("%s minutes active", minutes));

        TextView summaryMegabytes = (TextView) findViewById(R.id.summaryMegabytes);
        summaryMegabytes.setText(String.format("%s/%s Megabytes roamed", megabytes_used, megabytes_max));


        TextView summaryIota = (TextView) findViewById(R.id.summaryIota);
        summaryMegabytes.setText(String.format("%s Iota transferred", iotas_transferred));


    }

    private void startBillingProtocol() {
        // setup the flash channel etc...
        if (mService.isInRoleHotspot()) {
            mService.startBillingServer();
        } else {
            mService.startBillingClient();
        }
    }

    @Override
    protected void onStop() {
       /* if (!endRoamingFlag) {
            endRoaming();
        }*/
        super.onStop();
        unregisterReceiver(updateUIReceiver);
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

    }

    private void cancelNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(1);
    }


    private void showNotification() {
        // The id of the channel.
        String CHANNEL_ID = "com.flashwifi.wifip2p.roaming_1";

        String contentText = mService.isInRoleHotspot() ? "You are providing" : "You are consuming";

        // ToDo: make this work on high API version
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon_tethering_on)
                        .setContentTitle("IOTA HOTSPOT")
                        .setContentText(contentText);
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

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WiFiDirectBroadcastService.LocalBinder binder = (WiFiDirectBroadcastService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            // start hotspot
            if (mService.isInRoleHotspot()) {
                mService.startAP();
            } else {
                mService.connect2AP("Iotify", "1234567890");
            }
            initUIWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void initUIWithService() {

        TextView info_text = (TextView) findViewById(R.id.info_text);
        if (mService.isInRoleHotspot()) {
            info_text.setText(R.string.roaming_title);
        } else {
            info_text.setText(R.string.roaming_title_client);
        }

        stopButton = (Button) findViewById(R.id.stopRoamingButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                endRoaming();
            }
        });

        showNotification();
    }

    private void endRoaming() {
        endRoamingFlag = true;
        cancelNotification();
        if (mService.isInRoleHotspot()){
            mService.stopAP();
        } else {
            mService.disconnectAP();
        }
        mService.setRoaming(false);
        Toast.makeText(getApplicationContext(), "Press BACK now", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (mBound) {
            if (mService.isRoaming()) {
                Toast.makeText(getApplicationContext(), "stop roaming before leaving", Toast.LENGTH_LONG).show();
            } else {
                super.onBackPressed();
            }
        }
    }

}
