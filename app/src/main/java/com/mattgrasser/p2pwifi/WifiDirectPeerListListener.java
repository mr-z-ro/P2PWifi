package com.mattgrasser.p2pwifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;

class WifiDirectPeerListListener implements WifiP2pManager.PeerListListener {
    private static final String TAG = WifiDirectPeerListListener.class.getCanonicalName();
    private MainActivity mainActivity;

    public WifiDirectPeerListListener(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        // Look for p2p devices available
        Log.d(TAG, wifiP2pDeviceList.toString());
        mainActivity.mGoodDevices = new HashMap();
        for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
            WifiDirectDevice wDevice = new WifiDirectDevice(device);
            if (device.isServiceDiscoveryCapable()) {
                mainActivity.mGoodDevices.put(device.deviceName + "(" + device.deviceAddress + ")", wDevice);
            }
        }


        if (mainActivity.mGoodDevices.size() > 0) {
            // Prepare Device values for the array
            String[] spinnerArray = new String[mainActivity.mGoodDevices.size()];
            mainActivity.mGoodDevices.keySet().toArray(spinnerArray);
            ArrayAdapter<String> adapter =new ArrayAdapter<String>(mainActivity,android.R.layout.simple_spinner_item, spinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Present the values for selection
            mainActivity.mDevices.setAdapter(adapter);
        } else {
            mainActivity.mStatusText.append("No compatible devices found.\n");
        }
    }
}