package com.mattgrasser.p2pwifi;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class WifiDirectServerAsyncTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = WifiDirectServerAsyncTask.class.getCanonicalName();
    int len;
    byte buf[]  = new byte[1024];

    private MainActivity mainActivity;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private TextView statusText;

    public WifiDirectServerAsyncTask(MainActivity mainActivity, WifiP2pManager manager,
                                     WifiP2pManager.Channel channel, View statusText) {
        this.mainActivity = mainActivity;
        this.manager = manager;
        this.channel = channel;
        this.statusText = (TextView) statusText;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {

            Log.d(TAG, "Starting server on " + WifiDirectUtilities.PORT);
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(WifiDirectUtilities.PORT);
            Socket client = serverSocket.accept();
            Log.d(TAG, "Accepted client! " + client.toString());
            mainActivity.mRecipientAddress = client.getRemoteSocketAddress().toString().split(":")[0].replace("/",""); // Of the format "/192.168.0.0:12345"

            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            InputStream inputStream = client.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            String result = outputStream.toString("UTF-8");
            outputStream.close();
            inputStream.close();
            serverSocket.close();
            Log.d(TAG, "Received message! " + result);
            return "Message received: " + result;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Start activity that can handle the JPEG image
     */
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            // Display text and restart the server
            statusText.setText(result);
            Log.d(TAG, "Restarting server...");
            new WifiDirectServerAsyncTask(mainActivity, manager, channel, statusText).execute();
        }
    }
}