package org.libbootstrapiotdevice.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;

import org.libbootstrapiotdevice.WirelessNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Scan wifiManager networks in range for ssid with a pattern like "BST_device-name_IDIDID"
 */
public class BootstrapDevicesViaWifiScan extends BroadcastReceiver implements Handler.Callback {
    Set<BootstrapDeviceUpdateListener> observers = Collections.newSetFromMap(
            new WeakHashMap<BootstrapDeviceUpdateListener, Boolean>());
    private WifiManager wifiManager;
    private List<WirelessNetwork> entries = new ArrayList<>();
    private Handler delayHandler = new Handler(this);

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == 0) {
            wifiManager.startScan();
            delayHandler.removeMessages(0);
            delayHandler.sendEmptyMessageDelayed(0, 60000);
        }
        return true;
    }

    public List<WirelessNetwork> getEntries() {
        return entries;
    }

    public boolean start(@NonNull Context context) {
        if (PermissionChecker.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
        }

        context.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        delayHandler.sendEmptyMessage(0);
        return true;
    }

    public void stop(Context context) {
        delayHandler.removeMessages(0);
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException ignored) {}
    }

    public void addObserver(BootstrapDeviceUpdateListener observer) {
        observers.add(observer);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        entries.clear();
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult result: results) {
            WirelessNetwork b = new WirelessNetwork();
            if (result.SSID.length()>11 && result.SSID.charAt(result.SSID.length()-7)=='_' && result.SSID.charAt(4)=='_' && result.SSID.startsWith("BST")) {
                b.is_bound = result.SSID.charAt(3)=='B';
                b.ssid = result.SSID;
                b.pwd = null;
                b.strength = WifiManager.calculateSignalLevel(result.level, 100);
                entries.add(b);
            }
        }

        for (BootstrapDeviceUpdateListener observer: observers)
            observer.deviceChangesFinished();
    }


}
