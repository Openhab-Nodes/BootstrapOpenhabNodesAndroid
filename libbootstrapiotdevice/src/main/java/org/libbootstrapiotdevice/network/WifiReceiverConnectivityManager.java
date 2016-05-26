package org.libbootstrapiotdevice.network;

import android.annotation.TargetApi;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import org.libbootstrapiotdevice.NetworkConnectivityResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Like the WifiReceiver but for android >= 5. React to wifi changes and
 * informs all registered listeners. Listeners are one-time only.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WifiReceiverConnectivityManager extends ConnectivityManager.NetworkCallback {
    private WifiManager wifiManager;
    private List<NetworkConnectivityResponse> listeners = new ArrayList<>();

    public WifiReceiverConnectivityManager(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onAvailable(final Network network) {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null)
            return;

        if (info.getSupplicantState() == SupplicantState.COMPLETED &&
                info.getBSSID() != null && info.getIpAddress() != 0) {
            for (NetworkConnectivityResponse listener : listeners) {
                listener.notifyWifiChanged(info.getSSID());
            }
            listeners.clear();
        }
    }

    @Override
    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null)
            return;

        Log.w("WifiReceiverM", "onLinkPropertiesChanged " + info.getSSID());
    }

    public void addListener(NetworkConnectivityResponse listener) {
        this.listeners.add(listener);
    }

    public void removeListener(NetworkConnectivityResponse listener) {
        this.listeners.remove(listener);
    }

    public boolean register(ConnectivityManager connectivityManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
            try {
                connectivityManager.requestNetwork(request, this);
                return true;
            } catch (SecurityException e) {
                Log.w("Service", e.getMessage());
                return false;
            }
        }
        return false;
    }

    public void unregister(ConnectivityManager connectivityManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                connectivityManager.bindProcessToNetwork(null);
                connectivityManager.unregisterNetworkCallback(this);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
