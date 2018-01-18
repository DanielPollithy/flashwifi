package com.flashwifi.wifip2p.billing;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import com.flashwifi.wifip2p.negotiation.SocketWrapper;
import com.flashwifi.wifip2p.protocol.BillMessage;
import com.flashwifi.wifip2p.protocol.BillMessageAnswer;
import com.flashwifi.wifip2p.protocol.BillingCloseChannel;
import com.flashwifi.wifip2p.protocol.BillingCloseChannelAnswer;
import com.flashwifi.wifip2p.protocol.BillingOpenChannel;
import com.flashwifi.wifip2p.protocol.BillingOpenChannelAnswer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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
    private State state = State.NOT_PAIRED;
    private ServerSocket serverSocket;
    private Socket socket;
    private SocketWrapper socketWrapper;
    private int PORT = 9199;
    private static final int serverTimeoutMillis = 2 * 60 * 1000;

    Context context;

    private void sendUpdateUIBroadcastWithMessage(String message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.setAction("com.flashwifi.wifip2p.update_roaming");
        context.sendBroadcast(local);
    }

    public BillingServer(int bookedMegabytes, int timeoutMinutes, int maxMinutes, int iotaDepositClient, Context context){
        this.context = context;
        Accountant.getInstance().start(bookedMegabytes, timeoutMinutes, maxMinutes, iotaDepositClient);
        gson = new GsonBuilder().create();
    }

    public void start() throws IOException {
        // 0) create deadline guard
        createDeadlineGuard();
        // 1) create a socket

        Log.d(TAG, "start: Billing server has been started");

        // ToDo: receive end of roaming broadcast

        while (state != State.CLOSED) {
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

                    // answer with billingOpenChannelAnswerString
                    // ToDo: create the flash channel
                    String[] myDigests = new String[]{"1234", "2345", "3456"};
                    BillingOpenChannelAnswer billingOpenChannelAnswer = new BillingOpenChannelAnswer(0, 0, "", "", myDigests);
                    String billingOpenChannelAnswerString = gson.toJson(billingOpenChannelAnswer);
                    socketWrapper.sendLine(billingOpenChannelAnswerString);

                    sendUpdateUIBroadcastWithMessage("Channel established");

                    // OK
                    state = State.ROAMING;
                }

                if (state == State.ROAMING) {
                    String latestBillString;
                    BillMessage latestBill;
                    Bill b;
                    String latestBillAnswerString;
                    BillMessageAnswer latestBillAnswer;

                    // loop until roaming ends
                    int count = 0;

                    while (state == State.ROAMING) {
                        // sleep 1 minute
                        Log.d(TAG, "start: I go to sleep for 1 minute!");
                        Thread.sleep(60 * 1000);
                        Log.d(TAG, "start: Good morning!");
                        // create new bill
                        // ToDo: integrate real network data
                        b = Accountant.getInstance().createBill(3,9);
                        // ToDo: integrate real flash channel
                        latestBill = new BillMessage(b, "<flash obj>", Accountant.getInstance().isCloseAfterwards());
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
                    }
                    state = State.FULLY_ATTACHED;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
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
