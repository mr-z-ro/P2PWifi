package com.mattgrasser.p2pwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
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
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "WIFI_P2P_STATE_ENABLED");
                mActivity.mStatusText.append("WIFI_P2P_STATE_ENABLED\n");
                mActivity.onWifiP2pStateChange(true);
            } else {
                Log.d(TAG, "WIFI_P2P_STATE_DISABLED");
                mActivity.mStatusText.append("WIFI_P2P_STATE_DISABLED\n");
                mActivity.onWifiP2pStateChange(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed!  We should probably do something about that
            Log.d(TAG, "WIFI_P2P_PEERS_CHANGED");
            mActivity.mStatusText.append("WIFI_P2P_PEERS_CHANGED\n");

            // Get peers from intent extras and call peer listener directly
            WifiP2pDeviceList deviceList = (WifiP2pDeviceList) intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
            if (deviceList != null) {
                if (mActivity.mGoodDevices == null ||
                        (!mActivity.mGoodDevices.values().containsAll(deviceList.getDeviceList()) &&
                                !deviceList.getDeviceList().containsAll(mActivity.mGoodDevices.values()))) {
                    mPeerListListener.onPeersAvailable(deviceList);
                }
            }

            // Optionally call WifiP2pManager.requestPeers() to get a list of current peers
            //if (mManager != null) {
            //    mManager.requestPeers(mChannel, mPeerListListener);
            //}
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed!  We should probably do something about that
            Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED");
            mActivity.mStatusText.append("WIFI_P2P_CONNECTION_CHANGED\n");
            WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            if (p2pInfo != null) {
                Log.d(TAG, "p2pInfo: " + p2pInfo.toString());
                mActivity.mStatusText.append("p2pInfo: " + p2pInfo.toString() + "\n");
            }

            // Respond to new connection or disconnections (http://stackoverflow.com/a/24880041/604003)
            WifiP2pGroup p2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            if ( p2pGroup != null ) {
                if ( p2pGroup.getOwner() != null ) {
                    // Start the server for both sides to receive messages
                    Log.d(TAG, "Starting the server...");
                    mActivity.mStatusText.append("Starting the server..." + "\n");
                    new WifiDirectServerAsyncTask(mActivity, mManager, mChannel, mActivity.mStatusText).execute();

                    if (p2pInfo.isGroupOwner) {
                        // We're the GO, start the server
                        Log.d(TAG, "WiFi Connected! (You're the GO!)");
                        mActivity.mStatusText.append("WiFi Connected! (You're the GO!)\n");
                    } else if (p2pInfo.groupFormed) {
                        InetAddress goAddress = p2pInfo.groupOwnerAddress;
                        Log.d(TAG, "WiFi Connected! (The GO MAC is: " + p2pGroup.getOwner().deviceAddress + " & IP is: " + goAddress + ")");
                        mActivity.mStatusText.append("WiFi Connected! (The GO MAC is: " + p2pGroup.getOwner().deviceAddress + " & IP is: " + goAddress + ")\n");
                        mActivity.mRecipientAddress = goAddress.getHostAddress();
                        new WifiDirectClientAsyncTask(mActivity, "Can you hear me Major Tom?").execute();
                    }
                }
                else {
                    Log.d(TAG, "WiFi Disconnected (p2pGroup getOwner = null)");
                    mActivity.mStatusText.append("WiFi Disconnected (p2pGroup getOwner = null)\n");
                    return;
                }

                Collection<WifiP2pDevice> peerList = p2pGroup.getClientList();
                ArrayList<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>(peerList);

                Log.d(TAG, "p2pGroup list size = " + list.size());
                mActivity.mStatusText.append("p2pGroup list size = " + list.size() + "\n");
                if ( list.size() <= 0 ) {
                    Log.d(TAG, "WiFi Disconnected.");
                    mActivity.mStatusText.append("WiFi Disconnected.\n");
                    return;
                }

                String host = null;
                for (int i = 1; i < list.size(); i++) {
                    host = list.get(i).deviceAddress;
                    Log.d(TAG, "peer #" + i + " address: " + host);
                    mActivity.mStatusText.append("peer #" + i + " address: " + host + "\n");
                }
            } else {
                Log.d(TAG, "WiFi Disconnected (p2pGroup = null)");
                mActivity.mStatusText.append("WiFi Disconnected (p2pGroup = null)\n");
                return;
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED");
            mActivity.mStatusText.append("WIFI_P2P_THIS_DEVICE_CHANGED\n");

            // Respond to this device's wifi state changing
            int wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
            switch(wifiState) {
                case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                    Log.d(TAG, "WIFI_P2P_STATE_DISABLED");
                    mActivity.mStatusText.append("WIFI_P2P_STATE_DISABLED\n");
                    break;
                case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                    Log.d(TAG, "WIFI_P2P_STATE_ENABLED");
                    mActivity.mStatusText.append("WIFI_P2P_STATE_ENABLED\n");
                    break;
                default:
                    Log.d(TAG, "Unknown state.");
                    mActivity.mStatusText.append("Unknown state.\n");
            }
        }
    }
}

