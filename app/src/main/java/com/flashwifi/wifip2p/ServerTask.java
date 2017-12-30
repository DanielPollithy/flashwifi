package com.flashwifi.wifip2p;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTask extends AsyncTask<String, Void, String> {

    private boolean running = true;

    @Override
    protected String doInBackground(String... params) {
        try {

            String msg_received = null;

            while (running) {
                ServerSocket serverSocket = new ServerSocket(9999);
                Socket client = serverSocket.accept();
                Log.d("ServerTask", "doInBackground: openend");
                DataInputStream DIS = new DataInputStream(client.getInputStream());
                msg_received = DIS.readUTF();
                Log.d(">>>>>", msg_received);
                serverSocket.close();
            }

            return msg_received;
        } catch (IOException e) {
            Log.d("", e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {}

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}
}