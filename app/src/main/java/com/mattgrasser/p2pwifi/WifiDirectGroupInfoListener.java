package com.mattgrasser.p2pwifi;

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.view.View;

class WifiDirectGroupInfoListener implements WifiP2pManager.GroupInfoListener {
    private static final String TAG = WifiDirectGroupInfoListener.class.getCanonicalName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity mainActivity;

    public WifiDirectGroupInfoListener(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity mainActivity) {
        this.manager = manager;
        this.channel = channel;
        this.mainActivity = mainActivity;
    }

    @Override

    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup != null) {
            mainActivity.logEvent(TAG, "[Group Created]:\n " + wifiP2pGroup.toString());
        } else if (mainActivity.mIsGO) {
            mainActivity.logEvent(TAG, "Group info is not available (is null)! Creating now.");

            // Create a group
            manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    mainActivity.logEvent(TAG, "Group creation re-initiation successful.");
                }

                @Override
                public void onFailure(int reasonCode) {
                    mainActivity.logEvent(TAG, "Group creation failed:" + reasonCode);
                }
            });
        }
    }
}