package com.mattgrasser.p2pwifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;

class WifiDirectDnsSdListener implements WifiP2pManager.DnsSdTxtRecordListener, WifiP2pManager.DnsSdServiceResponseListener {
    private static final String TAG = WifiDirectDnsSdListener.class.getCanonicalName();
    private MainActivity mainActivity;

    public WifiDirectDnsSdListener(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
        /* Callback includes:
         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */

    public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
        Log.d(TAG, "DnsSdTxtRecord available: " + record.toString()  + " at " + fullDomain);
        mainActivity.mServiceDevices = new HashMap<String, WifiDirectDevice>();

        WifiDirectDevice wDevice = new WifiDirectDevice(device);
        wDevice.addServiceRecord(record);

        mainActivity.mServiceDevices.put(device.deviceName + "(" + device.deviceAddress + ")", wDevice);
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                        WifiP2pDevice resourceType) {

        if (mainActivity.mServiceDevices == null) {
            mainActivity.mServiceDevices = new HashMap<String, WifiDirectDevice>();
        }
        // Update the device name with the human-friendly version from
        // the DnsTxtRecord, assuming one arrived.
        resourceType.deviceName = mainActivity.mServiceDevices
                .containsKey(resourceType.deviceName + "(" + resourceType.deviceAddress + ")") ? mainActivity.mServiceDevices
                .get(resourceType.deviceName + "(" + resourceType.deviceAddress + ")").deviceName : resourceType.deviceName;

        // Prepare Device values for the array
        String[] spinnerArray = new String[mainActivity.mServiceDevices.size()];
        mainActivity.mServiceDevices.keySet().toArray(spinnerArray);

        Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
    }
}