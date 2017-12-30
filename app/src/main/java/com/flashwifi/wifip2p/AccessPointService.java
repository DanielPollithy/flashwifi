package com.flashwifi.wifip2p;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Random;

public class AccessPointService extends Service {
    public final static String TAG = "AccessPointService";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();

    private boolean setup = false;

    // broadcast stuff
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    // socket stuff
    AccessPointTask apTask;
    boolean apRuns;

    public void startAP() {
        Log.d("", "start AP");
        if (!apRuns) {
            apRuns = true;
            apTask.execute(this);
        } else {
            Log.d("", "startSocketServer: ALREADY RUNNING");
        }
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        AccessPointService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AccessPointService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        apTask = new AccessPointTask();
    }

    private void sendUpdateUIBroadcast(){
        Intent local = new Intent();
        local.setAction("jenny.daniel.wifip2p.update_ui");
        this.sendBroadcast(local);
    }
}