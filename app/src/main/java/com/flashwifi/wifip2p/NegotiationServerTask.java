package com.flashwifi.wifip2p;


import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class NegotiationServerTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "NegServerTask";
    
    private boolean running = true;
    private ServerSocket serverSocket;
    private Socket socket;
    private State state;
    private NegotiationClientTask.State client_state;

    private boolean isClient = false;

    BufferedReader in;
    PrintWriter out;

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

        try {
            serverSocket = new ServerSocket(9898);
            state = State.INITIAL;
            Log.d(TAG, "doInBackground: Server is waiting for connection");
            socket = serverSocket.accept();

            // set up the stream reader and writer
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // WAIT FOR CLIENT
            state = State.WAIT_FOR_CLIENT;
            String hello = getLine();

            // Check: Is the peer in the same role as we are?
            // server and server or client and client MAKES NO SENSE
            // we have to check this because we can't control it
            if (hello.contains("SERVER") && !isClient || hello.contains("CLIENT") && isClient) {
                Log.d(TAG, "doInBackground: Pairing roles are broken.");
                state = State.ERROR;
                socket.close();
                return null;
            }

            // If we are the server
            if (!isClient) {
                return_string = runServerProtocol();
            } else {
                // we are the client
                return_string = runClientProtocol();
            }
        } catch (IOException e) {
            Log.d("", e.getMessage());
        }

        return return_string;
    }

    private String runClientProtocol() throws IOException {
        // WAIT FOR OFFER
        client_state = NegotiationClientTask.State.WAIT_FOR_OFFER;
        String offer = getLine();

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

        // Send OK
        sendLine("OK from Client");
        client_state = NegotiationClientTask.State.SUCCESS;

        socket.close();

        return null;
    }

    private String runServerProtocol() throws IOException {
        // CHECK_CLIENT_REQUEST
        state = State.CHECK_CLIENT_REQUEST;
        if (!true) {
            writeError(0, "");
            state = State.ERROR;
            // ToDo: Error handling
        }

        // send offer
        sendLine("Offer from server");

        // WAIT_FOR_ANSWER
        state = State.WAIT_FOR_ANSWER;
        String answer = getLine();

        // CHECK_ANSWER
        state = State.CHECK_ANSWER;
        if (!true) {
            writeError(0, "");
            state = State.ERROR;
            // ToDo: Error handling
        }

        // CHECK_ITP
        state = State.CHECK_ITP;
        if (!true) {
            writeError(0, "");
            state = State.ERROR;
            // ToDo: Error handling
        }

        // GENERATE_PASSWORD
        state = State.GENERATE_PASSWORD;
        String password = "123456789";
        String hotspotName = "Iotify-123";

        // send password and hotspot name
        sendLine("Credentials from server: " + password + " ||| " + hotspotName);

        // WAIT_FOR_OK
        state = State.WAIT_FOR_OK;
        String ok = getLine();

        // CREATE_HOTSPOT
        state = State.CREATE_HOTSPOT;

        // now close the socket and leave
        state = State.SUCCESS;

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
        WAIT_FOR_CLIENT,
        CHECK_CLIENT_REQUEST,
        WAIT_FOR_ANSWER,
        CHECK_ANSWER,
        CHECK_ITP,
        GENERATE_PASSWORD,
        WAIT_FOR_OK,
        CREATE_HOTSPOT,
        SUCCESS,
        ERROR;
    }
}
