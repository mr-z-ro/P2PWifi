package com.mattgrasser.p2pwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.TextUtils;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = WifiDirectBroadcastReceiver.class.getCanonicalName();

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;
    private WifiDirectPeerListListener mPeerListListener;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity, WifiDirectPeerListListener peerListListener) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.mPeerListListener = peerListListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Broadcast intent action to indicate whether Wi-Fi p2p is enabled or disabled.
            onStateChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Broadcast intent action indicating that the available peer list has changed.
            onPeersChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Broadcast intent action indicating that the state of Wi-Fi p2p connectivity has changed.
            onConnectionChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Broadcast intent action indicating that this device details have changed.
            onThisDeviceChanged(intent);
        }
    }

    private void onStateChanged(Intent intent) {
        // Check to see if Wi-Fi is enabled and notify appropriate activity
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            mActivity.logEvent(TAG, "WIFI_P2P_STATE_ENABLED");
            mActivity.onWifiP2pStateChange(true);
        } else {
            mActivity.logEvent(TAG, "WIFI_P2P_STATE_DISABLED");
            mActivity.onWifiP2pStateChange(false);
        }
    }

    private void onPeersChanged(Intent intent) {
        // Get peers from intent extras
        WifiP2pDeviceList deviceList = (WifiP2pDeviceList) intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        int deviceCount = (deviceList == null) ? 0 : deviceList.getDeviceList().size();
        mActivity.logEvent(TAG, "WIFI_P2P_PEERS_CHANGED (" + deviceCount + " devices)");

        // If our local list matches the received list, we're already up-to-date
        if (mActivity.mGoodDevices != null
                && deviceList != null
                && mActivity.mGoodDevices.values().containsAll(deviceList.getDeviceList())
                && deviceList.getDeviceList().containsAll(mActivity.mGoodDevices.values())) {
            return;
        }

        // Lists don't match, so call peer listener directly
        mPeerListListener.onPeersAvailable(deviceList);
    }

    private void onConnectionChanged(Intent intent) {
        // Connection state changed!  We should probably do something about that
        mActivity.logEvent(TAG, "WIFI_P2P_CONNECTION_CHANGED");
        WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        if (p2pInfo != null) {
            mActivity.logEvent(TAG, "p2pInfo:\n " + TextUtils.join("\n ", p2pInfo.toString().split(" ")));
        }

        // Check the connection to make sure it's already happened
        WifiP2pGroup p2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
        if ( p2pGroup == null || p2pGroup.getOwner() == null ) {
            mActivity.logEvent(TAG, "No GO available: p2pGroup or p2pGroup.getOwner == null");
            return;
        }

        // Respond to new connection or disconnections (http://stackoverflow.com/a/24880041/604003)
        // Start the server for both sides to receive messages
        mActivity.logEvent(TAG, "Starting the server...");
        new WifiDirectServerAsyncTask(mActivity, mManager, mChannel).execute();

        // Do Group Owner (GO) and non-GO logic
        if (p2pInfo.isGroupOwner) {
            // We're the GO
            mActivity.logEvent(TAG, "WiFi Connected! (You're the GO!)");
            mActivity.mIsGO = true;

            // Get the list of peers
            Collection<WifiP2pDevice> peerList = p2pGroup.getClientList();
            ArrayList<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>(peerList);

            // Output info about peers in the group
            mActivity.logEvent(TAG, "p2pGroup list size = " + list.size());
            if ( list.size() <= 0 ) {
                mActivity.logEvent(TAG, "Nobody is connected.");
                return;
            }
            String host = null;
            for (int i = 1; i < list.size(); i++) {
                host = list.get(i).deviceAddress;
                mActivity.logEvent(TAG, "peer #" + i + " address: " + host);
            }
        } else if (p2pInfo.groupFormed) {
            // We're not the GO
            mActivity.mIsGO = false;

            // Need to store GO IP
            // NOTE: we'll have to send a message to let GO know our local address before they can respond
            InetAddress goAddress = p2pInfo.groupOwnerAddress;
            mActivity.mRecipientAddress = goAddress.getHostAddress();
            mActivity.logEvent(TAG, "WiFi Connected!\n GO MAC: " + p2pGroup.getOwner().deviceAddress + "\n GO IP: " + goAddress.getHostAddress());
        }
    }

    private void onThisDeviceChanged(Intent intent) {
        mActivity.logEvent(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED");

        // Respond to this device's wifi state changing
        int wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
        switch(wifiState) {
            case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                mActivity.logEvent(TAG, "WIFI_P2P_STATE_DISABLED");
                break;
            case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                mActivity.logEvent(TAG, "WIFI_P2P_STATE_ENABLED");
                break;
            default:
                mActivity.logEvent(TAG, "Unknown WiFi P2P state.");
        }
    }
}

