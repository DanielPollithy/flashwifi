package com.flashwifi.wifip2p;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AccessPointTask extends AsyncTask<Context, Void, String> {

    private final static String TAG = "AccessPointTask";

    @Override
    protected String doInBackground(Context... params) {
        String ssid = "iota-wifi-121431";
        Context context = params[0];
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo w = wifi.getConnectionInfo();
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
                netConfig.SSID = "\""+ssid+"\"";
                netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                //netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                //netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                //netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                //netConfig.preSharedKey = password;
                //netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                //netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                //netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                //netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

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
                        System.out.println("SUCCESSdddd");
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
            //statusView.setText("Your phone's API does not contain setWifiApEnabled method to configure an access point");
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {}

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}
}