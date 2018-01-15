package com.flashwifi.wifip2p.accesspoint;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.content.Context.WIFI_SERVICE;

public class StopAccessPointTask extends AsyncTask<Context, Void, String> {

    private final static String TAG = "AccessPointTask";
    private  Context context;
    String ssid = "iota-wifi-121431";
    WifiManager wifi;
    WifiInfo w;

    @Override
    protected String doInBackground(Context... params) {
        context = params[0];
        wifi = (WifiManager) context.getSystemService(WIFI_SERVICE);

        w = wifi.getConnectionInfo();
        Log.d("dsd", w.toString());

        if (wifi.isWifiEnabled())
        {
            wifi.setWifiEnabled(false);
        }
        Log.d(TAG, "stopAP: THIS IS STOPPING THE AP");
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, false);
                } catch (Exception ex) {
                }
                break;
            }
        }

        if (!wifi.isWifiEnabled())
        {
            wifi.setWifiEnabled(true);
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