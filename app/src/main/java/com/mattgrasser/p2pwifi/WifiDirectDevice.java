package com.mattgrasser.p2pwifi;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by matt on 5/21/16.
 */
public class WifiDirectDevice extends WifiP2pDevice {
    public boolean isGroupOwner;
    public Map record;

    public static final Creator<WifiP2pDevice> CREATOR = null;

    public WifiDirectDevice(WifiP2pDevice device) {
        super(device);
        this.isGroupOwner = false;
    }

    public void addServiceRecord(Map record) {
        this.record = record;
    }
}
