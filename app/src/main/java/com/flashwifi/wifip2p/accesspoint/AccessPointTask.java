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

public class AccessPointTask extends AsyncTask<Context, Void, String> {

    private final static String TAG = "AccessPointTask";
    private  Context context;
    String ssid = "Iotify";
    String key = "1234567890";
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
        Method[] wmMethods = wifi.getClass().getDeclaredMethods(); //Get all declared methods in WifiManager class
        boolean methodFound = false;

        for (Method method: wmMethods){
            if (method.getName().equals("setWifiApEnabled")){
                methodFound = true;
                WifiConfiguration netConfig = new WifiConfiguration();
                netConfig.SSID = String.format("%s", ssid);
                netConfig.preSharedKey = String.format("%s", key);
                netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                try {
                    boolean apstatus = (Boolean) method.invoke(wifi, netConfig,true);
                    //statusView.setText("Creating a Wi-Fi Network \""+netConfig.SSID+"\"");
                    for (Method isWifiApEnabledmethod: wmMethods)
                    {
                        if (isWifiApEnabledmethod.getName().equals("isWifiApEnabled")){
                            while (!(Boolean)isWifiApEnabledmethod.invoke(wifi)){
                            };
                            for (Method method1: wmMethods){
                                if(method1.getName().equals("getWifiApState")){
                                    int apstate;
                                    apstate = (Integer)method1.invoke(wifi);
                                    //                      netConfig = (WifiConfiguration)method1.invoke(wifi);
                                    //statusView.append("\nSSID:"+netConfig.SSID+"\nPassword:"+netConfig.preSharedKey+"\n");
                                }
                            }
                        }
                    }

                    if(apstatus)
                    {
                        System.out.println("SUCCESS");
                        //statusView.append("\nAccess Point Created!");
                        //finish();
                        //Intent searchSensorsIntent = new Intent(this,SearchSensors.class);
                        //startActivity(searchSensorsIntent);
                    }
                    else
                    {
                        System.out.println("FAILED");

                        //statusView.append("\nAccess Point Creation failed!");
                    }
                }
                catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!methodFound){
            // ToDo: implement this
            //statusView.setText("Your phone's API does not contain setWifiApEnabled method to configure an access point");
        }

        /*int number_minutes = 60;
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
        //stopAP();
    }

    private void stopAP() {
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
    }

    @Override
    protected void onPostExecute(String result) {

    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}
}