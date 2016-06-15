package com.mattgrasser.p2pwifi;

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class WifiDirectUtilities {
    private static final String TAG = WifiDirectUtilities.class.getCanonicalName();

    public static int PORT = 8888;
    public static String NAME = "John Doe";
    public static int ID = (int) (Math.random() * 1000);
    public static String SERVICE_NAME = "grasschat";
    public static String SERVICE_TYPE = "http";
    public static String SERVICE_PROTOCOL = "tcp";

    //  Create a string map containing information about your service.
    public static Map SERVICE_RECORD = new HashMap() {
        {
            put("listenport", String.valueOf(PORT));
            put("username", NAME);
            put("id", String.valueOf(ID));
        }
    };

    // http://stackoverflow.com/a/11790407: get IP from MAC
    // http://stackoverflow.com/a/14480530/604003: P2P vs universal MAC
    public static String getIPFromMac(String MAC) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {

                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    // Basic sanity check
                    String device = splitted[5];
                    if (device.matches(".*p2p-p2p0.*")){
                        String mac = splitted[3];
                        if (mac.equals(MAC)) {
                            return splitted[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // http://stackoverflow.com/questions/15152817/can-i-change-the-group-owner-in-a-persistent-group-in-wi-fi-direct/26242221#26242221
    public static void deletePersistentGroups(WifiP2pManager manager, WifiP2pManager.Channel channel){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(manager, channel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}