package org.libbootstrapiotdevice;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.libbootstrapiotdevice.network.BootstrapDevices;
import org.libbootstrapiotdevice.network.UDPMulticastSendReceive;
import org.libbootstrapiotdevice.network.WifiReceiver;
import org.libbootstrapiotdevice.network.WifiReceiverConnectivityManager;
import org.libbootstrapiotdevice.network.WifiUtils;

import cc.mvdan.accesspoint.WifiApControl;

public class BootstrapService extends Service implements Handler.Callback {
    private static int TEST_WIFI_AND_RESET = 10;
    private static int TEST_AP_WIFI = 11;
    private final IBinder mBinder = new LocalBinder();
    BootstrapDevices bootstrapDevices;
    int ap_owner = 0;
    private String access_point_key;
    private String access_point_ssid;
    private UDPMulticastSendReceive udpNetwork;
    private int orig_networkId;
    private Handler handler;
    private boolean sameNetwork;
    @Nullable
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private WifiReceiverConnectivityManager wifiReceiverM;
    private WifiReceiver wifiReceiver;

    public boolean isSameNetwork() {
        return sameNetwork;
    }

    public void setSameNetwork(boolean sameNetwork) {
        this.sameNetwork = sameNetwork;
    }

    @Override
    public boolean handleMessage(Message msg) {
        NetworkConnectivityResponse response = (NetworkConnectivityResponse) msg.obj;
        if (response.isDone)
            return true;

        response.isDone = true;
        wifiReceiver.removeListener(response);
        wifiReceiverM.removeListener(response);

        if (response.what == TEST_WIFI_AND_RESET) {
            boolean connected = WifiUtils.isConnectedToWifi(wifiManager, response.ssid);
            WifiUtils.restoreNetwork(wifiManager, connectivityManager, orig_networkId);
            WifiUtils.removeStoredNetwork(wifiManager, response.ssid);
            response.callback.wifiSuccess(connected);
        } else if (response.what == TEST_AP_WIFI) {
            WifiApControl apControl = WifiApControl.getInstance(this);
            String ssid = apControl.getWifiApConfiguration().SSID;
            boolean connected = WifiUtils.checkWifiAP(this, ssid);
            if (connected)
                udpNetwork.start(this, BootstrapDevices.RECEIVE_PORT, bootstrapDevices);
            response.callback.wifiSuccess(connected);
        }

        return true;
    }

    public void takeOverAP(@NonNull Activity activity) {
        ap_owner = activity.hashCode();
    }

    public void restoreWifi(@Nullable Activity activity) {
        // Only the activity which started or took over the ownership
        // may restore the wifi / close the AP.
        if (ap_owner != 0 && activity != null && activity.hashCode() != ap_owner)
            return;

        WifiApControl apControl = WifiApControl.getInstance(this);
        if (apControl != null) {
            apControl.disable();
        }

        if (orig_networkId != -1)
            WifiUtils.restoreNetwork(wifiManager, connectivityManager, orig_networkId);
        orig_networkId = -1;
    }

    public void startWifiAP(@Nullable Activity activity, Callback callback) {
        if (activity != null)
            ap_owner = activity.hashCode();
        else
            ap_owner = 0;

        WifiApControl apControl = WifiApControl.getInstance(this);
        if (apControl == null)
            return;
        orig_networkId = WifiUtils.getCurrentNetworkID(wifiManager);
        NetworkConnectivityResponse response = new NetworkConnectivityResponse(callback,
                handler, TEST_AP_WIFI, access_point_ssid);

        assert wifiManager != null;
        WifiUtils.startAP(apControl, wifiManager, access_point_ssid, access_point_key);

        // Listen to network changes
        wifiReceiver.addListener(response);
        wifiReceiverM.addListener(response);
        // Maximum time to wait and check if the AP is created
        handler.sendMessageDelayed(handler.obtainMessage(TEST_AP_WIFI, response), 5000);
    }

    public void testWifi(Callback callback, String ssid, String password) {
        WifiApControl apControl = WifiApControl.getInstance(this);
        if (apControl != null)
            apControl.disable();

        NetworkConnectivityResponse response = new NetworkConnectivityResponse(callback,
                handler, TEST_WIFI_AND_RESET, ssid);

        if (WifiUtils.connectToWifi(wifiManager, ssid, password)) {
            callback.wifiSuccess(true);
        } else {
            // Listen to network changes
            wifiReceiver.addListener(response);
            wifiReceiverM.addListener(response);
            // Maximum time to wait and check if the connection is established
            handler.sendMessageDelayed(handler.obtainMessage(TEST_WIFI_AND_RESET, response), 5000);
        }
    }

    public boolean checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();

                ConnectivityManager.NetworkCallback t = new ConnectivityManager.NetworkCallback() {
                };
                cm.requestNetwork(request, t);
                cm.unregisterNetworkCallback(t);
                return WifiApControl.getInstance(this) != null;
            } catch (SecurityException e) {
                return false;
            }
        } else
            return WifiApControl.getInstance(this) != null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void openPermissionSettings(Activity activity) {
        Intent goToSettings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        goToSettings.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(goToSettings);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiReceiverM = new WifiReceiverConnectivityManager(wifiManager);
        wifiReceiver = new WifiReceiver(wifiManager);
        handler = new Handler(Looper.myLooper(), this);

        // Get notified of wifi changes
        if (!wifiReceiverM.register(connectivityManager))
            wifiReceiver.register(this);

        String bound_key = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String unbound_key = this.getString(R.string.unbound_key);
        access_point_ssid = this.getString(R.string.access_point_ssid);
        access_point_key = this.getString(R.string.access_point_key);
        bootstrapDevices = new BootstrapDevices(null, bound_key.getBytes(), unbound_key.getBytes(),
                access_point_ssid);
        udpNetwork = new UDPMulticastSendReceive();
        bootstrapDevices.setNetwork(udpNetwork);
    }

    public BootstrapDevices getBootstrapDevices() {
        return bootstrapDevices;
    }

    @Override
    public void onDestroy() {
        wifiReceiverM.unregister(connectivityManager);
        wifiReceiver.unregister(this);

        restoreWifi(null);
        udpNetwork.tearDown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getAccessPointKey() {
        return access_point_key;
    }

    public String getAccessPointSsid() {
        return access_point_ssid;
    }

    public interface Callback {
        void wifiSuccess(boolean success);
    }

    public class LocalBinder extends Binder {
        public BootstrapService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BootstrapService.this;
        }
    }

}
