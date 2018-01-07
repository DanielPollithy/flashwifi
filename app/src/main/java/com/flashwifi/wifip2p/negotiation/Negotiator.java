package com.flashwifi.wifip2p.negotiation;


import android.util.Log;

import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Negotiator {
    private static final String TAG = "Negotiator";
    private static final int PORT = 9898;
    private static final int clientTimeoutMillis = 100000;
    private static final int serverTimeoutMillis = 100000;
    private final String ownMacAddress;

    private SocketWrapper socketWrapper;

    private HotspotState hotspot_state;
    private ConsumerState consumer_state;

    // client as in client-server
    private boolean isClient;
    // consumer as in consumer-hotspot
    private boolean isConsumer;

    private Gson gson;

    public enum ConsumerState {
        INITIAL,
        WAIT_FOR_OFFER,
        CHECK_OFFER,
        WAIT_FOR_PASSWORD,
        SUCCESS,
        ERROR
    }

    public enum HotspotState {
        INITIAL,
        WAIT_FOR_CLIENT,
        CHECK_CLIENT_REQUEST,
        WAIT_FOR_ANSWER,
        CHECK_ANSWER,
        CHECK_ITP,
        GENERATE_PASSWORD,
        WAIT_FOR_OK,
        CREATE_HOTSPOT,
        SUCCESS,
        ERROR
    }

    public Negotiator(boolean isConsumer, String ownMacAddress) {
        this.isConsumer = isConsumer;
        gson = new GsonBuilder().create();
        this.ownMacAddress = ownMacAddress;
        Log.d(TAG, "Negotiator: " + ownMacAddress);
    }

    public boolean workAsClient(String serverIPAddress) {
        this.isClient = true;

        boolean success = false;
        Socket socket = null;

        try {
            // create client socket that connects to server
            socket = new Socket(serverIPAddress, PORT);
            socket.setSoTimeout(clientTimeoutMillis);
            Log.d(TAG, "workAsClient: client socket created");

            // wrap the socket
            socketWrapper = new SocketWrapper(socket);

            // send Client Request
            String hello = isClient ? "HELLO I AM CLIENT" : "HELLO I AM SERVER";
            socketWrapper.sendLine(hello);

            // Whether we want to provide a hotspot or use one
            if (isConsumer) {
                success = runConsumerProtocol(serverIPAddress);
            } else {
                success = runHotspotProtocol(serverIPAddress);
            }
        } catch (SocketTimeoutException ste) {
            Log.d(TAG, "workAsServer: ### Timed out after 1 seconds");
        } catch (IOException e) {
            Log.d("", e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    public boolean workAsServer() {
        // this device is the socket server
        this.isClient = false;

        boolean success = false;
        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            // use the port to start
            serverSocket = new ServerSocket(PORT);
            //serverSocket.setSoTimeout(serverTimeoutMillis);
            Log.d(TAG, "doInBackground: Server is waiting for connection");

            // accept one connection
            socket = serverSocket.accept();
            //socket.setSoTimeout(serverTimeoutMillis);

            // wrap the socket
            socketWrapper = new SocketWrapper(socket);

            // WAIT FOR CLIENT
            String hello = socketWrapper.getLine();

            if (hello == null) {
                error(0, "no hello received");
                return false;
            }

            // Check: Is the peer in the same role as we are
            // server and server or client and client MAKES NO SENSE
            if (hello.contains("SERVER") && !isClient || hello.contains("CLIENT") && isClient){
                error(1, "Pairing roles are broken");
                return false;
            }

            // Whether we want to provide a hotspot or use one
            if (isConsumer) {
                success = runConsumerProtocol(socketWrapper.getClientAddress().getHostAddress());
            } else {
                success = runHotspotProtocol(socketWrapper.getClientAddress().getHostAddress());
            }
        } catch (SocketTimeoutException ste) {
            Log.d(TAG, "workAsServer: ### Timed out after 1 seconds");
        } catch (IOException e) {
            Log.d("", e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return success;
    }

    private boolean runConsumerProtocol(String ipAddress) throws IOException {
        consumer_state = ConsumerState.WAIT_FOR_OFFER;

        // RECEIVE OFFER
        String offerString = socketWrapper.getLine();
        if (offerString == null) {
            error(2, "No offer received");
            return false;
        }

        // CHECK OFFER
        consumer_state = ConsumerState.CHECK_OFFER;
        NegotiationOffer offer = gson.fromJson(offerString, NegotiationOffer.class);
        String otherMac = offer.getHotspotMac();
        // Write offer to the PeerStore
        PeerStore.getInstance().setLatestOffer(otherMac, offer);

        // ToDo: implement accept or deny logic
        if (!true) {
            error(3, "Offer not acceptable");
            return false;
        }

        // SEND NegotiationAnswer
        // ToDo: where shall the input come from?
        NegotiationOfferAnswer answer = new NegotiationOfferAnswer(true, 10, ownMacAddress);
        String answerString = gson.toJson(answer);
        socketWrapper.sendLine(answerString);

        // WAIT FOR PASSWORD and hostname
        consumer_state = ConsumerState.WAIT_FOR_PASSWORD;
        String finalizationString = socketWrapper.getLine();

        if (finalizationString == null) {
            error(4, "No finalization received");
            return false;
        }

        NegotiationFinalization finalization = gson.fromJson(finalizationString, NegotiationFinalization.class);
        // Write offer to the PeerStore
        PeerStore.getInstance().setLatestFinalization(otherMac, finalization);


        // Send OK
        socketWrapper.sendLine("OK from Client");
        consumer_state = ConsumerState.SUCCESS;

        // End
        socketWrapper.close();

        return true;
    }

    private boolean runHotspotProtocol(String ipAddress) throws IOException {
        // CHECK_CLIENT_REQUEST
        hotspot_state = HotspotState.CHECK_CLIENT_REQUEST;

        // send offer
        int iotaPerMegabyte = (int) (Math.random() * (1000 - 10)) + 10;
        NegotiationOffer offer = new NegotiationOffer(1, 100, iotaPerMegabyte, ownMacAddress);
        String offerString = gson.toJson(offer);
        socketWrapper.sendLine(offerString);

        // WAIT_FOR_ANSWER
        hotspot_state = HotspotState.WAIT_FOR_ANSWER;
        String answerString = socketWrapper.getLine();

        if (answerString == null) {
            error(8, "No answer received");
            return false;
        }

        // Parse the answer
        NegotiationOfferAnswer answer = gson.fromJson(answerString, NegotiationOfferAnswer.class);
        String otherMac = answer.getConsumerMac();
        PeerStore.getInstance().setLatestOfferAnswer(otherMac, answer);

        // CHECK_ANSWER
        hotspot_state = HotspotState.CHECK_ANSWER;

        if (!answer.isAgreeToConditions()) {
            error(5, "Client does not agree to conditions");
            return false;
        }

        // CHECK_ITP
//        server_state = NegotiationServerTask.State.CHECK_ITP;
//        if (!true) {
//            writeError(0, "");
//            server_state = NegotiationServerTask.State.ERROR;
//            // ToDo: Error handling
//        }

        // GENERATE_PASSWORD
        // ToDo: Don't mock this
        hotspot_state = HotspotState.GENERATE_PASSWORD;
        String password = "123456789";
        String hotspotName = "Iotify-123";

        // send password and hotspot name
        NegotiationFinalization finalization = new NegotiationFinalization(hotspotName, password,
                "Address", 0, 0, "");
        String finalizationString = gson.toJson(finalization);

        socketWrapper.sendLine(finalizationString);

        // WAIT_FOR_OK
        hotspot_state = HotspotState.WAIT_FOR_OK;
        String ok = socketWrapper.getLine();

        // CREATE_HOTSPOT
        hotspot_state = HotspotState.CREATE_HOTSPOT;

        // now close the socket and leave
        hotspot_state = HotspotState.SUCCESS;

        socketWrapper.close();

        return true;
    }

    // ------------------------
    //   HELPER METHODS BELOW
    // ------------------------

    private void error(int code, String msg) throws IOException {
        Log.d(TAG, "error: " + msg);
        if (isConsumer) {
            consumer_state = ConsumerState.ERROR;
        } else {
            hotspot_state = HotspotState.ERROR;
        }
        socketWrapper.close();
    }
}
