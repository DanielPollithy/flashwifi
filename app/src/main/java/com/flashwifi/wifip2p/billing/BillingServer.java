package com.flashwifi.wifip2p.billing;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.util.Log;

import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.negotiation.SocketWrapper;
import com.flashwifi.wifip2p.protocol.BillMessage;
import com.flashwifi.wifip2p.protocol.BillMessageAnswer;
import com.flashwifi.wifip2p.protocol.BillingCloseChannel;
import com.flashwifi.wifip2p.protocol.BillingCloseChannelAnswer;
import com.flashwifi.wifip2p.protocol.BillingOpenChannel;
import com.flashwifi.wifip2p.protocol.BillingOpenChannelAnswer;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import static android.content.Context.WIFI_SERVICE;

/**
 * 1) This class keeps the socket connection alive.
 * 2) It tracks the state of the communication.
 * 3) It triggers the creation of bills
 * 4) It sends the bills and flash objects to the peer
 * 5) It watches the time and deadlines
 * 6)
 */

public class BillingServer {
    private static final String TAG = "BillingServer";
    private final Gson gson;
    private final String mac;
    private final String hotspotRefundAddress;
    private final String channelRootAddress;
    private final String[] digests;
    private State state = State.NOT_PAIRED;
    private ServerSocket serverSocket;
    private Socket socket;
    private SocketWrapper socketWrapper;
    private int PORT = 9199;
    private static final int serverTimeoutMillis = 2 * 60 * 1000;

    Context context;
    NetworkStatsManager networkStatsManager;

