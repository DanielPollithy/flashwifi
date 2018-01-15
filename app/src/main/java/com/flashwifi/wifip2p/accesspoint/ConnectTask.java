package com.flashwifi.wifip2p.accesspoint;

import android.content.Context;
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

    @Override
    protected String doInBackground(Context... params) {
        Context context = params[0];
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);

        Log.d(TAG, "doInBackground: GO to sleep");
        try {
            Thread.sleep(30 * 1000);
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
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();

                    connected = true;
                    break;
                }
            }
        }

        Log.d(TAG, "doInBackground: now wait");

        int number_minutes = 60;
        for (int i=0; i<number_minutes; i++) {
            try {
                Thread.sleep(1000*60);
            } catch (InterruptedException e) {
                break;
            }
        }

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