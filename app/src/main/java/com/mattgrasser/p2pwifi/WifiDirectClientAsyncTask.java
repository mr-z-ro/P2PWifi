package com.mattgrasser.p2pwifi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

class WifiDirectClientAsyncTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = WifiDirectClientAsyncTask.class.getCanonicalName();

    String host;
    int len;
    String msg;
    Socket socket;
    byte buf[];
    boolean errorOccurred;

    private MainActivity mainActivity;

    public WifiDirectClientAsyncTask(MainActivity mainActivity, String msg) {
        this.mainActivity = mainActivity;
        this.host = mainActivity.mRecipientAddress;
        this.msg = msg;
        this.socket = new Socket();
        this.buf = new byte[1024];
        this.errorOccurred = false;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mainActivity.logEvent(TAG, "Connecting to " + host + ":" + WifiDirectUtilities.PORT);
    }

    @Override
    protected String doInBackground(Void... voids) {
        // for debug worker thread
        //if (android.os.Debug.isDebuggerConnected())
        //    android.os.Debug.waitForDebugger();

        Log.d(TAG, "Connecting to " + host + ":" + WifiDirectUtilities.PORT);

        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, WifiDirectUtilities.PORT)), 500);
            Log.d(TAG, "Connected to " + host + ":" + WifiDirectUtilities.PORT);

            Log.d(TAG, "Sending message: " + msg);
            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data will be retrieved by the server device.
             */
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = new ByteArrayInputStream(this.msg.getBytes("UTF-8"));
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Message sent!");
            return "Successfully sent! (message: " + this.msg + ")";
        } catch (Exception e) {
            Log.e(MainActivity.TAG, e.getMessage());
            this.errorOccurred = true;
            return "Could not send because of " + e.getMessage();
        }

        /**
         * Clean up any open sockets when done
         * transferring or if an exception occurred.
         */
        finally {
            if (socket == null || !socket.isConnected()) {
                this.errorOccurred = true;
                return "Socket was either null or not connected and could not be closed";
            }

            try {
                socket.close();
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                this.errorOccurred = true;
                return "Socket couldn't be closed: " + e.getMessage();
            }
        }
    }

    /**
     * Handle the message
     */
    @Override
    protected void onPostExecute(String result) {
        if (!this.errorOccurred) {
            mainActivity.logEvent(TAG, result);
        } else {
            mainActivity.logEvent(TAG, "Error sending message: " + result);
            this.errorOccurred = false; // in case of reuse
        }
    }
}