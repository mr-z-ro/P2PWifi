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

class WifiDirectServerAsyncTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = WifiDirectServerAsyncTask.class.getCanonicalName();
    int len;
    byte buf[]  = new byte[1024];

    private MainActivity mainActivity;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private boolean errorOccurred;

    public WifiDirectServerAsyncTask(MainActivity mainActivity, WifiP2pManager manager,
                                     WifiP2pManager.Channel channel) {
        this.mainActivity = mainActivity;
        this.manager = manager;
        this.channel = channel;
        this.errorOccurred = false;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainActivity.logEvent(TAG, "Starting server on " + WifiDirectUtilities.PORT);
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            // Create a server socket and wait (thread is blocked) until client connection.
            Log.d(TAG, "Starting server on " + WifiDirectUtilities.PORT);
            ServerSocket serverSocket = new ServerSocket(WifiDirectUtilities.PORT);
            Socket client = serverSocket.accept();
            Log.d(TAG, "Accepted client! " + client.toString());
            mainActivity.mRecipientAddress = client.getRemoteSocketAddress().toString().split(":")[0].replace("/",""); // Of the format "/192.168.0.0:12345"

            // If this code is reached, a client has connected and transferred data
            InputStream inputStream = client.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Read the data
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            String result = outputStream.toString("UTF-8");
            outputStream.close();
            inputStream.close();
            serverSocket.close();

            Log.d(TAG, "Received message! " + result);
            return result;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            this.errorOccurred = true;
            return e.getMessage();
        }
    }

    /**
     * Start activity that can handle the JPEG image
     */
    @Override
    protected void onPostExecute(String result) {
        if (!this.errorOccurred) {
            // Display text and restart the server
            mainActivity.logEvent(TAG, "Message received: " + result + "\nRestarting server...");
            if (mainActivity.mIsGO) {
                String response = WifiDirectUtilities.getAutomatedResponse(result);
                if (response != null) {
                    new WifiDirectClientAsyncTask(mainActivity, response).execute();
                }
            }
            new WifiDirectServerAsyncTask(mainActivity, manager, channel).execute();
        } else {
            mainActivity.logEvent(TAG, result);
            this.errorOccurred = false; // in case of reuse
        }
    }
}