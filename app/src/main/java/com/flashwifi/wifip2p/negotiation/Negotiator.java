package com.flashwifi.wifip2p.negotiation;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.flashwifi.wifip2p.R;
import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Negotiator {
    private static final String TAG = "Negotiator";
    private static final int PORT = 9898;
    private static final int clientTimeoutMillis = 5000;
    private static final int serverTimeoutMillis = 5000;
    private final String peerMac;
    private String ownMacAddress;

    private SocketWrapper socketWrapper;

    private HotspotState hotspot_state;
    private ConsumerState consumer_state;

    // client as in client-server
    private boolean isClient;
    // consumer as in consumer-hotspot
    private boolean isConsumer;

    private String mac = null;
    private String peer_mac_address = null;

    private Gson gson;
    private SharedPreferences prefs;
    private Context context;

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

    private void sendUpdateUIBroadcastWithMessage(String message, String snd_message){
        Intent local = new Intent();
        local.putExtra("message", message);
        local.putExtra("snd_message", snd_message);
        local.setAction("com.flashwifi.wifip2p.update_ui");
        context.sendBroadcast(local);
    }

    public Negotiator(boolean isConsumer, String ownMacAddress, SharedPreferences prefs, Context context,
                      String peerMac) {
        this.peerMac = peerMac;
        this.isConsumer = isConsumer;
        gson = new GsonBuilder().create();
        this.ownMacAddress = ownMacAddress;
        this.prefs = prefs;
        this.context = context;
        Log.d(TAG, "Negotiator: " + ownMacAddress);
    }

    public NegotiationReturn workAsClient(String serverIPAddress) {
        this.isClient = true;
        Socket socket = null;
        NegotiationReturn negReturn;
        int code = 0;
        boolean restartAfterwards = false;


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
                Log.d(TAG, "workAsClient: runConsumerProtocol");
                negReturn = runConsumerProtocol(serverIPAddress);
            } else {
                Log.d(TAG, "workAsClient: runHotspotProtocol");
                negReturn = runHotspotProtocol(serverIPAddress);
            }

            mac = negReturn.mac;
            restartAfterwards = negReturn.restartAfterwards;
            code = negReturn.code;
        } catch (SocketTimeoutException ste) {
            mac = null;
            restartAfterwards = true;
            code = R.string.err_timeout;
            Log.d(TAG, "workAsServer: ### Timed out");
        } catch (IOException e) {
            mac = null;
            restartAfterwards = true;
            code = R.string.err_io_exception;
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
        return new NegotiationReturn(code, mac, restartAfterwards);
    }

    public NegotiationReturn workAsServer() {
        // this device is the socket server
        this.isClient = false;

        peer_mac_address = null;
        ServerSocket serverSocket = null;
        Socket socket = null;
        int code = 0;
        NegotiationReturn negReturn;
        boolean restartAfterwards = false;

        try {
            // use the port to start
            serverSocket = new ServerSocket(PORT);
            serverSocket.setSoTimeout(serverTimeoutMillis);
            Log.d(TAG, "doInBackground: Server is waiting for connection");

            // accept one connection
            socket = serverSocket.accept();
            socket.setSoTimeout(serverTimeoutMillis);

            // wrap the socket
            socketWrapper = new SocketWrapper(socket);

            // WAIT FOR CLIENT
            String hello = socketWrapper.getLine();

            if (hello == null || hello.contains("java.net.SocketException")) {
                return error(R.string.no_hello_received, true);
            }

            // Check: Is the peer in the same role as we are
            // server and server or client and client MAKES NO SENSE
            if (hello.contains("SERVER") && !isClient || hello.contains("CLIENT") && isClient){
                return error(R.string.err_pairing_roles_broken, true);
            }

            // Whether we want to provide a hotspot or use one
            if (isConsumer) {
                negReturn = runConsumerProtocol(socketWrapper.getClientAddress().getHostAddress());
            } else {
                negReturn = runHotspotProtocol(socketWrapper.getClientAddress().getHostAddress());
            }
            peer_mac_address = negReturn.mac;
            code = negReturn.code;
            restartAfterwards = negReturn.restartAfterwards;

        } catch (SocketTimeoutException ste) {
            Log.d(TAG, "workAsServer: ### Timed out");
            code = R.string.err_timeout;
            peer_mac_address = null;
            restartAfterwards = true;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            code = R.string.err_io_exception;
            peer_mac_address = null;
            restartAfterwards = true;
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
        return new NegotiationReturn(code, peer_mac_address, restartAfterwards);
    }

    private NegotiationReturn runConsumerProtocol(String ipAddress) throws IOException {
        consumer_state = ConsumerState.WAIT_FOR_OFFER;

        // RECEIVE OFFER
        Log.d(TAG, "runConsumerProtocol: wait for getline");
        String offerString = socketWrapper.getLine();
        if (offerString == null || offerString.contains("java.net.SocketException")) {
            return error(R.string.err_no_offer_received, true);
        }

        // CHECK OFFER
        consumer_state = ConsumerState.CHECK_OFFER;
        NegotiationOffer offer = gson.fromJson(offerString, NegotiationOffer.class);
        String otherMac = offer.getHotspotMac();
        if (otherMac == null) {
            otherMac = this.peerMac;
        }
        // Write offer to the PeerStore
        PeerStore.getInstance().setLatestOffer(otherMac, offer);

        // accept or deny logic
        boolean agree = true;
        int disagree_reason = 0;
        int iotaPerMegabyte = Integer.valueOf(prefs.getString("edit_text_buy_price", "-1"));
        if (iotaPerMegabyte < 0) {
            return error(R.string.err_buy_price_bad_setting, false);
        }
        if (offer.getIotaPerMegabyte() > iotaPerMegabyte) {
            agree = false;
            disagree_reason = R.string.err_price_too_high;
        }

        int hotspot_max_minutes = offer.getMaxMinutes();
        int hotspot_min_minutes = offer.getMinMinutes();
        int client_roaming_minutes = Integer.valueOf(prefs.getString("edit_text_client_minutes", "-1"));

        if (client_roaming_minutes < 0) {
            return error(R.string.err_client_minutes_bad_setting, false);
        }

        if (client_roaming_minutes < hotspot_min_minutes || client_roaming_minutes > hotspot_max_minutes) {
            agree = false;
            disagree_reason = R.string.err_client_minutes_not_acceptable_for_hotspot;
        }

        // check consumer has enough iotas
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String defaultValue = "0";
        String storedBalance = sharedPref.getString("balance", defaultValue);
        int iotaBalance = Integer.parseInt(storedBalance);

        Log.d(TAG, "runConsumerProtocol: MY IOTA BALANCE: " + iotaBalance);

        // Assumption 1 MB per minute
        if (iotaBalance < client_roaming_minutes * iotaPerMegabyte) {
            Log.d(TAG, "runConsumerProtocol: Balance too low");
            agree = false;
            disagree_reason = R.string.err_client_not_enough_iota;
        }

        // SEND NegotiationAnswer
        NegotiationOfferAnswer answer = new NegotiationOfferAnswer(agree, client_roaming_minutes, ownMacAddress);
        PeerStore.getInstance().setLatestOfferAnswer(otherMac, answer);
        String answerString = gson.toJson(answer);
        socketWrapper.sendLine(answerString);

        if (!agree) {
            return error(disagree_reason, false);
        }

        // WAIT FOR PASSWORD and hostname
        consumer_state = ConsumerState.WAIT_FOR_PASSWORD;
        String finalizationString = socketWrapper.getLine();

        if (finalizationString == null || finalizationString.contains("java.net.SocketException")) {
            return error(R.string.err_no_finalization_received, true);
        } else if (answerString.equals("BYE")) {
            return error(R.string.err_peer_quit, false);
        }

        NegotiationFinalization finalization = gson.fromJson(finalizationString, NegotiationFinalization.class);
        // Write offer to the PeerStore
        PeerStore.getInstance().setLatestFinalization(otherMac, finalization);


        // Send OK
        socketWrapper.sendLine("OK from Client");
        consumer_state = ConsumerState.SUCCESS;

        // End
        socketWrapper.close();

        return new NegotiationReturn(0, otherMac, false);
    }

    private NegotiationReturn runHotspotProtocol(String ipAddress) throws IOException {
        // CHECK_CLIENT_REQUEST
        hotspot_state = HotspotState.CHECK_CLIENT_REQUEST;

        // send offer
        int iotaPerMegabyte = Integer.valueOf(prefs.getString("edit_text_sell_price", "-1"));
        if (iotaPerMegabyte < 0) {
            return error(R.string.err_sell_price_bad_setting, false);
        }
        int minMinutes = Integer.valueOf(prefs.getString("edit_text_sell_min_minutes", "-1"));
        if (minMinutes < 0) {
            return error(R.string.err_min_minutes_bad_setting, false);
        }
        int maxMinutes = Integer.valueOf(prefs.getString("edit_text_sell_max_minutes", "-1"));
        if (maxMinutes < 0) {
            return error(R.string.err_max_minutes_bad_setting, false);
        }

        // check consumer has enough iotas
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String defaultValue = "0";
        String storedBalance = sharedPref.getString("balance", defaultValue);
        int iotaBalance = Integer.parseInt(storedBalance);

        // Assumption 1 MB per minute
        if (iotaBalance < maxMinutes * iotaPerMegabyte) {
            Log.d(TAG, "runHotspotProtocol: Balance too low");
            return error(R.string.err_hotspot_not_enough_iota, false);
        }

        NegotiationOffer offer = new NegotiationOffer(minMinutes, maxMinutes, iotaPerMegabyte, ownMacAddress);

        String offerString = gson.toJson(offer);
        socketWrapper.sendLine(offerString);

        // WAIT_FOR_ANSWER
        hotspot_state = HotspotState.WAIT_FOR_ANSWER;
        String answerString = socketWrapper.getLine();

        if (answerString == null || answerString.contains("java.net.SocketException")) {
            return error(R.string.err_no_answer_received, true);
        } else if (answerString.equals("BYE")) {
            return error(R.string.err_peer_quit, false);
        }

        // Parse the answer
        NegotiationOfferAnswer answer = gson.fromJson(answerString, NegotiationOfferAnswer.class);
        String otherMac = answer.getConsumerMac();
        if (otherMac == null) {
            Log.d(TAG, "runHotspotProtocol: using fallback MAC " + this.peerMac);
            otherMac = this.peerMac;
        }
        peer_mac_address = otherMac;
        PeerStore.getInstance().setLatestOffer(otherMac, offer);
        PeerStore.getInstance().setLatestOfferAnswer(otherMac, answer);

        // CHECK_ANSWER
        hotspot_state = HotspotState.CHECK_ANSWER;

        if (!answer.isAgreeToConditions()) {
            return error(R.string.err_client_does_not_agree, false);
        }

        if (answer.getDuranceInMinutes() > maxMinutes || answer.getDuranceInMinutes() < minMinutes) {
            return error(R.string.err_client_minutes_out_of_bounds, false);
        }

        // CHECK_ITP
//        server_state = NegotiationServerTask.State.CHECK_ITP;
//        if (!true) {
//            writeError(0, "");
//            server_state = NegotiationServerTask.State.ERROR;
//        }

        // GENERATE_PASSWORD and hotspot name
        hotspot_state = HotspotState.GENERATE_PASSWORD;
        int min = 100000000;
        int max = 999999999;
        String password = Integer.toString(ThreadLocalRandom.current().nextInt(min, max + 1));
        String hotspotName = "Iotify-"+Integer.toString(ThreadLocalRandom.current().nextInt(100, 10000));


        // ToDo: get depositAddressFlashChannel from SharedPreferences
        // ToDo: get the bandwidth of the hotspot
        // ToDo: create initial flash object
        double bandwidth = 0.1; // megabyte per second;
        int max_data_volume_megabytes = (int) (answer.getDuranceInMinutes() * bandwidth);
        int max_iota_transferred = max_data_volume_megabytes * offer.getIotaPerMegabyte();
        max_iota_transferred = 10;
        String rootAddress = "JZWUMRUEYFJOCDDRZCNIIMDZSX9LWMITNMDIAIUJKUV9LVDLSICDABFYTTBZFGEBJOADDN9WZ9IJJJD9DXRJRR9TOW";
        // send the most important message to the user
        NegotiationFinalization finalization = new NegotiationFinalization(hotspotName, password,
                rootAddress, max_iota_transferred, max_iota_transferred, "<flashObj>");
        PeerStore.getInstance().setLatestFinalization(otherMac, finalization);
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

        return new NegotiationReturn(0, otherMac, false);
    }

    // ------------------------
    //   HELPER METHODS BELOW
    // ------------------------

    private NegotiationReturn error(int err_no, boolean restartAfterwards) throws IOException {
        String str = context.getString(err_no);
        Log.d(TAG, "error: " + str);
        if (isConsumer) {
            consumer_state = ConsumerState.ERROR;
        } else {
            hotspot_state = HotspotState.ERROR;
        }
        String macAdd = (restartAfterwards) ? null : this.peer_mac_address;
        sendUpdateUIBroadcastWithMessage("error", str);
        if (this.peer_mac_address != null) {
            PeerStore.getInstance().setErrorMessage(this.peer_mac_address, str);
        }
        if (!restartAfterwards) {
            try {
                socketWrapper.sendLine("BYE");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        socketWrapper.close();
        return new NegotiationReturn(err_no, macAdd, restartAfterwards);
    }

    public class NegotiationReturn {
        public int code;
        public String mac;
        public boolean restartAfterwards;

        public NegotiationReturn(int code, String mac, boolean restartAfterwards) {
            this.mac = mac;
            this.code = code;
            this.restartAfterwards = restartAfterwards;
        }
    }
}
