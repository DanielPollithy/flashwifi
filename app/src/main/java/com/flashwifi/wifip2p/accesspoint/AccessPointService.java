package com.flashwifi.wifip2p.accesspoint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AccessPointService extends Service {
    public final static String TAG = "AccessPointService";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private boolean setup = false;

    // broadcast stuff
    BroadcastReceiver mReceiver;

    // socket stuff
    AccessPointTask apTask;
    boolean apRuns = false;

    public void startAP() {
        Log.d("xxxxxxxxxxxxxx", "start AP");
        if (!apRuns) {
            apRuns = true;
            apTask = new AccessPointTask();
            apTask.execute(this);
        } else {
            Log.d("", "startSocketServer: ALREADY RUNNING");
        }
    }

    public void stopAP() {
        Log.d("xxxxxxxxxxxxxx", "stop AP");
        if (apRuns) {
            apRuns = false;
            apTask.cancel(true);
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
    }

    private void sendUpdateUIBroadcast(){
        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.update_ui");
        this.sendBroadcast(local);
    }
}