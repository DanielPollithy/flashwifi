package com.flashwifi.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.flashwifi.wifip2p.datastore.PeerInformation;
import com.flashwifi.wifip2p.datastore.PeerStore;

import java.util.ArrayList;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public final static String TAG = "Brudiiii";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private WiFiDirectBroadcastService service;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WiFiDirectBroadcastService service) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        WifiP2pManager.PeerListListener myPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                ArrayList<String> arrayList = new ArrayList<>();
                PeerInformation peer;

                // let the PeerInformations age
                PeerStore.getInstance().makeNewGeneration();

                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                    arrayList.add(device.deviceName + " : " + device.deviceAddress);

                    // create a PeerInformation from the WifiP2pDevice
                    peer = new PeerInformation();
                    peer.setWifiP2pDevice(device);
                    // update the PeerStore
                    PeerStore.getInstance().updateOrCreate(peer);

                }

                // change the state of the service
                service.setArrayList(arrayList);
            }
        };

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: WIFI_P2P_STATE_CHANGED_ACTION");
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d(TAG, "Wifi P2P is enabled");

            } else {
                // Wi-Fi P2P is not enabled
                Log.d(TAG, "Wi-Fi P2P is not enabled");
            }
            // Check to see if Wi-Fi is enabled and notify appropriate activity
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: WIFI_P2P_PEERS_CHANGED_ACTION");
            // Call WifiP2pManager.requestPeers() to get a list of current peers

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, myPeerListListener);
            }


        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION");
            // Respond to new connection or disconnections
            WifiP2pInfo p2p_info = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            NetworkInfo network_info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pGroup p2p_group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            Log.d(TAG, "p2p_info: " + p2p_info);
            Log.d(TAG, "network_info: " + network_info);
            Log.d(TAG, "p2p_group: " + p2p_group);

            Log.d(TAG, "onReceive: " + network_info.getState().toString());
            service.setConnectionStateChanged(p2p_info, network_info, p2p_group);

            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                    service.setNewIncomingConnection(wifiP2pInfo);
                }
            });

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            // Respond to this device's wifi state changing
        }
    }


}