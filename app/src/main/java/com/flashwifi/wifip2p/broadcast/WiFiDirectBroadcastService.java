package com.flashwifi.wifip2p.broadcast;
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
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    AccessPointTask apTask;
    boolean apRuns = false;
    ConnectTask connectTask;
    private boolean negotiatorRunning = false;
    private String ownMacAddressStore = null;

    private State applicationState = State.READY;
    private String password;
    private String seed;
    private boolean busy;

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

    private Notification getMyActivityNotification(String text){
        CharSequence title = getText(R.string.app_name);

        // what to do on click?
        Intent notificationIntent = getNotificationIntent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(this, WiFiDirectBroadcastService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.icon_tethering_on);

        return new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setTicker(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.icon_tethering_on)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.icon_tethering_off, "Stop",
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
        Intent intent = new Intent(this, RoamingActivity.class);
        intent.putExtra("address", macAddress);
        intent.putExtra("key", key);
        intent.putExtra("ssid", ssid);
        intent.putExtra("seed", seed);
        intent.putExtra("password", password);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        changeApplicationState(State.ROAMING);
        startActivity(intent);
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
                    BillingServer billingServer = new BillingServer(mac, getApplicationContext());

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
                    BillingClient billingClient = new BillingClient(mac, getApplicationContext());
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
        Log.d(TAG, "getWFDMacAddress: GET MAC ADRESS =========================");
        if (ownMacAddressStore != null) {
            return ownMacAddressStore;
        }
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ntwInterface : interfaces) {

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

                Log.d(TAG, "getWFDMacAddress: " + strBuilder.toString());

                if (ntwInterface.getName().equalsIgnoreCase("p2p0")) {
                    this.ownMacAddressStore = strBuilder.toString();
                }

            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return this.ownMacAddressStore;
    }

    private void stopAllTasks() {
        for (Thread thread:threads) {
            Log.d(TAG, "stopAllTasks: stop thread");
            thread.interrupt();
        }
    }


    public void startNegotiationServer(final boolean isClient, String macAddress, String peerMac) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Negotiator negotiator = new Negotiator(
                        isClient,
                        macAddress,
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                        getBaseContext(),
                        peerMac
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


        Intent local = new Intent();
        local.setAction("com.flashwifi.wifip2p.start_roaming");
        local.putExtra("peer_mac_address", peer_mac_address);
        local.putExtra("ssid", ssid);
        local.putExtra("key", key);
        this.sendBroadcast(local);
    }

    public void startNegotiationClient(final InetAddress address, final boolean isClient, String macAddress, String peerMacAddress) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Negotiator negotiator = new Negotiator(
                        isClient,
                        macAddress,
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                        getBaseContext(),
                        peerMacAddress
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
        if (!roaming) {
            stopAllTasks();
        }
        isRoaming = roaming;
    }

    public void freezeWiFiP2P() {

    }

    public void fundChannel(String address) {
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
            if (fundErrorCount < maxFundErrors) {
                Log.d(TAG, "fundChannel: channel funded");
                sendUpdateRoamingBroadcastWithMessage("Channel funded");
            } else {
                Log.d(TAG, "fundChannel: too many fund errors");
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
                    startNegotiationClient(groupOwnerAddress, isInRoleConsumer(), clientMac, ownerMac);
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
                            // start the protocol
                            /*WifiP2pInfo p2p_info = getP2p_info();
                            NetworkInfo network_info = getNetwork_info();

                            Log.d(TAG, "onSuccess: Connection was successful");
                            int max_tries = 10;

                            while (busy && max_tries > 0) {
                                max_tries --;
                                int finalMax_tries = max_tries;
                                mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                                    @Override
                                    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {

                                        if (wifiP2pGroup == null || wifiP2pGroup.getClientList().size() == 0 || wifiP2pGroup.getOwner() == null) {
                                            Log.d(TAG, "onGroupInfoAvailable: NOOOOOO CLIENTS????");
                                            Toast.makeText(getApplicationContext(), "The P2P group is missing owner or members", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.d(TAG, wifiP2pGroup.toString());
                                            String clientMac = ((WifiP2pDevice) wifiP2pGroup.getClientList().toArray()[0]).deviceAddress;
                                            String ownerMac = wifiP2pGroup.getOwner().deviceAddress;

                                            busy = false;

                                            if (p2p_info.isGroupOwner) {
                                                Log.d(TAG, "You are the group owner");
                                                startNegotiationServer(true, ownerMac, address);
                                                Log.d(TAG, "SocketServer started");
                                            } else {
                                                Log.d(TAG, "You are a member of the group");
                                                // groupOwnerAddress = p2p_info.groupOwnerAddress;
                                                InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                                                Log.d(TAG, "Group owner address: " + p2p_info.groupOwnerAddress.getHostAddress());
                                                startNegotiationClient(groupOwnerAddress, true, clientMac, address);
                                                Log.d(TAG, "Client Socket started");
                                            }
                                        }
                                    }
                                });
                            }
                            busy = false;
                            Log.d(TAG, "onSuccess: done with connecting");*/
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
            //asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            //AsyncTask.execute(thread);
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
}