package com.flashwifi.wifip2p.broadcast;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.flashwifi.wifip2p.accesspoint.AccessPointTask;
import com.flashwifi.wifip2p.accesspoint.ConnectTask;
import com.flashwifi.wifip2p.accesspoint.StopAccessPointTask;
import com.flashwifi.wifip2p.billing.BillingClient;
import com.flashwifi.wifip2p.billing.BillingServer;
import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.negotiation.Negotiator;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WiFiDirectBroadcastService extends Service {
    public final static String TAG = "WiFiService";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private boolean setup = false;
    private boolean inRoleHotspot = false;
    private boolean inRoleConsumer = false;
    private boolean isRoaming = false;
    private boolean billingServerIsRunning = false;
    private boolean billingClientIsRunning = false;

    private boolean enabled = false;

    // broadcast stuff
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver = null;
    IntentFilter mIntentFilter;

    // content attributes
    ArrayList<String> arrayList = new ArrayList<>();
    WifiP2pInfo p2p_info;
    NetworkInfo network_info;
    WifiP2pGroup p2p_group;
    ArrayList<String> receivedMessages = new ArrayList<>();

    // async task store
    ArrayList<Thread> threads = new ArrayList<Thread>();


    private String currentDeviceConnected = null;

    // discovery mode
    private boolean discoveryModeActive = false;

    // HOTSPOT
    // socket stuff
    AccessPointTask apTask;
    boolean apRuns = false;
    ConnectTask connectTask;

    public void enableWiFi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
        }
    }

    public void disableWiFi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(false);
        }
    }

    public void startBillingServer(){
        if (!billingServerIsRunning) {
            billingServerIsRunning = true;

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: instantiate billing server");
                    // ToDo: remove magic numbers
                    BillingServer billingServer = new BillingServer(10, 20, 30, 10000, 1000, getApplicationContext());

                    try {
                        billingServer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // ToDo: handle billingServer EXIT CODES
                    // -> close the roaming etc.
                }
            };
            Log.d(TAG, "startBillingServer");
            Thread thread = new Thread(task);
            //asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            //AsyncTask.execute(thread);
            threads.add(thread);
            thread.start();
        }
    }


    public void startBillingClient() {
        if (!billingClientIsRunning) {
            billingClientIsRunning = true;

            Log.d(TAG, "startBillingClient");

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    // ToDo: remove magic numbers
                    BillingClient billingClient = new BillingClient(getApplicationContext());
                    // ToDo: get the AP gateway ip address
                    // https://stackoverflow.com/questions/9035784/how-to-know-ip-address-of-the-router-from-code-in-android
                    billingClient.start("192.168.43.1");
                    // ToDo: handle billingServer EXIT CODES
                    // -> close the roaming etc.
                }
            };
            Thread thread = new Thread(task);
            threads.add(thread);
            AsyncTask.execute(thread);
        }
    }

    public void connect2AP(String ssid, String key) {
        connectTask = new ConnectTask();
        Log.d(TAG, "connect2AP: CONNECT TO THE HOTSPOT");
        connectTask.execute(getApplicationContext(), ssid, key);
    }


    public void disconnectAP() {
        // ToDo: implement this?
    }

    public void startAP(String ssid, String key) {
        if (!apRuns) {
            Log.d(TAG, "start Access point");
            apRuns = true;
            apTask = new AccessPointTask();
            apTask.execute(getApplicationContext(), ssid, key);
        } else {
            Log.d(TAG, "Access point ALREADY RUNNING");
        }
    }

    public void stopAP() {
        Log.d(TAG, "stop AP");
        if (apRuns) {
            apRuns = false;
            //new StopAccessPointTask().execute(getApplicationContext());
            apTask.cancel(true);
        } else {
            Log.d("", "startSocketServer: ALREADY RUNNING");
        }
    }

    public String getWFDMacAddress(){
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ntwInterface : interfaces) {

                if (ntwInterface.getName().equalsIgnoreCase("p2p0")) {
                    byte[] byteMac = ntwInterface.getHardwareAddress();
                    if (byteMac==null){
                        return null;
                    }
                    StringBuilder strBuilder = new StringBuilder();
                    for (int i=0; i<byteMac.length; i++) {
                        strBuilder.append(String.format("%02X:", byteMac[i]));
                    }

                    if (strBuilder.length()>0){
                        strBuilder.deleteCharAt(strBuilder.length()-1);
                    }

                    return strBuilder.toString();
                }

            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    private void stopAllTasks() {
        for (Thread thread:threads) {
            Log.d(TAG, "stopAllTasks: stop thread");
            thread.interrupt();
        }
    }

    public void startNegotiationServer(final boolean isClient, String macAddress) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Negotiator negotiator = new Negotiator(isClient, getWFDMacAddress());
                String peer_mac_address = null;
                while (!isRoaming && enabled && peer_mac_address == null) {
                    Log.d(TAG, "run: " + enabled);
                    peer_mac_address = negotiator.workAsServer();
                    // ToDo: use other broadcast for this
                    // sendUpdateUIBroadcast();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (peer_mac_address == null) {
                        deletePersistentGroups();
                    }
                }
                if (peer_mac_address != null) {
                    // block the discovery mode due to switch to roaming state
                    setRoaming(true);
                    // tell the UI
                    NegotiationFinalization negFin = PeerStore.getInstance().getLatestFinalization(peer_mac_address);
                    String ssid = negFin.getHotspotName();
                    String key = negFin.getHotspotPassword();
                    sendStartRoamingBroadcast(peer_mac_address, ssid, key);
                }
            }


        };
        if (!isRoaming()) {
            Thread thread = new Thread(task);
            threads.add(thread);
            AsyncTask.execute(thread);
        } else {
            Log.d(TAG, "startNegServer: BLOCKED due to roaming state");
        }
    }

    private void sendStartRoamingBroadcast(String peer_mac_address, String ssid, String key) {
        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.start_roaming");
        local.putExtra("peer_mac_address", peer_mac_address);
        local.putExtra("ssid", ssid);
        local.putExtra("key", key);
        this.sendBroadcast(local);
    }

    public void startNegotiationClient(final InetAddress address, final boolean isClient, String macAddress) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Negotiator negotiator = new Negotiator(isClient, getWFDMacAddress());
                String peer_mac_address = null;
                while (!isRoaming && enabled && peer_mac_address == null) {
                    Log.d(TAG, "run: " + enabled);
                    System.out.println(" *******+ work as client *******");
                    peer_mac_address = negotiator.workAsClient(address.getHostAddress());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (peer_mac_address == null) {
                        deletePersistentGroups();
                    }
                }
                if (peer_mac_address != null) {
                    // block the discovery mode due to switch to roaming state
                    setRoaming(true);
                    // tell the UI
                    NegotiationFinalization negFin = PeerStore.getInstance().getLatestFinalization(peer_mac_address);
                    String ssid = negFin.getHotspotName();
                    String key = negFin.getHotspotPassword();
                    sendStartRoamingBroadcast(peer_mac_address, ssid, key);
                }
            }
        };
        if (!isRoaming()) {
            Thread thread = new Thread(task);
            threads.add(thread);
            AsyncTask.execute(thread);
        } else {
            Log.d(TAG, "startNegotiationClient: BLOCKED due to roaming state");
        }


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

    private void deletePersistentGroups() {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mManager, mChannel, netid, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void enableService() {
        if (!enabled) {
            enabled = true;
            setupService();
            enableWiFi();
        }
    }

    public void disableService(){
        if (enabled) {
            enabled = false;
            stopAllTasks();
            stopService_();
        }
    }

    private void stopService_() {
        if (setup) {
            try {
                unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "stopService_: Illegal argument exception");
                e.printStackTrace();
            }
        }
    }

    private void setupService() {
        if (!setup) {
            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);
            mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

            // delete all old groups
            deletePersistentGroups();

            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            registerReceiver(mReceiver, mIntentFilter);

            setup = true;
        } else {
            registerReceiver(mReceiver, mIntentFilter);
        }
    }

    public void startDiscoveryMode(WifiP2pManager.ActionListener action_listener) {
        mManager.discoverPeers(mChannel, action_listener);
        discoveryModeActive = true;
    }

    public boolean isInRoleHotspot() {
        return inRoleHotspot;
    }

    public void setInRoleHotspot(boolean inRoleHotspot) {
        this.inRoleHotspot = inRoleHotspot;
    }

    public boolean isInRoleConsumer() {
        return inRoleConsumer;
    }

    public void setInRoleConsumer(boolean inRoleConsumer) {
        this.inRoleConsumer = inRoleConsumer;
    }

    public boolean isRoaming() {
        return isRoaming;
    }

    public void setRoaming(boolean roaming) {
        if (roaming) {
            stopAllTasks();
        }
        isRoaming = roaming;
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public WiFiDirectBroadcastService getService() {
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
        stopService_();
    }

    @Override
    public void onCreate() {
        super.onCreate();
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

    public void stopDiscovery(WifiP2pManager.ActionListener action_listener) {
        mManager.stopPeerDiscovery(mChannel, action_listener);
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
            currentDeviceConnected = p2p_info.groupOwnerAddress.getHostAddress();
            sendUpdateUIBroadcastNewConnection();
        }
    }

    public void setConnectionStateChanged(WifiP2pInfo p2p_info, NetworkInfo network_info, WifiP2pGroup p2p_group) {
        this.p2p_info = p2p_info;
        this.network_info = network_info;
        this.p2p_group = p2p_group;
    }

    private void sendUpdateUIBroadcast(){
        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.update_ui");
        this.sendBroadcast(local);
    }

    private void sendUpdateUIBroadcastNewConnection(){
        Log.d(TAG, "sendUpdateUIBroadcastNewConnection: NOTIFY UI ABOUT NEW CONNECTION");
        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.update_ui");
        local.putExtra("what", "connectivity_changed");
        local.putExtra("currentDeviceConnected", currentDeviceConnected);
        this.sendBroadcast(local);
    }

    private void sendUpdateUIBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("com.flashwifi.wifip2p.update_ui");
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