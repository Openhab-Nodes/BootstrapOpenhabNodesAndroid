package org.libbootstrapiotdevice.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.libbootstrapiotdevice.NetworkConnectivityResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * For Android < 5 we use a normal BroadcastReceiver to get notified about
 * a wifi change.
 */
public class WifiReceiver extends BroadcastReceiver {
    private WifiManager wifiManager;
    private List<NetworkConnectivityResponse> listeners = new ArrayList<>();

    public WifiReceiver(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null)
            return;

        if (info.getSupplicantState() == SupplicantState.COMPLETED && info.getBSSID() != null &&
                info.getIpAddress() != 0) {
            for (NetworkConnectivityResponse listener : listeners) {
                listener.notifyWifiChanged(info.getSSID());
            }
            listeners.clear();
        }
    }

    public void register(Context context) {
        context.registerReceiver(this, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    public void addListener(NetworkConnectivityResponse listener) {
        this.listeners.add(listener);
    }

    public void removeListener(NetworkConnectivityResponse listener) {
        this.listeners.remove(listener);
    }

    public void unregister(Context context) {
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