    private void sendUpdateUIBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("com.flashwifi.wifip2p.update_roaming");
        context.sendBroadcast(local);
    }

    public BillingServer(String mac, Context context){
        this.context = context;
        this.mac = mac;

        Log.d(TAG, "BillingServer: MAC of other part: " + mac);

        // get the negotiated data
        NegotiationOffer offer = PeerStore.getInstance().getLatestNegotiationOffer(mac);
        NegotiationOfferAnswer answer = PeerStore.getInstance().getLatestNegotiationOfferAnswer(mac);
        NegotiationFinalization finalization = PeerStore.getInstance().getLatestFinalization(mac);
        // get the necessary values
        // ToDo: replace magic number with setting
        int totalMegabytes = 100;
        int treeDepth = 8;
        this.digests = new String[]{"1234", "2345", "3456"};
        int timeoutMinutesServer = 2 * 60 * 1000;

        this.hotspotRefundAddress = finalization.getHotspotRefundAddress();
        this.channelRootAddress = finalization.getDepositAddressFlashChannel();


        int iotaPerMegabyte = offer.getIotaPerMegabyte();
        int iotaDepositClient = totalMegabytes * iotaPerMegabyte;
        String clientRefundAddress = finalization.getClientRefundAddress();
        int totalMinutes = answer.getDuranceInMinutes();

        Accountant.getInstance().start(totalMegabytes, timeoutMinutesServer, totalMinutes, iotaDepositClient, iotaPerMegabyte);
        gson = new GsonBuilder().create();
        networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
    }

    public void start() throws IOException {
        // 0) create deadline guard
        createDeadlineGuard();
        // 1) create a socket

        Log.d(TAG, "start: Billing server has been started");

        try {
            Log.d(TAG, "startBillingProtocol: wait for some milliseconds");
            Thread.sleep(5000);
            Log.d(TAG, "startBillingProtocol: now let's go");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long start_bytes_received = TrafficStats.getTotalRxBytes();
        long start_bytes_transmitted = TrafficStats.getTotalTxBytes();


        int max_errors = 1;
        int errors = 0;

        while (state != State.CLOSED && state != State.ERROR && errors < max_errors) {
            try {
                // create server socket
                serverSocket = new ServerSocket(PORT);
                serverSocket.setSoTimeout(serverTimeoutMillis);
                Log.d(TAG, "doInBackground: Server is waiting for connection");

                socket = serverSocket.accept();
                socket.setSoTimeout(serverTimeoutMillis);
                Log.d(TAG, "start: Server accepted a connection");

                // wrap the socket
                socketWrapper = new SocketWrapper(socket);

                if (state == State.NOT_PAIRED) {
                    // await current state of client
                    String clientState = socketWrapper.getLineThrowing();
                    if (!clientState.contains("NOT_PAIRED")) {
                        Log.d(TAG, "start: PROBLEM PROBLEM PROBLEM! States not synced.");
                        // ToDo: How to synchronize client and server states?
                    }
                    // ToDo: send an eventually stored flash channel
                    socketWrapper.sendLine("Hello, Client! I am in state: NOT_PAIRED");

                    // receive the BillingOpenChannel message
                    String billingOpenChannelString = socketWrapper.getLineThrowing();
                    BillingOpenChannel billingOpenChannel = gson.fromJson(billingOpenChannelString, BillingOpenChannel.class);
                    PeerStore.getInstance().setLatestBillingOpenChannel(mac, billingOpenChannel);
                    // answer with billingOpenChannelAnswerString


                    // ToDo: create the flash channel

                    
                    BillingOpenChannelAnswer billingOpenChannelAnswer = new BillingOpenChannelAnswer(
                            Accountant.getInstance().getTotalIotaDeposit(),
                            Accountant.getInstance().getTotalIotaDeposit(),
                            hotspotRefundAddress,
                            channelRootAddress,
                            digests);
                    PeerStore.getInstance().setLatestBillingOpenChannelAnswer(mac, billingOpenChannelAnswer);
                    String billingOpenChannelAnswerString = gson.toJson(billingOpenChannelAnswer);
                    socketWrapper.sendLine(billingOpenChannelAnswerString);

                    sendUpdateUIBroadcastWithMessage("Channel established");

                    // OK
                    state = State.ROAMING;

                    // start funding
                    sendUpdateUIBroadcastWithMessage("Start Channel funding");
                }

                if (state == State.ROAMING) {
                    String latestBillString;
                    BillMessage latestBill;
                    Bill b;
                    String latestBillAnswerString;
                    BillMessageAnswer latestBillAnswer;

                    // loop until roaming ends
                    int count = 0;
                    int billing_interval_seconds = 60;

                    while (state == State.ROAMING) {
                        // sleep 1 minute
                        Log.d(TAG, "start: I go to sleep for 1 minute!");
                        Thread.sleep(billing_interval_seconds * 1000);
                        Log.d(TAG, "start: Good morning!");
                        // create new bill
                        long total_bytes;
                        NetworkStats  networkStats;

                        long new_bytes_received = TrafficStats.getTotalRxBytes();
                        long new_bytes_transmitted = TrafficStats.getTotalTxBytes();

                        long bytes_received = new_bytes_received - start_bytes_received;
                        long bytes_transmitted = new_bytes_transmitted - start_bytes_transmitted;

                        start_bytes_received = new_bytes_received;
                        start_bytes_transmitted = new_bytes_transmitted;

                        total_bytes = bytes_received + bytes_transmitted;

                        Log.d(TAG, "Bytes Received" + bytes_received);
                        Log.d(TAG, "Bytes Transferred" + bytes_transmitted);

                        b = Accountant.getInstance().createBill((int)total_bytes);

                        // check if the resources are empty
                        boolean closeAfterwards = false;
                        if (Accountant.getInstance().shouldCloseChannel()) {
                            Log.d(TAG, "start: SHOULD CLOSE CHANNEL!");
                            closeAfterwards = true;
                        }


                        // ToDo: integrate real flash channel
                        latestBill = new BillMessage(b, "<flash obj>", Accountant.getInstance().isCloseAfterwards() || closeAfterwards);
                        latestBillString = gson.toJson(latestBill);
                        socketWrapper.sendLine(latestBillString);

                        // get answer
                        latestBillAnswerString = socketWrapper.getLineThrowing();
                        latestBillAnswer = gson.fromJson(latestBillAnswerString, BillMessageAnswer.class);
                        // ToDo: check signature
                        // ToDo: flash object -> diff()
                        // ToDo: update UI

                        // close the channel if the last invoice has <closeAfterwards>
                        // or the last answer had <closeAfterwards>
                        if (latestBill.isCloseAfterwards() || latestBillAnswer.isCloseAfterwards()) {
                            state = State.CLOSE;
                        }

                        sendUpdateUIBroadcastWithMessage("Billing");
                    }

                }

                if (state == State.CLOSE) {
                    Log.d(TAG, "start: state is CLOSE now");
                    // ToDo: handle the final deposit of the flash channel
                    // ToDo: sign the transaction
                    BillingCloseChannel billingCloseChannel = new BillingCloseChannel(0,0,0,0,"", "", "");
                    String closeChannelString = gson.toJson(billingCloseChannel);
                    socketWrapper.sendLine(closeChannelString);

                    String billingCloseChannelAnswerString = socketWrapper.getLineThrowing();
                    BillingCloseChannelAnswer billingCloseChannelAnswer = gson.fromJson(billingCloseChannelAnswerString, BillingCloseChannelAnswer.class);
                    // ToDo: validate the signature

                    // change the ui
                    sendUpdateUIBroadcastWithMessage("Channel closed");
                    state = State.CLOSED;
                }

                if (state == State.CLOSED) {
                    // ToDo: attach the last transaction to the tangle
                    boolean attached = false;
                    while (!attached) {
                        Log.d(TAG, "start: Attach to tangle please");
                        Thread.sleep(5000);
                    }
                    state = State.FULLY_ATTACHED;
                }
            } catch (SocketException e) {
                e.printStackTrace();
                sendUpdateUIBroadcastWithMessage("Socket exception");
                errors++;
            } catch (IOException e) {
                e.printStackTrace();
                sendUpdateUIBroadcastWithMessage("IOException");
                errors++;
            } catch (InterruptedException e) {
                sendUpdateUIBroadcastWithMessage("InterruptedException");
                e.printStackTrace();
                errors++;
            } catch (Exception e) {
                e.printStackTrace();
                errors++;
            } finally {
                try {
                    if (socketWrapper != null) {
                        socketWrapper.close();
                    }
                } catch (Exception e) {

                }
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (Exception e) {

                }
            }
            if (errors >= max_errors) {
                Log.d(TAG, "start: error count too high");
                state = State.ERROR;
                sendUpdateUIBroadcastWithMessage("Exit");
            }
        }

    }


    private void createDeadlineGuard() {
        // this method measures the time and stops the connection if
        // nothing happened after <timeoutMinutes>
    }

    enum State {
        NOT_PAIRED,
        INITIAL,
        ROAMING,
        CLOSE,
        CLOSED,
        FULLY_ATTACHED,
        ERROR
    }
}
