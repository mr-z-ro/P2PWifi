package com.mattgrasser.p2pwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;

/* P2P resources:
 * General P2P flow: https://developer.android.com/guide/topics/connectivity/wifip2p.html
 * Network Service Discovery (NSD): https://developer.android.com/training/connect-devices-wirelessly/nsd.html
 * Wifi P2P for NSD: https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static String TAG;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    TextView mStatusText;
    Button mDiscoverPeers;
    Button mDiscoverAllServices;
    Spinner mDevices;
    Button mConnect;
    Button mGroupInfo;
    EditText mMessage;
    Button mSend;
    Button mDisconnect;

    HashMap<String, WifiDirectDevice> mGoodDevices;
    HashMap<String, WifiDirectDevice> mServiceDevices;
    String mRecipientAddress;
    boolean mIsGO = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TAG = this.getClass().getCanonicalName();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        WifiDirectUtilities.deletePersistentGroups(mManager, mChannel);
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, new WifiDirectPeerListListener(this));

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mStatusText = (TextView) findViewById(R.id.status);
        mDiscoverPeers = (Button) findViewById(R.id.discover_peers);
        mDiscoverPeers.setOnClickListener(this);
        mDiscoverAllServices = (Button) findViewById(R.id.discover_all_services);
        mDiscoverAllServices.setOnClickListener(this);
        mDevices = (Spinner) findViewById(R.id.devices);
        mConnect = (Button) findViewById(R.id.connect);
        mConnect.setOnClickListener(this);
        mGroupInfo = (Button) findViewById(R.id.group_info);
        mGroupInfo.setOnClickListener(this);
        mMessage = (EditText) findViewById(R.id.message);
        mSend = (Button) findViewById(R.id.send);
        mDisconnect = (Button) findViewById(R.id.disconnect);
        mDisconnect.setOnClickListener(this);
        mSend.setOnClickListener(this);
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        registerLocalService();
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        mManager.clearLocalServices(mChannel, null);
    }
    /* unregister the network discovery service
     * https://developer.android.com/training/connect-devices-wirelessly/nsd.html#teardown
     */
    @Override
    protected void onDestroy() {
        mManager.clearLocalServices(mChannel, null);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mDiscoverPeers.getId()) {
            discoverPeers();
        } else if (v.getId() == mDiscoverAllServices.getId()) {
            discoverServices();
        } else if (v.getId() == mConnect.getId()) {
            connectToPeer();
        } else if (v.getId() == mGroupInfo.getId()) {
            mManager.requestGroupInfo(mChannel, new WifiDirectGroupInfoListener(mManager, mChannel, this));
        } else if (v.getId() == mSend.getId()) {
            sendMessage();
        } else if (v.getId() == mDisconnect.getId()) {
            disconnectPeers();
        }
    }

    private void connectToPeer() {
        // Connect to the selected device
        mStatusText.setText("Connecting to device...");
        String deviceKey = (String) mDevices.getSelectedItem();
        final WifiDirectDevice device = mGoodDevices.get(deviceKey);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC; // push-button connect (as opposed to PIN, etc)

        // Set groupOwnerIntent so there aren't 2 groupowners.
        // It ranges between 0-15 , higher the value, higher
        // the possibility of becoming a groupOwner
        config.groupOwnerIntent = 0;

        if (mServiceDevices != null) {
            final WifiDirectDevice wDevice = mServiceDevices.get(deviceKey);
            if (wDevice != null && !wDevice.isGroupOwner && Integer.valueOf(wDevice.record.get("id").toString()) < WifiDirectUtilities.ID) {
//                // Create a group
//                mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
//                    @Override
//                    public void onSuccess() {
//                        MainActivity.this.onGroupCreateResult(-1);
//                    }
//
//                    @Override
//                    public void onFailure(int reasonCode) {
//                        MainActivity.this.onGroupCreateResult(reasonCode);
//                    }
//                });
                this.mIsGO = true;
                config.groupOwnerIntent = 1;
            }
        }

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                mStatusText.setText("Connected via WiFi!");
                // BroadcastReceiver will note when the connection changes in WIFI_P2P_PEERS_CHANGED_ACTION
            }

            @Override
            public void onFailure(int reasonCode) {
                onFailureReasonCode(reasonCode);
            }
        });
    }

    private void disconnectPeers() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "removeGroup success");
                WifiDirectUtilities.deletePersistentGroups(mManager, mChannel);
                mRecipientAddress = null;
                mGoodDevices = null;
                mServiceDevices = null;
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "removeGroup fail: " + reason);
            }
        });
    }

    private void sendMessage() {
        // If we don't have an address, validate that a device is selected and a message has been entered
        if (mRecipientAddress == null) {
            if (mDevices.getSelectedItem() == null) {
                mStatusText.setText("Please select a device to message.");
                return;
            }
            if (mMessage.getText() == null || mMessage.getText().toString().equals("")) {
                mStatusText.setText("Please enter a message to send.");
                return;
            }
        }
        WifiDirectClientAsyncTask t = new WifiDirectClientAsyncTask(this, mMessage.getText().toString());
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
            t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            t.execute();
    }

    private void discoverPeers() {
        mManager.discoverPeers(mChannel,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                            /* If the discovery process succeeds and detects peers, the
                             * system broadcasts the WIFI_P2P_PEERS_CHANGED_ACTION intent,
                             * which you can listen for in a broadcast receiver to obtain
                             * a list of peers.
                             */
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        onFailureReasonCode(reasonCode);
                    }
                });
    }

    private void discoverServices() {
        mManager.addServiceRequest(mChannel, WifiP2pDnsSdServiceRequest.newInstance(WifiDirectUtilities.SERVICE_TYPE),
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Success!
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        onFailureReasonCode(reasonCode);
                    }
                }
        );
        mManager.discoverServices(mChannel,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Success!
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        onFailureReasonCode(reasonCode);
                    }
                }
        );
    }

    private void onFailureReasonCode(int reasonCode) {
        String reason = "Unknown";
        switch (reasonCode) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                reason = "P2P not supported on device";
                break;
            case WifiP2pManager.BUSY:
                reason = "Busy";
                break;
            case WifiP2pManager.ERROR:
                reason = "General error";
                break;
        }
        mStatusText.setText("Connection failed, reason: " + reason);
    }

    public void onWifiP2pStateChange(boolean isEnabled) {
        mStatusText.setText("P2P WiFi is enabled: " + isEnabled);
        if (isEnabled) {
            // Check if we have a group
            mManager.requestGroupInfo(mChannel, new WifiDirectGroupInfoListener(mManager, mChannel, this));
        }
    }

    private void registerLocalService() {
        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(WifiDirectUtilities.SERVICE_NAME,
                        "_" + WifiDirectUtilities.SERVICE_TYPE + "._" + WifiDirectUtilities.SERVICE_PROTOCOL,
                        WifiDirectUtilities.SERVICE_RECORD);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
        WifiDirectDnsSdListener listener = new WifiDirectDnsSdListener(this);
        mManager.setDnsSdResponseListeners(mChannel, listener, listener);
    }

    public void onGroupCreateResult(int result) {
        if (result < 0) { // success
            mStatusText.append("(group is created)");
        } else {
            onFailureReasonCode(result);
            mStatusText.append("(group create failed)");
        }
    }
}
