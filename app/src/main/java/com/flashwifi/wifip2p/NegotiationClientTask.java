package com.flashwifi.wifip2p;


import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class NegotiationClientTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "NegClientTask";
    
    private boolean running = true;
    private ServerSocket serverSocket;
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

    @Override
    protected String doInBackground(String... params) {
        String return_string = null;

        String isClientString = params[0];
        if (isClientString.equals("True")) {
            isClient = true;
        }

        String ipAddressString = params[1];


        try {
            client_state = State.INITIAL;

            // create socket
            socket = new Socket(ipAddressString, 9898);
            Log.d(TAG, "doInBackground: client socket created");

            // set up the stream reader and writer
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // send Client Request
            String hello = isClient ? "HELLO I AM CLIENT" : "HELLO I AM SERVER";
            sendLine(hello);

            if (isClient) {
                return_string = runClientProtocol();
            } else {
                return_string = runServerProtocol();
            }

        } catch (IOException e) {
            Log.d("", e.getMessage());
        }

        return return_string;
    }

    private void closeConnection() throws IOException {
        socket.close();
    }

    private String runClientProtocol() throws IOException {
        // WAIT FOR OFFER
        client_state = NegotiationClientTask.State.WAIT_FOR_OFFER;
        String offer = getLine();
        if (offer == null) {
            closeConnection();
            return null;
        }

        // CHECK OFFER
        client_state = NegotiationClientTask.State.CHECK_OFFER;
        if (!true) {
            client_state = NegotiationClientTask.State.ERROR;
            writeError(1, "Offer not acceptable");
            // ToDo: Error handling
            return null;
        }

        // SEND ANSWER
        sendLine("Answer from Client");

        // WAIT FOR PASSWORD
        client_state = NegotiationClientTask.State.WAIT_FOR_PASSWORD;
        String credentials = getLine();
        if (credentials == null) {
            closeConnection();
            return null;
        }

        // Send OK
        sendLine("OK from Client");
        client_state = NegotiationClientTask.State.SUCCESS;

        socket.close();
        return null;
    }

    private String runServerProtocol() throws IOException {
        // CHECK_CLIENT_REQUEST
        server_state = NegotiationServerTask.State.CHECK_CLIENT_REQUEST;
        if (!true) {
            writeError(0, "");
            server_state = NegotiationServerTask.State.ERROR;
            // ToDo: Error handling
        }

        // send offer
        sendLine("Offer from server");

        // WAIT_FOR_ANSWER
        server_state = NegotiationServerTask.State.WAIT_FOR_ANSWER;
        String answer = getLine();

        // CHECK_ANSWER
        server_state = NegotiationServerTask.State.CHECK_ANSWER;
        if (!true) {
            writeError(0, "");
            server_state = NegotiationServerTask.State.ERROR;
            // ToDo: Error handling
        }

        // CHECK_ITP
        server_state = NegotiationServerTask.State.CHECK_ITP;
        if (!true) {
            writeError(0, "");
            server_state = NegotiationServerTask.State.ERROR;
            // ToDo: Error handling
        }

        // GENERATE_PASSWORD
        server_state = NegotiationServerTask.State.GENERATE_PASSWORD;
        String password = "123456789";
        String hotspotName = "Iotify-123";

        // send password and hotspot name
        sendLine("Credentials from server: " + password + " ||| " + hotspotName);

        // WAIT_FOR_OK
        server_state = NegotiationServerTask.State.WAIT_FOR_OK;
        String ok = getLine();

        // CREATE_HOTSPOT
        server_state = NegotiationServerTask.State.CREATE_HOTSPOT;

        // now close the socket and leave
        server_state = NegotiationServerTask.State.SUCCESS;

        socket.close();

        return null;
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
