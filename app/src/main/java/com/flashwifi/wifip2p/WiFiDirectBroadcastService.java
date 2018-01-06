package com.flashwifi.wifip2p;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

public class WiFiDirectBroadcastService extends Service {
    public final static String TAG = "WiFiService";

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

    // content attributes
    ArrayList<String> arrayList = new ArrayList<>();
    WifiP2pInfo p2p_info;
    NetworkInfo network_info;
    WifiP2pGroup p2p_group;
    ArrayList<String> receivedMessages = new ArrayList<>();

    // socket stuff
    NegotiationServerTask negotiationServerTask;
    boolean serverRuns;

    private String currentDeviceConnected = null;

    // discovery mode
    private boolean discoveryModeActive = false;

    public void startNegotiationServer(boolean isClient) {
        Log.d("", "startSocketServer: ");
        //negotiationServerTask = new NegotiationServerTask();
        //negotiationServerTask.execute();
        String isClientString = (isClient) ? "True" : "False";
        new NegotiationServerTask().execute(isClientString);
    }

    public void startNegotiationClient(InetAddress address, boolean isClient) {
        Log.d("", "startSocketClient: ");
        String isClientString = (isClient) ? "True" : "False";
        String ipaddr = address.getHostAddress();
        new NegotiationClientTask().execute(isClientString, ipaddr);
    }

    public void sendMessageToSocketServer(InetAddress address, String message) {
        new ClientTask().execute(address.getHostAddress(), message);
    }

    public WifiP2pInfo getP2p_info() {
        return p2p_info;
    }

    public NetworkInfo getNetwork_info() {
        return network_info;
    }

    public WifiP2pGroup getP2p_group() {
        return p2p_group;
    }

    private void setupService() {
        if (!setup) {
            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);
            mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            registerReceiver(mReceiver, mIntentFilter);

            serverRuns = false;


            setup = true;
        }
    }

    public void startDiscoveryMode(WifiP2pManager.ActionListener action_listener) {
        mManager.discoverPeers(mChannel, action_listener);
        discoveryModeActive = true;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        WiFiDirectBroadcastService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WiFiDirectBroadcastService.this;
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
        setupService();
    }

    public ArrayList<String> getReceivedMessages() {
        return receivedMessages;
    }

    private void addReceivedMessage(String message) {
        receivedMessages.add(message);
        sendUpdateUIBroadcastWithMessage(message);
    }

    public void getPeerList(WifiP2pManager.ActionListener action_listener) {
        mManager.discoverPeers(mChannel, action_listener);
    }

    public ArrayList<String> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<String> arrayList) {
        this.arrayList = arrayList;
        sendUpdateUIBroadcast();
    }

    public void setNewIncomingConnection(WifiP2pInfo wifiP2pInfo){
        // This method is called when a new device connected to this one
        this.p2p_info = wifiP2pInfo;
        if (p2p_info.groupFormed) {
            if (currentDeviceConnected == null || !currentDeviceConnected.equals(p2p_info.groupOwnerAddress.getHostAddress())) {
                currentDeviceConnected = p2p_info.groupOwnerAddress.getHostAddress();
                sendUpdateUIBroadcastNewConnection();
            }
        }
    }

    public void setConnectionStateChanged(WifiP2pInfo p2p_info, NetworkInfo network_info, WifiP2pGroup p2p_group) {
        this.p2p_info = p2p_info;
        this.network_info = network_info;
        this.p2p_group = p2p_group;
    }

    private void sendUpdateUIBroadcast(){
        Intent local = new Intent();
        local.setAction("jenny.daniel.wifip2p.update_ui");
        this.sendBroadcast(local);
    }

    private void sendUpdateUIBroadcastNewConnection(){
        Log.d(TAG, "sendUpdateUIBroadcastNewConnection: SEND THEEM SHIIIIIIIT");
        Intent local = new Intent();
        local.setAction("jenny.daniel.wifip2p.update_ui");
        local.putExtra("what", "connectivity_changed");
        this.sendBroadcast(local);
    }

    private void sendUpdateUIBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("jenny.daniel.wifip2p.update_ui");
        this.sendBroadcast(local);
    }

    public void connect(String address, WifiP2pManager.ActionListener actionListener) {
        WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        // groupOwnerIntent determines how much you want to become the group onwer
        // 0 means little and 15 means a lot
        // https://stackoverflow.com/questions/18703881/how-to-make-a-specific-group-owner-in-wifi-direct-android
        config.groupOwnerIntent = 0;
        mManager.connect(mChannel, config, actionListener);
    }
}