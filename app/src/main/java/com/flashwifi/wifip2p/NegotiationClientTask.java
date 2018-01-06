package com.flashwifi.wifip2p;


import android.os.AsyncTask;
import android.util.Log;

import com.flashwifi.wifip2p.datastore.PeerStore;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class NegotiationClientTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "NegClientTask";
    
    private boolean running = true;
    private Socket socket;
    private NegotiationServerTask.State server_state;
    private NegotiationClientTask.State client_state;

    BufferedReader in;
    PrintWriter out;
    private boolean isClient = false;

    public String getLine() {
        String response;
        try {
            response = in.readLine();
        } catch (IOException ex) {
            response = "Error: " + ex;
        }
        Log.d(TAG, "getLine: " + response);
        return response;
    }

    private void sendLine(String line) {
        out.println(line);
        Log.d(TAG, "sendLine: " + line);
    }

    public void writeError(int code, String msg) {
        sendLine(String.format("err_code: %s msg: %s", Integer.toString(code), msg));
    }

    private boolean runClient(String ipAddressString, boolean isClient, String macAddress) {
        boolean success = false;

        try {
            client_state = State.INITIAL;

            // create socket
            socket = new Socket(ipAddressString, 9898);
            // set the socket timeout to 1 second
            // ToDo: make this a variable
            socket.setSoTimeout(1000);
            Log.d(TAG, "doInBackground: client socket created");

            // set up the stream reader and writer
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // send Client Request
            String hello = isClient ? "HELLO I AM CLIENT" : "HELLO I AM SERVER";
            sendLine(hello);

            if (isClient) {
                success = runClientProtocol(macAddress);
            } else {
                success = runServerProtocol(macAddress);
            }

        } catch (SocketTimeoutException ste) {
            System.out.println("### Timed out after 1 seconds");
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

    @Override
    protected String doInBackground(String... params) {

        String isClientString = params[0];
        if (isClientString.equals("True")) {
            isClient = true;
        }

        String ipAddressString = params[1];

        String macAddress = params[2];

        boolean success = false;

        while (!success){
            success = runClient(ipAddressString, isClient, macAddress);
        }

        return null;

    }

    private void closeConnection() throws IOException {
        socket.close();
    }

    private boolean runClientProtocol(String macAddress) throws IOException {

        Gson gson = new Gson();

        // WAIT FOR OFFER
        client_state = NegotiationClientTask.State.WAIT_FOR_OFFER;

        String offerString = getLine();
        if (offerString == null) {
            closeConnection();
            return false;
        }


        // CHECK OFFER
        client_state = NegotiationClientTask.State.CHECK_OFFER;

        // Get offer

        NegotiationOffer offer = gson.fromJson(offerString, NegotiationOffer.class);
        // Write offer to the PeerStore
        PeerStore.getInstance().setLatestOffer(macAddress, offer);

        // ToDo: implement accept or deny logic
        if (!true) {
            client_state = NegotiationClientTask.State.ERROR;
            writeError(1, "Offer not acceptable");
            // ToDo: Error handling
            return false;
        }

        // SEND NegotiationAnswer
        NegotiationOfferAnswer answer = new NegotiationOfferAnswer(true, 10);
        String answerString = gson.toJson(answer);
        sendLine(answerString);

        // WAIT FOR PASSWORD and hostname
        client_state = NegotiationClientTask.State.WAIT_FOR_PASSWORD;
        String finalizationString = getLine();

        if (finalizationString == null) {
            closeConnection();
            return false;
        }

        NegotiationFinalization finalization = gson.fromJson(finalizationString, NegotiationFinalization.class);
        // Write offer to the PeerStore
        PeerStore.getInstance().setLatestFinalization(macAddress, finalization);


        // Send OK
        sendLine("OK from Client");
        client_state = NegotiationClientTask.State.SUCCESS;

        socket.close();
        return true;
    }

    private boolean runServerProtocol(String macAddress) throws IOException {
        // Json serializer
        Gson gson = new GsonBuilder().create();


        // CHECK_CLIENT_REQUEST
        server_state = NegotiationServerTask.State.CHECK_CLIENT_REQUEST;
        if (!true) {
            writeError(0, "");
            server_state = NegotiationServerTask.State.ERROR;
            // ToDo: Error handling
        }

        // send offer
        NegotiationOffer offer = new NegotiationOffer(1, 100, 0);
        String offerString = gson.toJson(offer);


        sendLine(offerString);

        // WAIT_FOR_ANSWER
        server_state = NegotiationServerTask.State.WAIT_FOR_ANSWER;
        String answerString = getLine();

        // Parse the answer
        NegotiationOfferAnswer answer = gson.fromJson(answerString, NegotiationOfferAnswer.class);
        PeerStore.getInstance().setLatestOfferAnswer(macAddress, answer);

        // CHECK_ANSWER
        server_state = NegotiationServerTask.State.CHECK_ANSWER;

        if (!answer.isAgreeToConditions()) {
            writeError(0, "Client does not agree to conditions");
            server_state = NegotiationServerTask.State.ERROR;
            return false;
            // ToDo: Error handling
        }

        // CHECK_ITP
//        server_state = NegotiationServerTask.State.CHECK_ITP;
//        if (!true) {
//            writeError(0, "");
//            server_state = NegotiationServerTask.State.ERROR;
//            // ToDo: Error handling
//        }

        // GENERATE_PASSWORD
        server_state = NegotiationServerTask.State.GENERATE_PASSWORD;
        String password = "123456789";
        String hotspotName = "Iotify-123";

        // send password and hotspot name
        NegotiationFinalization finalization = new NegotiationFinalization(hotspotName, password,
                "Address", 0, 0, "");
        String finalizationString = gson.toJson(finalization);

        sendLine(finalizationString);

        // WAIT_FOR_OK
        server_state = NegotiationServerTask.State.WAIT_FOR_OK;
        String ok = getLine();

        // CREATE_HOTSPOT
        server_state = NegotiationServerTask.State.CREATE_HOTSPOT;

        // now close the socket and leave
        server_state = NegotiationServerTask.State.SUCCESS;

        socket.close();

        return true;
    }

    @Override
    protected void onPostExecute(String result) {}

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}

    public enum State {
        INITIAL,
        WAIT_FOR_OFFER,
        CHECK_OFFER,
        WAIT_FOR_PASSWORD,
        SUCCESS,
        ERROR;
    }
}
