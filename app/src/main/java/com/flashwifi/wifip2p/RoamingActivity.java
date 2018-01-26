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
import java.util.Set;

import com.flashwifi.wifip2p.billing.Accountant;
import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;
import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService.RoamingState;
import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;

import org.w3c.dom.Text;


public class RoamingActivity extends AppCompatActivity {
    private static final String TAG = "RoamingActivity";

    String name;
    String address;

    WiFiDirectBroadcastService mService = null;
    boolean mBound = false;

    BroadcastReceiver updateUIReceiver;


    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");
        filter.addAction("com.flashwifi.wifip2p.update_roaming");

        if (updateUIReceiver == null) {
            updateUIReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateUi(intent);
                }
            };
            registerReceiver(updateUIReceiver, filter);
        }

        // Bind to LocalService
        if (mService == null) {
            Intent intent = new Intent(this, WiFiDirectBroadcastService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void restoreUIFromService() {
        // ACCESS POINT
        if (mService.getRoamingStates().contains(RoamingState.ACCESS_POINT)) {
            CheckBox apConnected = (CheckBox)findViewById(R.id.accessPointActive);
            apConnected.setChecked(true);
        }
        // FLASH CHANNEL
        if (mService.getRoamingStates().contains(RoamingState.FLASH_CHANNEL)) {
            CheckBox flashEstablished = (CheckBox)findViewById(R.id.flashEstablished);
            flashEstablished.setChecked(true);
        }
        // CHANNEL FUNDED
        if (mService.getRoamingStates().contains(RoamingState.CHANNEL_FUNDED)) {
            CheckBox channelFunded = (CheckBox)findViewById(R.id.channelFunded);
            channelFunded.setChecked(true);
        }

        // STOP BUTTON
        Button stopButton = (Button) findViewById(R.id.stopRoamingButton);
        ProgressBar stopProgressBar = (ProgressBar) findViewById(R.id.stopProgressBar);
        if (!mService.getRoamingStates().contains(RoamingState.END_REQUESTED)) {
            stopButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    endRoaming();
                }
            });
        } else {
            stopProgressBar.setVisibility(View.VISIBLE);
        }

        // EXIT STATE
        TextView stopText = (TextView) findViewById(R.id.stopText);
        if (mService.getRoamingStates().contains(RoamingState.EXIT)) {
            stopProgressBar.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            stopText.setVisibility(View.VISIBLE);
        }

        if (mService.isInRoleHotspot() && !mService.getRoamingStates().contains(RoamingState.CHANNEL_ATTACHED) && mService.getRoamingStates().contains(RoamingState.EXIT)) {
            CardView cardView = (CardView) findViewById(R.id.card_view_tangle_attachment);
            cardView.setVisibility(View.VISIBLE);
        }

        updateBillingCard();
        updateNotification();

    }

    private void updateNotification() {
        // ToDo: what shall happen?
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
                    //startBillingProtocol(1000);
                } else if (message.equals("AP FAILED")) {
                    apConnected.setChecked(false);
                    Toast.makeText(getApplicationContext(), "Connect to AP failed", Toast.LENGTH_LONG).show();
                    exitRoaming();
                } else if (message.equals("AP CREATION FAILED")) {
                    apConnected.setChecked(false);
                    Toast.makeText(getApplicationContext(), "Create AP failed", Toast.LENGTH_LONG).show();
                    exitRoaming();
                } else if (message.equals("AP STOPPED")) {
                    apConnected.setChecked(false);
                } else if (message.equals("Channel established")) {
                    flashEstablished.setChecked(true);
                } else if (message.equals("Start Channel funding")) {
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

        int minutes = Accountant.getInstance().getTotalDurance() / 60;
        int minutes_max = Accountant.getInstance().getBookedMinutes();
        int bytes_max = Accountant.getInstance().getBookedBytes();
        int bytes_used = Accountant.getInstance().getTotalBytes();
        int iotas_transferred = Accountant.getInstance().getTotalIotaPrice();
        int iotas_max = Accountant.getInstance().getTotalIotaDeposit();

        // only show the card if one bill came
        if (minutes == 0) {
            return;
        }

        CardView summaryView = (CardView) findViewById(R.id.card_view_overview);
        if (summaryView.getVisibility() != View.VISIBLE) {
            summaryView.setVisibility(View.VISIBLE);
        }

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

    @Override
    protected void onStop() {
        super.onStop();
        if (updateUIReceiver != null) {
            unregisterReceiver(updateUIReceiver);
            updateUIReceiver = null;
        }
        if (mService != null) {
            unbindService(mConnection);
            mService = null;
            mBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roaming);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        address = intent.getStringExtra("address");
        Log.d(TAG, "onCreate: Roaming activity got peer address: " + address);
    }



    private void initUIWithService() {

        TextView info_text = (TextView) findViewById(R.id.info_text);
        if (mService.isInRoleHotspot()) {
            info_text.setText(R.string.roaming_title);
        } else {
            info_text.setText(R.string.roaming_title_client);
        }

        restoreUIFromService();


    }

    private void endRoaming() {
        if (!mService.getRoamingStates().contains(RoamingState.END_REQUESTED)) {
            mService.addRoamingState(RoamingState.END_REQUESTED);

            // the next bill will send the close request
            Accountant.getInstance().setCloseAfterwards(true);

            // meanwhile show a loading icon
            ProgressBar stopProgressBar = (ProgressBar) findViewById(R.id.stopProgressBar);
            stopProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void exitRoaming() {

        // hide the spinner and the stop button
        ProgressBar stopProgressBar = (ProgressBar) findViewById(R.id.stopProgressBar);
        stopProgressBar.setVisibility(View.GONE);
        Button stopButton = (Button) findViewById(R.id.stopRoamingButton);
        stopButton.setVisibility(View.GONE);

        TextView stopText = (TextView) findViewById(R.id.stopText);
        stopText.setVisibility(View.VISIBLE);

        stopRoamingBroadcast();

        Toast.makeText(getApplicationContext(), "Press BACK now", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (mBound) {
            if (mService.isRoaming()) {
                Toast.makeText(getApplicationContext(), "stop roaming before leaving", Toast.LENGTH_LONG).show();
            } else {
                mService.setRoaming(false);
                mService.changeApplicationState(WiFiDirectBroadcastService.State.READY);
                super.onBackPressed();
            }
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

            initUIWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
