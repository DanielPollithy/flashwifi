package com.flashwifi.wifip2p.accesspoint;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;

public class ConnectTask extends AsyncTask<Context, Void, String> {

    private final static String TAG = "AccessPointTask";
    String ssid = "Iotify";
    String key = "1234567890";
    WifiManager wifi;
    WifiInfo w;

    Context context;

    private void sendUpdateUIBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("com.flashwifi.wifip2p.update_roaming");
        context.sendBroadcast(local);
    }

    @Override
    protected String doInBackground(Context... params) {
        context = params[0];
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);

        WifiInfo info = wifiManager.getConnectionInfo(); //get WifiInfo
        int old_network_id = info.getNetworkId(); //get id of currently connected network

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);

        Log.d(TAG, "doInBackground: GO to sleep");
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "doInBackground: disconnect");
        int netId = wifiManager.addNetwork(wifiConfig);
        
        boolean connected = false;
        
        while (!connected) {
            Log.d(TAG, "doInBackground: try to find the network");
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    Log.d(TAG, "doInBackground: found it!!!");
                    wifiManager.disconnect();
                    wifiManager.disableNetwork(old_network_id);
                    boolean worked = wifiManager.enableNetwork(i.networkId, true);
                    if (worked) {
                        Log.d(TAG, "doInBackground: WORKED enableNetwork");
                        sendUpdateUIBroadcastWithMessage("AP SUCCESS");
                    } else {
                        Log.d(TAG, "doInBackground: Error connecting to the network");
                        sendUpdateUIBroadcastWithMessage("AP FAILED");
                    }
                    wifiManager.reconnect();

                    connected = true;
                    break;
                }
            }
        }

        /*Log.d(TAG, "doInBackground: now wait");

        int number_minutes = 60;
        for (int i=0; i<number_minutes; i++) {
            try {
                Thread.sleep(1000*60);
            } catch (InterruptedException e) {
                break;
            }
        }*/

        return null;
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(String result) {
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}
}