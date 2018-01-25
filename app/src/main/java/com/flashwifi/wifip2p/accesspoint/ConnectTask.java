package com.flashwifi.wifip2p.accesspoint;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.flashwifi.wifip2p.protocol.NegotiationFinalization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;

public class ConnectTask extends AsyncTask<Object, Void, String> {

    private final static String TAG = "AccessPointTask";
    String ssid;
    String key;
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
    protected String doInBackground(Object... params) {
        context = (Context) params[0];
        ssid = (String) params[1];
        key = (String) params[2];
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

        int max_tries = 10;
        
        while (!connected && max_tries > 0) {
            max_tries--;
            Log.d(TAG, "doInBackground: try to find the network");
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            WifiInfo wifiInfo;
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    Log.d(TAG, "doInBackground: found it!!!");
                    wifiManager.disconnect();
                    wifiManager.disableNetwork(old_network_id);
                    wifiManager.reconnect();
                    boolean worked = wifiManager.enableNetwork(i.networkId, true);
                    if (worked) {
                        connected = true;
                        boolean connected_to_it = false;
                        String new_ssid;
                        Log.d(TAG, "New network added!");
                        int max_seconds = 10;
                        boolean wrong_network = false;
                        while (!connected_to_it && max_seconds > 0 && !wrong_network) {
                            Log.d(TAG, "try to connect to hotspot");
                            wifiInfo = wifiManager.getConnectionInfo();
                            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                                new_ssid = wifiInfo.getSSID();
                                if (new_ssid.contains(ssid)) {
                                    connected_to_it = true;
                                    Log.d(TAG, "doInBackground: WORKED enableNetwork");
                                    sendUpdateUIBroadcastWithMessage("AP SUCCESS");
                                } else {
                                    Log.d(TAG, "WRONG NETWORK");
                                    sendUpdateUIBroadcastWithMessage("AP FAILED");
                                    wrong_network = true;
                                }
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            max_seconds--;
                            if (max_seconds == 0) {
                                Log.d(TAG, "TRIED TOO LONG TO CONNECT TO HOTSPOT -> stop");
                                connected = false;
                                sendUpdateUIBroadcastWithMessage("AP FAILED");
                            }
                        }

                    } else {
                        Log.d(TAG, "doInBackground: Error connecting to the network");
                        Log.d(TAG, "doInBackground: Let's try it again");
                        //sendUpdateUIBroadcastWithMessage("AP FAILED");
                    }
                }
            }
            if (max_tries == 0) {
                Log.d(TAG, "doInBackground: Error connecting to the network");
                Log.d(TAG, "doInBackground: THIS WAS THE LAST TRY");
                sendUpdateUIBroadcastWithMessage("AP FAILED");
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