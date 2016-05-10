package org.openhab_node.bootstrapopenhabnodes.bootstrap.network;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * Allows to connect to a wifi with callback methods and restore the wifi network that was connected on start
 */
public class WifiUtils extends Handler {

    public interface Callback {
        void connected(boolean connected, List<InetAddress> addresses);
        void canConnect(boolean canConnect);
    }

    private String dest_ssid;
    private String dest_pwd;
    private Callback callback;
    private boolean testOnly;

    private int orig_networkId;

    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;

    private static int CONNECT_FAIL = 0;
    private static int CONNECT_OK = 1;
    private static int CONNECT_NOW = 10;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class WifiReceiverM extends ConnectivityManager.NetworkCallback {
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onAvailable(final Network network) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null)
                return;

            if (info.getSupplicantState() == SupplicantState.COMPLETED &&
                    info.getBSSID() != null && info.getIpAddress() != 0 && info.getSSID().equals(dest_ssid)) {
                Log.w("WifiReceiverM", "bind network to process "+info.getSSID());
                connectivityManager.bindProcessToNetwork(network);
                // We need to wait another second before we can use getWifiAddresses()
                // We need to wait another second before we can use getWifiAddresses()
                removeMessages(CONNECT_OK);
                sendMessageDelayed(obtainMessage(CONNECT_OK), 1000);
            }
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null)
                return;

            Log.w("WifiReceiverM", "onLinkPropertiesChanged "+info.getSSID());
        }
    }

    private WifiReceiverM wifiReceiverM = new WifiReceiverM();

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null)
                return;

            if (info.getSupplicantState() == SupplicantState.COMPLETED && info.getBSSID() != null && info.getIpAddress() != 0) {
                removeMessages(CONNECT_FAIL);
                removeMessages(CONNECT_OK);
                Log.w("WifiReceiver", info.getSSID());
                if(info.getSSID().equals(dest_ssid)) {
                    // We need to wait another second before we can use getWifiAddresses()
                    sendMessageDelayed(obtainMessage(CONNECT_OK), 1000);
                } else if (callback != null){
                    sendMessageDelayed(obtainMessage(CONNECT_FAIL), 2000);
                }
            }
        }
    }

    private WifiReceiver wifiReceiver = new WifiReceiver();

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == CONNECT_NOW) {
            connectToWifi();
            return;
        }
        List<InetAddress> addresses = msg.what==CONNECT_OK ? getWifiAddresses() : null;

        if (callback == null) {
            return;
        }

        if (testOnly) {
            restoreNetwork();
            removeStoredNetwork(dest_ssid);
            callback.canConnect(msg.what==CONNECT_OK);
        } else
            callback.connected(msg.what==CONNECT_OK, addresses);

        callback = null;
        dest_ssid = null;
    }

    @Nullable
    private List<InetAddress> getWifiAddresses() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
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
            Log.wtf("WIFIIP", "Unable to NetworkInterface.getNetworkInterfaces()");
        }
        return null;
    }


    /**
     * Connect to the given wifi network.
     */
    private void connectToWifi() {
        if (Looper.myLooper()!=Looper.getMainLooper()) {
            throw new RuntimeException("connectToWifi can only be called in the main thread");
        }
        if (callback == null)
            return;

        WifiConfiguration wifiConfig = null;
        for (WifiConfiguration configuration: wifiManager.getConfiguredNetworks()) {
            if (configuration.SSID.equals(dest_ssid)) {
                wifiConfig = configuration;
                wifiConfig.preSharedKey = dest_pwd;
                break;
            }
        }

        if (wifiConfig == null) {
            wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = dest_ssid;
            wifiConfig.preSharedKey = dest_pwd;
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiConfig.networkId = netId;
        }

        if (wifiConfig.networkId == -1) {
            Log.w("WifiUtils", "connectToWifi: values not valid!");
            callback.canConnect(false);
            callback.connected(false, null);
            this.callback = null;
            return;
        }

        wifiManager.disconnect();
        wifiManager.enableNetwork(wifiConfig.networkId, true);
        wifiManager.reconnect();
    }

    public void removeStoredNetwork(String ssid) {
        ssid = "\"" + ssid + "\"";
        for (WifiConfiguration configuration: wifiManager.getConfiguredNetworks()) {
            if (configuration.SSID.equals(ssid)) {
                wifiManager.disableNetwork(configuration.networkId);
                wifiManager.removeNetwork(configuration.networkId);
            }
        }
    }

    public void restoreNetwork() {
        removeMessages(CONNECT_FAIL);
        removeMessages(CONNECT_OK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(null);
        }
        wifiManager.disconnect();
        wifiManager.enableNetwork(orig_networkId, true);
        wifiManager.reconnect();
    }

    public void ConnectToWifi(String ssid, String key, boolean testOnly, Callback callback) {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info != null && info.getSSID().equals(ssid)) {
            if (testOnly) {
                callback.canConnect(true);
            } else
                callback.connected(true, getWifiAddresses());
            return;
        }

        // WifiInfo.getSSID() will add quotes to the ssid. See http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID().
        dest_ssid = "\"" + ssid + "\"";
        dest_pwd = "\"" + key + "\"";
        this.testOnly = testOnly;
        this.callback = callback;
        sendEmptyMessageDelayed(CONNECT_FAIL, 20000);
        sendEmptyMessage(CONNECT_NOW);
    }

    public WifiUtils(Context context) {
        super(Looper.getMainLooper());
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public void start(Context context) {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info != null)
            orig_networkId = info.getNetworkId();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
            try {
                connectivityManager.requestNetwork(request, wifiReceiverM);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }


    public void tearDown(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                connectivityManager.bindProcessToNetwork(null);
                connectivityManager.unregisterNetworkCallback(wifiReceiverM);
            } catch (IllegalArgumentException ignored) {
            }
        }
        try {
            context.unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        restoreNetwork();
    }
}
