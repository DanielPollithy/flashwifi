package com.flashwifi.wifip2p;

import android.annotation.SuppressLint;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.flashwifi.wifip2p.billing.Accountant;
import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;
import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;

import org.w3c.dom.Text;


public class RoamingActivity extends AppCompatActivity {
    private static final String TAG = "RoamingActivity";
    ArrayList<String> arrayList;
    ArrayAdapter<String> listAdapter;
    ListView listView;

    String name;
    String address;
    String ssid;
    String key;

    WiFiDirectBroadcastService mService;
    //AccessPointService apService;
    boolean mBound = false;
    InetAddress groupOwnerAddress;

    BroadcastReceiver updateUIReceiver;
    private boolean hotspot_running = false;

    private Button stopButton;
    private boolean endRoamingFlag = false;
    private boolean initiatedEnd;

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
                CheckBox channelFunded = (CheckBox)findViewById(R.id.channelFunded);
                if (message.equals("AP SUCCESS")) {
                    apConnected.setChecked(true);
                    mService.resetBillingState();
                    // when the AP is setup we can start the server
                    startBillingProtocol();
                } else if (message.equals("AP FAILED")) {
                    apConnected.setChecked(false);
                    Toast.makeText(getApplicationContext(), "Could not create Access point", Toast.LENGTH_LONG).show();
                    exitRoaming();
                } else if (message.equals("AP STOPPED")) {
                    apConnected.setChecked(false);
                } else if (message.equals("Channel established")) {
                    flashEstablished.setChecked(true);
                } else if (message.equals("Start Channel funding")) {
                    // start the task
                    mService.fundChannel(address);
                } else if (message.equals("Channel funded")) {
                    channelFunded.setChecked(true);
                } else if (message.equals("Billing")) {
                    updateBillingCard();
                } else if (message.equals("Channel closed")) {
                    exitRoaming();
                    if (mService.isInRoleHotspot()) {
                        showRetransferCard();
                    }
                } else if (message.equals("Socket exception")) {
                    //Toast.makeText(getApplicationContext(), "Socket exception", Toast.LENGTH_LONG).show();
                    //exitRoaming();
                } else if (message.equals("Exit")) {
                    Toast.makeText(getApplicationContext(), "Can't connect", Toast.LENGTH_LONG).show();
                    exitRoaming();
                }
                // ToDo: add a critical error that uses exitRoaming()
            }

        }

        //TextView connection_status = (TextView) findViewById(R.id.connection_status);
        //final View activity_view = findViewById(R.id.chatView);

        //connection_status.setText(network_info.toString());

    }

    private void stopRoamingBroadcast() {
        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.stop_roaming");
        this.sendBroadcast(local);
    }

    private void showRetransferCard() {
        CardView cardView = (CardView) findViewById(R.id.card_view_tangle_attachment);
        cardView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("DefaultLocale")
    private void updateBillingCard() {
        CardView summaryView = (CardView) findViewById(R.id.card_view_overview);
        if (summaryView.getVisibility() != View.VISIBLE) {
            summaryView.setVisibility(View.VISIBLE);
        }
        int minutes = Accountant.getInstance().getTotalDurance() / 60;
        int minutes_max = Accountant.getInstance().getBookedMinutes();
        int bytes_max = Accountant.getInstance().getBookedBytes();
        int bytes_used = Accountant.getInstance().getTotalBytes();
        int iotas_transferred = Accountant.getInstance().getTotalIotaPrice();
        int iotas_max = Accountant.getInstance().getTotalIotaDeposit();

        TextView summaryMinutes = (TextView) findViewById(R.id.summaryMinutes);
        summaryMinutes.setText(String.format("%d/%d minutes active", minutes, minutes_max));

        TextView summaryMegabytes = (TextView) findViewById(R.id.summaryMegabytes);
        summaryMegabytes.setText(String.format("%d/%d Megabytes roamed", bytes_used, bytes_max));

        TextView summaryIota = (TextView) findViewById(R.id.summaryIota);
        summaryIota.setText(String.format("%d/%d Iota transferred", iotas_transferred, iotas_max));

        ProgressBar progressMinutes = (ProgressBar) findViewById(R.id.progressbarDurance);
        progressMinutes.setMax(minutes_max);
        progressMinutes.setProgress(minutes);

        ProgressBar progressMegabytes = (ProgressBar) findViewById(R.id.progressbarMegabytes);
        progressMegabytes.setMax(bytes_max);
        progressMegabytes.setProgress(bytes_used);

        ProgressBar progressIota = (ProgressBar) findViewById(R.id.progressbarIota);
        progressIota.setProgress(iotas_transferred);
        progressIota.setMax(iotas_max);


    }

    private void startBillingProtocol() {
        // setup the flash channel etc...
        if (mService.isInRoleHotspot()) {
            mService.startBillingServer(address);
        } else {
            mService.startBillingClient(address);
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

        Accountant.getInstance().reset();

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        address = intent.getStringExtra("address");
        NegotiationFinalization negFin = PeerStore.getInstance().getLatestFinalization(address);
        ssid = negFin.getHotspotName();
        key = negFin.getHotspotPassword();

        initUI();

    }

    private void cancelNotification() {

    }


    private void initUI() {

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
                mService.startAP(ssid, key);
            } else {
                mService.connect2AP(ssid, key);
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

        updateBillingCard();

        // ToDo: update notification
        //showNotification();
    }

    private void endRoaming() {
        if (!initiatedEnd) {
            initiatedEnd = true;
            Accountant.getInstance().setCloseAfterwards(true);
            // the next bill will send the close request
            // meanwhile show a loading icon
            ProgressBar stopProgressBar = (ProgressBar) findViewById(R.id.stopProgressBar);
            stopProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void exitRoaming() {
        Accountant.getInstance().setCloseAfterwards(true);
        endRoamingFlag = true;
        cancelNotification();
        if (mService.isInRoleHotspot()){
            mService.stopAP();
        } else {
            mService.disconnectAP();
        }
        mService.setRoaming(false);
        mService.resetBillingState();
        mService.setInRoleConsumer(false);
        mService.setInRoleHotspot(false);

        PeerStore.getInstance().clear();

        // hide the spinner and the stop button
        ProgressBar stopProgressBar = (ProgressBar) findViewById(R.id.stopProgressBar);
        stopProgressBar.setVisibility(View.GONE);
        Button stopButton = (Button) findViewById(R.id.stopRoamingButton);
        stopButton.setVisibility(View.GONE);

        TextView stopText = (TextView) findViewById(R.id.stopText);
        stopText.setVisibility(View.VISIBLE);

        stopRoamingBroadcast();

        Toast.makeText(getApplicationContext(), "Press BACK now", Toast.LENGTH_LONG).show();
        initiatedEnd = false;
        //finish();
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
