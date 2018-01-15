package com.flashwifi.wifip2p.billing;

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
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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
    private State state = State.NOT_PAIRED;
    private Socket socket;
    private SocketWrapper socketWrapper;
    private static final int PORT = 9199;
    private static final int clientTimeoutMillis = 2 * 60 * 1000;

    private BillingOpenChannel billingOpenChannel;
    private BillingOpenChannelAnswer billingOpenChannelAnswer;
    private BillMessage[] billMessages;

    public BillingClient(){
        gson = new GsonBuilder().create();
    }

    public void start(String serverIPAddress) {
        while (state != State.CLOSED) {
            try {
                // create client socket that connects to server
                socket = new Socket(serverIPAddress, PORT);
                socket.setSoTimeout(clientTimeoutMillis);

                // wrap the socket
                socketWrapper = new SocketWrapper(socket);

                if (state == State.NOT_PAIRED) {
                    // send current state
                    socketWrapper.sendLine("Hello, Hotspot! I am in state: NOT_PAIRED");
                    // receive the current state of the server
                    // and whether the flash channel is established
                    String hotspotStateLine = socketWrapper.getLineThrowing();
                    if (hotspotStateLine.contains("INITIAL") || hotspotStateLine.contains("NOT_PAIRED")) {
                        // ask the hotspot to open the flash channel
                        String[] digests = new String[]{"1234", "2345", "3456"};
                        billingOpenChannel = new BillingOpenChannel(100, 100, "clientAddress", 8, digests, 20 * 60 * 1000);
                        String billingOpenChannelString = gson.toJson(billingOpenChannel);
                        socketWrapper.sendLine(billingOpenChannelString);
                        // receive the hotspot details for the flash channel
                        String billingOpenChannelAnswerString = socketWrapper.getLineThrowing();
                        billingOpenChannelAnswer = gson.fromJson(billingOpenChannelAnswerString, BillingOpenChannelAnswer.class);
                        // now create the flash channel on our side
                        Accountant.getInstance().start(billingOpenChannel.getTotalMegabytes(), billingOpenChannel.getTimeoutMinutesClient());
                        state = State.ROAMING;
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
                        // ToDo: check if this bill is okay
                        // ToDo: add this bill to the Accountant
                        // ToDo: rewrite the account to manage client and server bills
                        // ToDo: mark bill accepted
                        // ToDo: flash object -> diff()
                        // ToDo: sign flash transaction
                        latestBillAnswer = new BillMessageAnswer("id", true, "", false);
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

                    state = State.CLOSED;
                }


            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    enum State {
        NOT_PAIRED,
        ROAMING,
        CLOSE,
        CLOSED
    }
}
