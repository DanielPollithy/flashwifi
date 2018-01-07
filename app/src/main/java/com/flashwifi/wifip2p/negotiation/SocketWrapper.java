package com.flashwifi.wifip2p.negotiation;


import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SocketWrapper {
    private static final String TAG = "SocketWrapper";
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public SocketWrapper(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

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

    public void sendLine(String line) {
        out.println(line);
        Log.d(TAG, "sendLine: " + line);
    }

    public void close() throws IOException {
        socket.close();
    }

    public InetAddress getClientAddress() {
        return socket.getInetAddress();
    }
}
