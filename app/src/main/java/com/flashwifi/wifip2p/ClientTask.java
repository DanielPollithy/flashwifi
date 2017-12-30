package com.flashwifi.wifip2p;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientTask extends AsyncTask<String, Void, String> {

    private final static String TAG = "ClientTask";

    @Override
    protected String doInBackground(String... params) {
        String host = params[0];
        String message = params[1];
        int port = 9999;
        Socket socket = new Socket();
        Log.d(TAG, "connectToSocketServer: socket created");
        byte buf[]  = new byte[1024];
        try {
            socket.bind(null);
            Log.d(TAG, "connectToSocketServer: socket bind");
            socket.connect((new InetSocketAddress(host, port)), 500);
            Log.d(TAG, "connectToSocketServer: socket connected");
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
            DOS.writeUTF(message);
            Log.d(TAG, "connectToSocketServer: utf written");
            socket.close();
        } catch (IOException e) {
            //catch logic
            Log.d(TAG, "connectToSocketServer: io exception");
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {}

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}
}