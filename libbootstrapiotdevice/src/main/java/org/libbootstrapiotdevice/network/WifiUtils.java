package org.libbootstrapiotdevice.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import cc.mvdan.accesspoint.WifiApControl;

/**
 * Allows to connect to a wifi with callback methods and restore the wifi network that was connected to on start.
 */
public class WifiUtils {

    private static final String TAG = "WifiUtils";

    public static int getCurrentNetworkID(@NonNull WifiManager wifiManager) {
        WifiInfo info = wifiManager.getConnectionInfo();
        return info != null ? info.getNetworkId() : -1;
    }

    public static boolean isConnectedToWifi(@NonNull WifiManager wifiManager, String ssid) {
        WifiInfo info = wifiManager.getConnectionInfo();
        return info != null && info.getSSID().replace("\"","").equals(ssid);
    }

    /**
     * Return the associated network addresses of the current wifi network. On android 5+ you may
     * provide a connectivity manager to make the output more reliable.
     *
     * @param connectivityManager A connectivityManager
     * @return Return a list of ip addresses.
     */
    @Nullable
    @SuppressWarnings("unused")
    public static List<InetAddress> getWifiAddresses(@Nullable ConnectivityManager connectivityManager) {
        if (android.os.Build.VERSION.SDK_INT >= 23 && connectivityManager != null) {
            LinkProperties prop = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork());

            List<InetAddress> addresses = new ArrayList<>();

            if (prop == null)
                return addresses;

            for (LinkAddress link : prop.getLinkAddresses()) {
                if (link.getAddress().isLoopbackAddress())
                    continue;
                if (link.getAddress().isLinkLocalAddress())
                    addresses.add(link.getAddress());
                else
                    addresses.add(0, link.getAddress());
            }
            return addresses;
        }

        List<InetAddress> addresses = new ArrayList<>();
        try {
            final Enumeration<NetworkInterface> e =  NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                final NetworkInterface networkInterface = e.nextElement();
                Enumeration<InetAddress> i = networkInterface.getInetAddresses();
                while (i.hasMoreElements()) {
                    InetAddress address = i.nextElement();
                    if (address.isLoopbackAddress())
                        continue;
                    if (address.isLinkLocalAddress())
                        addresses.add(address);
                    else
                        addresses.add(0, address);
                }
            }
            return addresses;
        } catch (SocketException e) {
            Log.wtf(TAG, "Unable to NetworkInterface.getNetworkInterfaces()");
        }
        return null;
    }

    /**
     * Connect to the given wifi network.
     * @param wifiManager A WifiManager
     * @param ssid The destination ssid
     * @param password The password
     */
    public static boolean connectToWifi(WifiManager wifiManager, String ssid, String password) {
        if (isConnectedToWifi(wifiManager, ssid)) {
            return true;
        }

        if (Looper.myLooper()!=Looper.getMainLooper()) {
            throw new RuntimeException("connectToWifi can only be called in the main thread");
        }

        wifiManager.setWifiEnabled(true);

        WifiConfiguration wifiConfig = null;
        if (wifiManager.getConfiguredNetworks() != null)
            for (WifiConfiguration configuration: wifiManager.getConfiguredNetworks()) {
                if (configuration.SSID.equals("\"" + ssid + "\"")) {
                    wifiConfig = configuration;
                    wifiConfig.preSharedKey = "\"" + password + "\"";
                    break;
                }
            }

        if (wifiConfig == null) {
            wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + ssid + "\"";
            wifiConfig.preSharedKey = "\"" + password + "\"";
            wifiConfig.networkId = wifiManager.addNetwork(wifiConfig);
        }

        if (wifiConfig.networkId == -1) {
            Log.w("WifiUtils", "connectToWifi: values not valid!");
            return false;
        }

        //wifiManager.disconnect();
        wifiManager.enableNetwork(wifiConfig.networkId, true);
        //wifiManager.reconnect();

        return false;
    }

    public static String getNetworkSSIDByID(WifiManager wifiManager, int networkID) {
        if (wifiManager == null || wifiManager.getConfiguredNetworks() == null)
            return null;
        for (WifiConfiguration configuration: wifiManager.getConfiguredNetworks()) {
            if (configuration.networkId == networkID) {
                return configuration.SSID.replace("\"","");
            }
        }
        return null;
    }

    public static void removeStoredNetwork(WifiManager wifiManager, String ssid) {
        ssid = "\"" + ssid + "\"";
        for (WifiConfiguration configuration: wifiManager.getConfiguredNetworks()) {
            if (configuration.SSID.equals(ssid)) {
                wifiManager.disableNetwork(configuration.networkId);
                wifiManager.removeNetwork(configuration.networkId);
            }
        }
    }

    public static void restoreNetwork(WifiManager wifiManager,
                                      ConnectivityManager connectivityManager, int orig_networkId) {
        if (orig_networkId == -1)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(null);
        }

        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo.getNetworkId() == orig_networkId)
            return;

        wifiManager.setWifiEnabled(true);
        wifiManager.enableNetwork(orig_networkId, true);
    }

    public static void startAP(@NonNull WifiApControl apControl, @NonNull WifiManager wifiManager,
                               String ssid, String password) {
        wifiManager.setWifiEnabled(false);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = ssid;
        wifiConfig.preSharedKey = password;
        wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        apControl.setWifiApEnabled(wifiConfig, true);
    }

    public static boolean checkWifiAP(Context context, String ssid, String password) {
        WifiApControl apControl = WifiApControl.getInstance(context);
        return apControl != null && apControl.getWifiApState() == WifiApControl.WIFI_AP_STATE_ENABLED &&
                apControl.getWifiApConfiguration().SSID.equals(ssid) &&
                apControl.getWifiApConfiguration().preSharedKey.equals(password);
    }

    public static boolean checkWifiAP(Context context, String ssid) {
        WifiApControl apControl = WifiApControl.getInstance(context);
        return apControl != null && apControl.getWifiApState() == WifiApControl.WIFI_AP_STATE_ENABLED &&
                apControl.getWifiApConfiguration().SSID.equals(ssid);
    }
}
