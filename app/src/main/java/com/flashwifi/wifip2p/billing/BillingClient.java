package com.flashwifi.wifip2p.billing;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import com.flashwifi.wifip2p.datastore.PeerInformation;
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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import static android.content.Context.WIFI_SERVICE;

/**
 * 1) This class keeps the socket connection alive.
 * 2) It tracks the state of the communication.
 * 3) It receives new bills and flash objects sent by the server
 * 4) It approves the changes
 * 5) It watches the connectivity (no internet -> no money)
 */

public class BillingClient {
    private static final String TAG = "BillingClient";
    private final Gson gson;
    private final String mac;
    private State state = State.NOT_PAIRED;
    private Socket socket;
    private SocketWrapper socketWrapper;
    private static final int PORT = 9199;
    private static final int clientTimeoutMillis = 2 * 60 * 1000;
    private static final int maxErrorCount = 5;

    private BillingOpenChannel billingOpenChannel;
    private BillingOpenChannelAnswer billingOpenChannelAnswer;
    private BillMessage[] billMessages;

    Context context;

    private void sendUpdateUIBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("com.flashwifi.wifip2p.update_roaming");
        context.sendBroadcast(local);
    }

    public BillingClient(String mac, Context context){
        this.context = context;
        this.mac = mac;
        gson = new GsonBuilder().create();
    }



    public void start(String serverIPAddress, InetAddress inetAddress) {
        int error_count = 0;

        try {
            Log.d(TAG, "startBillingProtocol: wait for some milliseconds");
            Thread.sleep(5000);
            Log.d(TAG, "startBillingProtocol: now let's go");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (state != State.CLOSED && state != State.ERROR) {
            try {
                // create client socket that connects to server
                Log.d(TAG, "try to connect to 192.168.43.1 PORT " + Integer.toString(PORT));
                socket = new Socket("192.168.43.1", PORT);
                socket.setSoTimeout(clientTimeoutMillis);
                Log.d(TAG, "connection is established");



                // wrap the socket
                socketWrapper = new SocketWrapper(socket);
                Log.d(TAG, "socket wrapped");

                Log.d(TAG, "We are in state: " + state);

                if (state == State.NOT_PAIRED) {
                    // send current state
                    socketWrapper.sendLine("Hello, Hotspot! I am in state: NOT_PAIRED");
                    // receive the current state of the server
                    // and whether the flash channel is established
                    String hotspotStateLine = socketWrapper.getLineThrowing();
                    if (hotspotStateLine.contains("INITIAL") || hotspotStateLine.contains("NOT_PAIRED")) {
                        // ask the hotspot to open the flash channel

                        // get the negotiated data
                        // get the necessary values
                        NegotiationOffer offer = PeerStore.getInstance().getLatestNegotiationOffer(mac);
                        NegotiationOfferAnswer answer = PeerStore.getInstance().getLatestNegotiationOfferAnswer(mac);
                        NegotiationFinalization finalization = PeerStore.getInstance().getLatestFinalization(mac);

                        // ToDo: replace magic number with setting
                        int totalMegabytes = 100;
                        int treeDepth = 8;
                        String[] digests = new String[]{"1234", "2345", "3456"};
                        int timeoutMinutesClient = 20 * 60 * 1000;

                        int iotaPerMegabyte = offer.getIotaPerMegabyte();
                        String clientRefundAddress = finalization.getClientRefundAddress();
                        int totalMinutes = answer.getDuranceInMinutes();

                        billingOpenChannel = new BillingOpenChannel(totalMegabytes, iotaPerMegabyte, clientRefundAddress, treeDepth, digests, timeoutMinutesClient, totalMinutes);
                        PeerStore.getInstance().setLatestBillingOpenChannel(mac, billingOpenChannel);
                        String billingOpenChannelString = gson.toJson(billingOpenChannel);
                        socketWrapper.sendLine(billingOpenChannelString);
                        // receive the hotspot details for the flash channel
                        String billingOpenChannelAnswerString = socketWrapper.getLineThrowing();
                        billingOpenChannelAnswer = gson.fromJson(billingOpenChannelAnswerString, BillingOpenChannelAnswer.class);
                        PeerStore.getInstance().setLatestBillingOpenChannelAnswer(mac, billingOpenChannelAnswer);
                        // now create the flash channel on our side
                        Accountant.getInstance().start(billingOpenChannel.getTotalMegabytes(), billingOpenChannel.getTimeoutMinutesClient(), billingOpenChannel.getTotalMinutes(), billingOpenChannelAnswer.getClientDepositIota(),
                                billingOpenChannel.getIotaPerMegabyte());
                        sendUpdateUIBroadcastWithMessage("Channel established");
                        state = State.ROAMING;

                        // start the task to fund the channel
                        sendUpdateUIBroadcastWithMessage("Start Channel funding");

                    } else {
                        // what to do if the hotspot already created stuff and was in roaming mode
                        // ToDo: ^^^^^
                    }
                }

                if (state == State.ROAMING) {
                    String latestBillString;
                    BillMessage latestBill;
                    String latestBillAnswerString;
                    BillMessageAnswer latestBillAnswer;

                    // loop until roaming ends
                    while (state == State.ROAMING) {
                        latestBillString = socketWrapper.getLineThrowing();
                        latestBill = gson.fromJson(latestBillString, BillMessage.class);
                        // add this bill to the Accountant
                        Accountant.getInstance().includeBillFromPeer(latestBill.getBill());
                        // ToDo: flash object -> diff()
                        // ToDo: sign flash transaction
                        sendUpdateUIBroadcastWithMessage("Billing");
                        latestBillAnswer = new BillMessageAnswer("id", true, "", Accountant.getInstance().isCloseAfterwards());
                        latestBillAnswerString = gson.toJson(latestBillAnswer);
                        socketWrapper.sendLine(latestBillAnswerString);

                        // close the channel if the last invoice has <closeAfterwards>
                        // or the last answer had <closeAfterwards>
                        if (latestBill.isCloseAfterwards() || latestBillAnswer.isCloseAfterwards()) {
                            state = State.CLOSE;
                        }
                    }
                }

                if (state == State.CLOSE) {
                    // handle the final deposit of the flash channel and sign the settlement
                    String closeChannelString = socketWrapper.getLineThrowing();
                    BillingCloseChannel billingCloseChannel = gson.fromJson(closeChannelString, BillingCloseChannel.class);
                    // ToDo: validate the remaining things
                    // ToDo: sign the final settlement
                    BillingCloseChannelAnswer billingCloseChannelAnswer = new BillingCloseChannelAnswer("");
                    String billingCloseChannelAnswerString = gson.toJson(billingCloseChannelAnswer);
                    socketWrapper.sendLine(billingCloseChannelAnswerString);

                    sendUpdateUIBroadcastWithMessage("Channel closed");

                    state = State.CLOSED;
                }


            } catch (SocketException e) {
                e.printStackTrace();
                sendUpdateUIBroadcastWithMessage("Socket exception");
                error_count++;
            } catch (UnknownHostException e) {
                sendUpdateUIBroadcastWithMessage("UnknownHostException exception");
                e.printStackTrace();
                error_count++;
            } catch (IOException e) {
                sendUpdateUIBroadcastWithMessage("IOException");
                e.printStackTrace();
                error_count++;
            } catch (Exception e) {
                e.printStackTrace();
                error_count++;
            } finally {
                if (socket != null) {
                    try {
                        socketWrapper.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // sleep in finally case 5 seconds
                try {
                    Thread.sleep(1000*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (error_count >= maxErrorCount) {
                    // stop trying to connect
                    Log.d(TAG, "start: error count too high");
                    state = State.ERROR;
                    sendUpdateUIBroadcastWithMessage("Exit");
                }

            }
        }
    }

    enum State {
        NOT_PAIRED,
        ROAMING,
        CLOSE,
        CLOSED,
        ERROR
    }
}
