package com.flashwifi.wifip2p.broadcast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.flashwifi.wifip2p.AddressBalanceTransfer;
import com.flashwifi.wifip2p.Constants;
import com.flashwifi.wifip2p.MainActivity;
import com.flashwifi.wifip2p.R;
import com.flashwifi.wifip2p.RoamingActivity;
import com.flashwifi.wifip2p.accesspoint.AccessPointTask;
import com.flashwifi.wifip2p.accesspoint.ConnectTask;
import com.flashwifi.wifip2p.accesspoint.StopAccessPointTask;
import com.flashwifi.wifip2p.billing.Accountant;
import com.flashwifi.wifip2p.billing.BillingClient;
import com.flashwifi.wifip2p.billing.BillingServer;
import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.iotaAPI.Requests.WalletTransferRequest;
import com.flashwifi.wifip2p.negotiation.Negotiator;
import com.flashwifi.wifip2p.protocol.BillingOpenChannel;
import com.flashwifi.wifip2p.protocol.BillingOpenChannelAnswer;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transfer;

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
    private Set<RoamingState> roamingStates;

    // broadcast stuff
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver = null;
    IntentFilter mIntentFilter;

    // content attributes
    ArrayList<String> arrayList = new ArrayList<>();
    WifiP2pInfo p2p_info;
    NetworkInfo network_info;
    WifiP2pGroup p2p_group = null;
    ArrayList<String> receivedMessages = new ArrayList<>();

    // async task store
    ArrayList<Thread> threads = new ArrayList<Thread>();


    private String currentDeviceConnected = null;

    // discovery mode
    private boolean discoveryModeActive = false;

    // HOTSPOT
    // socket stuff
    Thread apTask;
    boolean apRuns = false;
    ConnectTask connectTask;
    private boolean negotiatorRunning = false;
    private String ownMacAddressStore = null;

    private State applicationState = State.READY;
    private String password;
    private String seed;
    private boolean busy;

    private BroadcastReceiver roamingUIReceiver;
    private String peerMacAddress;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");
            password = intent.getStringExtra("password");
            seed = intent.getStringExtra("seed");

            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    getMyActivityNotification(getString(R.string.ready)));
        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
            stopAP();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
            System.exit(0);
        }
        return START_STICKY;
    }

    private Intent getNotificationIntent() {
        Intent notificationIntent = null;
        if  (applicationState == State.HOTSPOT)  {
            notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.HOTSPOT);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else if (applicationState == State.SEARCH) {
            notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.SEARCH);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else if (applicationState == State.P2P_CONNECTING) {
            Log.d(TAG, "getMyActivityNotification: please wait ");
            // ToDo: show progress bar
        } else if (applicationState == State.ROAMING) {
            notificationIntent = new Intent(this, RoamingActivity.class);
            notificationIntent.setAction(Constants.ACTION.ROAMING);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        else {
            notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        notificationIntent.putExtra("password", password);
        notificationIntent.putExtra("seed", seed);
        return notificationIntent;
    }

    public void listenToRoamingBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");
        filter.addAction("com.flashwifi.wifip2p.update_roaming");

        if (roamingUIReceiver == null) {
            roamingUIReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() != null && intent.getAction().equals("com.flashwifi.wifip2p.update_roaming")) {
                        String message = intent.getStringExtra("message");
                        if (message != null) {
                            Log.d(TAG, "updateUi: message=" + message);
                            if (message.equals("AP SUCCESS")) {
                                startBillingProtocol();
                                roamingStates.add(RoamingState.ACCESS_POINT);
                            } else if (message.equals("AP FAILED")) {
                                roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                                exitRoaming();
                            } else if (message.equals("AP CREATION FAILED")) {
                                roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                                exitRoaming();
                            } else if (message.equals("AP STOPPED")) {
                                roamingStates.add(RoamingState.ACCESS_POINT_STOPPED);
                            } else if (message.equals("Channel established")) {
                                roamingStates.add(RoamingState.FLASH_CHANNEL);
                            } else if (message.equals("Start Channel funding")) {
                                fundChannel();
                            } else if (message.equals("Channel funded")) {
                                roamingStates.add(RoamingState.CHANNEL_FUNDED);
                            } else if (message.equals("Billing")) {
                            } else if (message.equals("Channel closed")) {
                                exitRoaming();
                                if (isInRoleHotspot()) {
                                    // ToDo: retransfer stuff
                                }
                            } else if (message.equals("Socket exception")) {
                            } else if (message.equals("Exit")) {
                                exitRoaming();
                            }
                        }

                    }
                }
            };
            registerReceiver(roamingUIReceiver, filter);
        }
    }

    private void exitRoaming() {
        roamingStates.add(RoamingState.EXIT);
        Accountant.getInstance().setCloseAfterwards(true);
        stopRoaming();
    }

    public void stopListenToRoamingBroadcast() {
        if (roamingUIReceiver != null) {
            unregisterReceiver(roamingUIReceiver);
            roamingUIReceiver = null;
        }
    }

    private void startBillingProtocol() {
        resetBillingState();

        Log.d(TAG, "startBillingProtocol: with MAC <<<<<<<<<< " + peerMacAddress);

        if (isInRoleHotspot()) {
            startBillingServer(peerMacAddress);
        } else {
            startBillingClient(peerMacAddress);
        }
    }

    public Set<RoamingState> getRoamingStates() {
        return roamingStates;
    }

    public void addRoamingState(RoamingState roamingState) {
        this.roamingStates.add(roamingState);
    }

    private Notification getMyActivityNotification(String text){
        CharSequence title = getText(R.string.app_name);

        // what to do on click?
        Intent notificationIntent = getNotificationIntent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(this, WiFiDirectBroadcastService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);


        return new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setTicker(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.icon_tethering_off, "Force Quit",
                        pstopIntent).build();
    }

    /**
     * This is the method that can be called to update the Notification
     */
    private void updateNotification(String text) {
        Notification notification = getMyActivityNotification(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
    }

    public void changeApplicationState(State state) {
        this.applicationState = state;
        updateNotification(state.toString());
    }

    public boolean isSetup() {
        return setup;
    }

    public void enableWiFi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
        }
    }

    public void startRoaming(String macAddress, String key, String ssid, String seed, String password) {
        listenToRoamingBroadcast();
        changeApplicationState(State.ROAMING);
        roamingStates = new HashSet<RoamingState>();

        Intent intent = new Intent(this, RoamingActivity.class);
        intent.putExtra("address", macAddress);
        intent.putExtra("key", key);
        intent.putExtra("ssid", ssid);
        intent.putExtra("seed", seed);
        intent.putExtra("password", password);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);


        if (isInRoleHotspot()) {
            startAP(ssid, key);
        } else {
            connect2AP(ssid, key);
        }

        Accountant.getInstance().reset();
    }

    public void disableWiFi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(false);
        }
    }



    public void resetBillingState() {
        billingClientIsRunning = false;
        billingServerIsRunning = false;
    }

    public void startBillingServer(@NonNull String mac){
        if (!billingServerIsRunning) {
            billingServerIsRunning = true;

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: instantiate billing server");
                    // ToDo: Replace seedindex
                    int seedindex = 0;
                    BillingServer billingServer = new BillingServer(mac, getApplicationContext(), seed, seedindex);

                    try {
                        billingServer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // -> close the roaming etc.
                }
            };
            Log.d(TAG, "startBillingServer");
            Thread thread = new Thread(task);
            //asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            //AsyncTask.execute(thread);
            threads.add(thread);
            thread.start();
        } else {
            Log.d(TAG, "startBillingServer: blocked");
        }
    }


    public void startBillingClient(String mac) {
        if (!billingClientIsRunning) {
            billingClientIsRunning = true;

            Log.d(TAG, "startBillingClient");

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    BillingClient billingClient = new BillingClient(mac, getApplicationContext(), seed, 0); // ToDo: remove the magic number
                    // Gget the AP gateway ip address
                    // https://stackoverflow.com/questions/9035784/how-to-know-ip-address-of-the-router-from-code-in-android
                    final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    final DhcpInfo dhcp = manager.getDhcpInfo();
                    byte[] myIPAddress = BigInteger.valueOf(dhcp.gateway).toByteArray();
                    ArrayUtils.reverse(myIPAddress);
                    InetAddress myInetIP = null;
                    Inet6Address myInet6IP = null;
                    String routerIP = null;
                    try {
                        myInetIP = InetAddress.getByAddress(myIPAddress);
                        routerIP = myInetIP.getHostAddress();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        routerIP = "192.168.43.1";
                    }
                    Log.d(TAG, "DHCP gateway: " + routerIP);
                    billingClient.start(routerIP, myInetIP);
                    // ToDo: handle billingServer EXIT CODES
                    // -> close the roaming etc.
                }
            };
            Thread thread = new Thread(task);
            threads.add(thread);
            thread.start();
            //AsyncTask.execute(thread);
        } else {
            Log.d(TAG, "startBillingClient: blocked");
        }
    }

    public void connect2AP(String ssid, String key) {
        Log.d(TAG, "connect2AP: CONNECT TO THE HOTSPOT");

        Runnable task = new Runnable() {
            @Override
            public void run() {

                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

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

                int max_tries = 100;

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
                                            sendUpdateRoamingBroadcastWithMessage("AP SUCCESS");
                                            roamingStates.add(RoamingState.ACCESS_POINT);

                                        } else {
                                            Log.d(TAG, "WRONG NETWORK");
                                            wrong_network = true;
                                        }
                                    }
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    max_seconds--;
                                    if (max_seconds == 0) {
                                        Log.d(TAG, "TRIED TOO LONG TO CONNECT TO HOTSPOT -> stop");
                                        connected = false;
                                    }
                                }

                            } else {
                                Log.d(TAG, "doInBackground: Error connecting to the network");
                                Log.d(TAG, "doInBackground: Let's try it again");
                            }
                        }
                    }
                    if (max_tries == 0) {
                        Log.d(TAG, "doInBackground: Error connecting to the network");
                        Log.d(TAG, "doInBackground: THIS WAS THE LAST TRY");
                        sendUpdateRoamingBroadcastWithMessage("AP FAILED");
                        roamingStates.add(RoamingState.ACCESS_POINT);
                    }

                }

            }
        };

        Thread thread = new Thread(task);
        //threads.add(thread);
        thread.start();
    }


    public void disconnectAP() {
        // ToDo: implement this?
    }

    public void startAP(String ssid, String key) {
        if (!apRuns) {
            Log.d(TAG, "start Access point");
            apRuns = true;

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
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
                                    sendUpdateRoamingBroadcastWithMessage("AP SUCCESS");
                                    roamingStates.add(RoamingState.ACCESS_POINT);
                                }
                                else
                                {
                                    sendUpdateRoamingBroadcastWithMessage("AP CREATION FAILED");
                                    roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                                }
                            }
                            catch (IllegalArgumentException e) {
                                e.printStackTrace();
                                sendUpdateRoamingBroadcastWithMessage("AP CREATION FAILED");
                                roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                            }
                            catch (IllegalAccessException e) {
                                e.printStackTrace();
                                sendUpdateRoamingBroadcastWithMessage("AP CREATION FAILED");
                                roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                            }
                            catch (InvocationTargetException e) {
                                e.printStackTrace();
                                sendUpdateRoamingBroadcastWithMessage("AP CREATION FAILED");
                                roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                            }
                        }
                    }
                    if (!methodFound){
                        sendUpdateRoamingBroadcastWithMessage("AP CREATION FAILED");
                        roamingStates.add(RoamingState.ACCESS_POINT_ERROR);
                        //statusView.setText("Your phone's API does not contain setWifiApEnabled method to configure an access point");
                    }

                    int number_minutes = 60*24;
                    for (int i=0; i<number_minutes; i++) {
                        try {
                            Thread.sleep(1000*60);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "stopAP: THIS IS STOPPING THE AP");
                            WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(WIFI_SERVICE);

                            Method[] methods = wifiManager.getClass().getDeclaredMethods();
                            for (Method method : methods) {
                                if (method.getName().equals("setWifiApEnabled")) {
                                    try {
                                        method.invoke(wifiManager, null, false);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            }

                            if (!wifi.isWifiEnabled())
                            {
                                wifi.setWifiEnabled(true);
                            }
                            roamingStates.add(RoamingState.ACCESS_POINT_STOPPED);
                            break;
                        }
                    }
                }
            };

            Thread thread = new Thread(task);
            apTask = thread;
            thread.start();

        } else {
            Log.d(TAG, "Access point ALREADY RUNNING");
        }
    }

    public void stopAP() {
        Log.d(TAG, "stop AP");
        if (apRuns) {
            apRuns = false;
            apTask.interrupt();
        } else {
            Log.d("", "AP was not running");
        }
    }

    private void stopAllTasks() {
        for (Thread thread:threads) {
            Log.d(TAG, "stopAllTasks: stop thread");
            thread.interrupt();
        }
    }



    public void startNegotiationServer(final boolean isClient, String macAddress, String peerMac) {
        // ToDo: settlementAddress
        String settlementAddress = "RDNUSLPNOQUGDIZVOINTYRIRRIJMLODOC9ZTQU9KQSCDXPVSBILXUE9AHEOA9MNYZWNSECAVPQ9QSAHCN";


        Runnable task = new Runnable() {
            @Override
            public void run() {
                Negotiator negotiator = new Negotiator(
                        isClient,
                        macAddress,
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                        getBaseContext(),
                        peerMac,
                        "IUQDBHFDXK9EHKC9VUHCUXDLICLRANNDHYRMDYFCGSZMROWCZBLBNRKXWBSWZYDMLLHIHMP9ZPOPIFUSW"// settlementAddress
                        );

                String peer_mac_address = null;
                boolean restartAfterwards = true;
                int error_count = 0;
                int max_error_count = 10;
                Negotiator.NegotiationReturn ret = null;
                while (!isRoaming && enabled && peer_mac_address == null && restartAfterwards && (error_count < max_error_count)) {
                    Log.d(TAG, String.format("%d/%d errors", error_count, max_error_count));
                    Log.d(TAG, "run: " + enabled);
                    ret = negotiator.workAsServer();
                    peer_mac_address = ret.mac;
                    restartAfterwards = ret.restartAfterwards;
                    if (ret.code != 0) {
                        sendUpdateUIBroadcastWithMessage(getString(ret.code));
                        if (restartAfterwards) {
                            error_count++;
                        }
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        error_count++;
                    }
                    if (peer_mac_address == null) {
                        deletePersistentGroups();
                    }
                }
                if (peer_mac_address != null && ret != null && ret.code == 0) {
                    // block the discovery mode due to switch to roaming state
                    setRoaming(true);
                    // tell the UI
                    NegotiationFinalization negFin = PeerStore.getInstance().getLatestFinalization(peer_mac_address);
                    String ssid = negFin.getHotspotName();
                    String key = negFin.getHotspotPassword();
                    Log.d(TAG, "YYY: " + peer_mac_address);
                    sendStartRoamingBroadcast(peer_mac_address, ssid, key);
                } else if (peer_mac_address != null) {
                    PeerStore.getInstance().unselectAll();
                }
                negotiatorRunning = false;
            }


        };
        if (!isRoaming() && !negotiatorRunning) {
            negotiatorRunning = true;
            Thread thread = new Thread(task);
            threads.add(thread);
            //AsyncTask.execute(thread);
            stopAllTasks();
            thread.start();
            //stopDiscovery(null);
        } else {
            Log.d(TAG, "startNegServer: BLOCKED due to roaming state");
        }
    }

    private void sendStartRoamingBroadcast(String peer_mac_address, String ssid, String key) {
        startRoaming(peer_mac_address, key, ssid, seed, password);
        this.peerMacAddress = peer_mac_address;

        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.start_roaming");
        local.putExtra("peer_mac_address", peer_mac_address);
        local.putExtra("ssid", ssid);
        local.putExtra("key", key);
        this.sendBroadcast(local);
    }

    public void startNegotiationClient(final InetAddress address, final boolean isClient, String macAddress, String peerMacAddress, String settlementAddress) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Negotiator negotiator = new Negotiator(
                        isClient,
                        macAddress,
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                        getBaseContext(),
                        peerMacAddress,
                        "RDNUSLPNOQUGDIZVOINTYRIRRIJMLODOC9ZTQU9KQSCDXPVSBILXUE9AHEOA9MNYZWNSECAVPQ9QSAHCN" //settlementAddress
                );
                String peer_mac_address = null;
                boolean restartAfterwards = true;
                Negotiator.NegotiationReturn negotiationReturn = null;
                while (!isRoaming && enabled && peer_mac_address == null && restartAfterwards && negotiatorRunning) {
                    Log.d(TAG, "run: " + enabled);
                    System.out.println(" *******+ work as client *******");
                    negotiationReturn = negotiator.workAsClient(address.getHostAddress());
                    peer_mac_address = negotiationReturn.mac;
                    restartAfterwards = negotiationReturn.restartAfterwards;
                    if (negotiationReturn.code != 0) {
                        sendUpdateUIBroadcastWithMessage(getString(negotiationReturn.code));
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (peer_mac_address == null) {
                        deletePersistentGroups();
                    }
                }
                if (peer_mac_address != null && negotiationReturn != null && negotiationReturn.code == 0) {
                    // block the discovery mode due to switch to roaming state
                    setRoaming(true);
                    // tell the UI
                    NegotiationFinalization negFin = PeerStore.getInstance().getLatestFinalization(peer_mac_address);
                    String ssid = negFin.getHotspotName();
                    String key = negFin.getHotspotPassword();
                    Log.d(TAG, "ZZZ: " + peer_mac_address);
                    sendStartRoamingBroadcast(peer_mac_address, ssid, key);
                } else {
                    Log.d(TAG, "run: could not start roaming");
                }
                PeerStore.getInstance().unselectAll();
                negotiatorRunning = false;
            }
        };
        if (!isRoaming() && !negotiatorRunning) {
            negotiatorRunning = true;
            Thread thread = new Thread(task);
            threads.add(thread);
            stopAllTasks();
            thread.start();
        } else {
            Log.d(TAG, "startNegotiationClient: BLOCKED due to roaming state or negotiator running");
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
        if (setup) {
            mManager.discoverPeers(mChannel, action_listener);
            discoveryModeActive = true;
        }
    }

    public boolean isInRoleHotspot() {
        return inRoleHotspot;
    }

    public void setInRoleHotspot(boolean inRoleHotspot) {
        if (inRoleHotspot) {
            changeApplicationState(State.HOTSPOT);
        }
        this.inRoleHotspot = inRoleHotspot;
    }

    public boolean isInRoleConsumer() {
        return inRoleConsumer;
    }

    public void setInRoleConsumer(boolean inRoleConsumer) {
        if (inRoleConsumer) {
            changeApplicationState(State.SEARCH);
        }
        this.inRoleConsumer = inRoleConsumer;
    }

    public boolean isRoaming() {
        return isRoaming;
    }

    public void setRoaming(boolean roaming) {
        if (roaming) {
            changeApplicationState(State.ROAMING);
        } else {
            stopAllTasks();
        }
        isRoaming = roaming;
    }

    public void freezeWiFiP2P() {

    }

    public void fundChannel() {
        String address = this.peerMacAddress;
        // get the necessary data
        NegotiationOffer offer = PeerStore.getInstance().getLatestNegotiationOffer(address);
        NegotiationOfferAnswer offerAnser = PeerStore.getInstance().getLatestNegotiationOfferAnswer(address);
        NegotiationFinalization finalization = PeerStore.getInstance().getLatestFinalization(address);
        BillingOpenChannel openChannel = PeerStore.getInstance().getLatestBillingOpenChannel(address);
        BillingOpenChannelAnswer openChannelAnswer = PeerStore.getInstance().getLatestBillingOpenChannelAnswer(address);

        String multisigAddress = finalization.getDepositAddressFlashChannel();
        int timeoutClientSeconds, timeoutHotspotSeconds;

        // ToDo: Fix this bug!
        // openChannel should not be null here
        if (openChannel != null) {
            timeoutClientSeconds = openChannel.getTimeoutMinutesClient();
            timeoutHotspotSeconds = openChannelAnswer.getTimeoutMinutesHotspot() * 60;
        } else {
            timeoutClientSeconds = 3;
            timeoutHotspotSeconds = 3 * 60;
        }
        int timeout = (isInRoleHotspot()) ? timeoutHotspotSeconds : timeoutClientSeconds;

        int clientDeposit = finalization.getDepositClientFlashChannelInIota();
        int hotspotDeposit = finalization.getDepositServerFlashChannelInIota();
        int deposit = (isInRoleHotspot()) ? hotspotDeposit : clientDeposit;

        int depositTogether = clientDeposit + hotspotDeposit;

        String seed = Accountant.getInstance().getSeed();

        Log.d(TAG, "fundChannel: Let's fund " + multisigAddress);


        Runnable task = () -> {
            Log.d(TAG, "run: fund multisig address");

            boolean fundDone = false;
            boolean fundIsThere = false;

            int fundErrorCount = 0;
            int maxFundErrors = 10;

            SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean testnet = prefManager.getBoolean("pref_key_switch_testnet",false);

            IotaAPI api;

            if(testnet){
                //Testnet node:
                api = new IotaAPI.Builder()
                        .protocol("https")
                        .host("testnet140.tangle.works")
                        .port("443")
                        .build();
            }
            else{
                //Mainnet node:
                api = new IotaAPI.Builder()
                        .protocol("http")
                        .host("node.iotawallet.info")
                        .port("14265")
                        .build();
            }

            long start = System.currentTimeMillis() / 1000L;

            while (((System.currentTimeMillis()/1000L) - start < timeout || !fundDone || !fundIsThere) && fundErrorCount < maxFundErrors) {
                SendTransferResponse sendTransferResponse = null;
                if (!fundDone) {
                    Log.d(TAG, "fundChannel: DEPOSIT::" + Integer.toString(deposit));
                    try {
                        List<Transfer> transfers = new ArrayList<>();
                        transfers.add(new Transfer(multisigAddress, deposit, "", ""));

                        if(testnet) {
                            Log.d(TAG, "fundChannel: fund on testnet");
                            sendTransferResponse = api.sendTransfer(seed, 2, 4, 9, transfers, null, null, false);
                        }
                        else{
                            Log.d(TAG, "fundChannel: fund on mainnet");
                            sendTransferResponse = api.sendTransfer(seed, 2, 4, 18, transfers, null, null, false);
                        }
                    } catch (ArgumentException | IllegalAccessError | IllegalStateException e) {
                        String transferResult = "";
                        if (e instanceof ArgumentException) {
                            if (e.getMessage().contains("Sending to a used address.") || e.getMessage().contains("Private key reuse detect!") || e.getMessage().contains("Send to inputs!")) {
                                transferResult = "Sending to a used address/Private key reuse detect. Error Occurred.";
                            }
                            else if(e.getMessage().contains("Failed to connect to node")){
                                transferResult = "Failed to connect to node";
                            }
                            else{
                                transferResult = "Network Error: Attaching new address failed or Transaction failed";
                            }
                        }
                        if (e instanceof IllegalAccessError) {
                            transferResult = "Local POW needs to be enabled";
                        }
                        fundErrorCount++;
                        Log.d(TAG, "fundChannel: " + transferResult);
                    }

                    if(sendTransferResponse != null){
                        Boolean[] transferSuccess = sendTransferResponse.getSuccessfully();

                        Boolean success = true;
                        for (Boolean status : transferSuccess) {
                            if(status.equals(false)){
                                success = false;
                                fundErrorCount++;
                                break;
                            }
                        }
                        if(success){
                            Log.d(TAG, "fundChannel: successfully transferred my part");
                            fundDone = true;
                        }
                    }

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else if (fundDone && !fundIsThere) {
                    // now check the balance until we get it
                    // check the funding
                    Log.d(TAG, "fundChannel: checking the balance of the root...");
                    List<String> addresses = new ArrayList<String>();
                    addresses.add(multisigAddress);
                    GetBalancesResponse response = null;
                    try {
                        response = api.getBalances(100, addresses);
                    } catch (ArgumentException e) {
                        e.printStackTrace();
                    }
                    if (response != null) {
                        if (response.getBalances().length > 0) {
                            long balance = Integer.parseInt(response.getBalances()[0]);
                            Log.d(TAG, "fundChannel: Found balance " + response.getBalances()[0]);
                            if (balance > depositTogether) {
                                Log.d(TAG, "fundChannel: balance is enough on both sides");
                                fundIsThere = true;
                            }
                        }
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                
            }
            // ToDo: remove this mock
            if (fundErrorCount < maxFundErrors && false) {
                Log.d(TAG, "fundChannel: channel funded");
                sendUpdateRoamingBroadcastWithMessage("Channel funded");
                roamingStates.add(RoamingState.CHANNEL_FUNDED);
            } else {
                Log.d(TAG, "fundChannel: too many fund errors");
                roamingStates.add(RoamingState.FUNDING_ERROR);
            }


        };

        Log.d(TAG, "startFunding");
        Thread thread = new Thread(task);
        threads.add(thread);
        thread.start();

    }

    public void setP2PGroup(WifiP2pGroup p2PGroup) {
        this.p2p_group = p2PGroup;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public void stopRoaming() {
        if (isInRoleHotspot()){
            stopAP();
        } else {
            disconnectAP();
        }

        setRoaming(false);
        resetBillingState();
        setInRoleConsumer(false);
        setInRoleHotspot(false);
        setBusy(false);


        PeerStore.getInstance().clear();
        stopListenToRoamingBroadcast();
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
        if (setup) {
            mManager.discoverPeers(mChannel, action_listener);
        }
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
        updateNotificationNumberPeers(arrayList.size());
    }

    @SuppressLint("DefaultLocale")
    private void updateNotificationNumberPeers(int size) {
        if (applicationState == State.SEARCH || applicationState == State.HOTSPOT) {
            updateNotification(applicationState + ": " + String.format("%d peers", size));
        }
    }

    public void setNewIncomingConnection(WifiP2pInfo wifiP2pInfo, String ownerMac, String clientMac){
        // This method is called when a new device connected to this one
        this.p2p_info = wifiP2pInfo;
        if (p2p_info.groupFormed) {
            currentDeviceConnected = p2p_info.groupOwnerAddress.getHostAddress();
            sendUpdateUIBroadcastNewConnection();
            NetworkInfo network_info = getNetwork_info();
            WifiP2pInfo p2p_info = getP2p_info();

            if (network_info.getState() == NetworkInfo.State.CONNECTED) {
                // ToDo: look for the other device and make sure we are only two
                if (p2p_info.isGroupOwner) {
                    startNegotiationServer(isInRoleConsumer(), ownerMac, clientMac);
                } else {
                    InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                    // ToDo: get the settlementAddress
                    String settlementAddress = "RDNUSLPNOQUGDIZVOINTYRIRRIJMLODOC9ZTQU9KQSCDXPVSBILXUE9AHEOA9MNYZWNSECAVPQ9QSAHCN";
                    startNegotiationClient(groupOwnerAddress, isInRoleConsumer(), clientMac, ownerMac, settlementAddress);
                }

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

    private void sendUpdateRoamingBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("com.flashwifi.wifip2p.update_roaming");
        this.sendBroadcast(local);
    }

    // groupOwnerIntent determines how much you want to become the group onwer
    // 0 means little and 15 means a lot
    // https://stackoverflow.com/questions/18703881/how-to-make-a-specific-group-owner-in-wifi-direct-android
    public void connect(String address, WifiP2pManager.ActionListener actionListener) {
        if (!busy) {
            busy = true;

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: connect");
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = address;
                    config.groupOwnerIntent = 0;
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Connected to peer", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(getApplicationContext(), "Error connecting to peer", Toast.LENGTH_SHORT).show();
                            busy = false;
                        }
                    });
                }
            };
            Log.d(TAG, "start connect thread");
            Thread thread = new Thread(task);
            threads.add(thread);
            thread.start();
        } else {
            Log.d(TAG, "connecting blocked due to business");
        }
    }

    public enum State {
        READY,
        SEARCH,
        HOTSPOT,
        P2P_CONNECTING,
        NEGOTIATING,
        ROAMING,


        FUND_WALLET,
        WITHDRAW_WALLET,
        SETTINGS
    }

    public enum RoamingState {
        ACCESS_POINT,
        FLASH_CHANNEL,
        CHANNEL_FUNDED,
        END_REQUESTED,
        EXIT,
        CHANNEL_ATTACHED,
        ACCESS_POINT_STOPPED,


        FUNDING_ERROR,
        ACCESS_POINT_ERROR,
        ERROR

    }
}