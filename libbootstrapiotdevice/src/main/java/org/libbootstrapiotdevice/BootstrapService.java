package org.libbootstrapiotdevice;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
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
import android.util.Log;

import org.libbootstrapiotdevice.network.BootstrapCore;
import org.libbootstrapiotdevice.network.UDPMulticastSendReceive;
import org.libbootstrapiotdevice.network.WifiChangedObserverAndroid4;
import org.libbootstrapiotdevice.network.WifiChangedObserverAndroid5;
import org.libbootstrapiotdevice.network.WifiUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import cc.mvdan.accesspoint.WifiApControl;

/**
 * Responsible for opening/closing the wireless Access Point, the udp network
 * and an instance of the {@see BootstrapCore} bootstrap logic.
 */
public class BootstrapService extends Service implements Handler.Callback {
    private static int TEST_WIFI_AND_RESET = 10;
    private static int TEST_AP_WIFI = 11;
    // Service Related
    private final IBinder mBinder = new LocalBinder();
    @Nullable
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private WifiChangedObserverAndroid5 wifiChangedObserverAndroid5;
    private WifiChangedObserverAndroid4 wifiChangedObserverAndroid4;
    // Timeout handler
    private Handler handler;
    // Access Point Owner: We want to offer the user the automatic
    // reverting of the network settings, that are changed during the
    // bootstrap process (stopping the access point etc). To realize
    // that, we revert network settings on every activity onStop(),
    // as long as the current activity is still responsible / the owner.
    // To not revert/reestablish network settings on every activity
    // change, new activities claim the ownership in onStart().
    private int ap_owner = 0;

    // Bootstrap logic core and network
    private BootstrapCore bootstrapCore;
    private UDPMulticastSendReceive udpNetwork;
    private String access_point_key;
    private String access_point_ssid;
    private int orig_networkId;
    private boolean bootstrapInSameNetwork; //< Do we need an access point?

    /**
     * @return Return true if we need an access point to setup the nodes.
     */
    public boolean isBootstrapInSameNetwork() {
        return bootstrapInSameNetwork;
    }

    /**
     * If your nodes are part of the current network already (for example because
     * they are wired instead of wireless), set this to true. No access point
     * will be established in this case.
     *
     * @param sameNetwork Set to true to not establish an access point.
     */
    public void setBootstrapInSameNetwork(boolean sameNetwork) {
        this.bootstrapInSameNetwork = sameNetwork;
    }

    /**
     * @return Return the bootstrap core instance.
     */
    public BootstrapCore getBootstrapCore() {
        return bootstrapCore;
    }

    /**
     * Testing a new wifi connection and testing if an access point could be established
     * requires a timeout handler. This is the handleMessage() for that handler.
     * This handler is called by a delayed message (~5sec) and also from the WifiChangedObserverAndroid4
     * and WifiChangedObserverAndroid5 if a connection to a new wifi network is established.
     *
     * @param msg Must contain a NetworkConnectivityResponse as object, which contains the destination
     *            ssid and a callback.
     */
    @Override
    public boolean handleMessage(Message msg) {
        NetworkConnectivityResponse response = (NetworkConnectivityResponse) msg.obj;
        if (response.isDone)
            return true;

        response.isDone = true;
        wifiChangedObserverAndroid4.removeListener(response);
        wifiChangedObserverAndroid5.removeListener(response);

        assert wifiManager != null;

        if (response.what == TEST_WIFI_AND_RESET) {
            boolean connected = WifiUtils.isConnectedToWifi(wifiManager, response.ssid);
            WifiUtils.restoreNetwork(wifiManager, connectivityManager, orig_networkId);
            WifiUtils.removeStoredNetwork(wifiManager, response.ssid);
            response.callback.wifiSuccess(connected);
        } else if (response.what == TEST_AP_WIFI) {
            WifiApControl apControl = WifiApControl.getInstance(this);
            String ssid = apControl.getWifiApConfiguration().SSID;
            boolean connected = WifiUtils.checkWifiAP(this, ssid);
            if (connected) {
                Network current_network = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    Network n[] = connectivityManager.getAllNetworks();
                    for (Network network : n) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo.isConnected() &&
                                networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                            current_network = network;
                            break;
                        }
                    }
                }

                // Broadcasting on the Access Point network will only work if we set the
                // network interface on the udp socket on our self.
                NetworkInterface current_interface = null;
                try {
                    Inet4Address ipv4 = apControl.getInet4Address();
                    final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
                    while (current_interface == null && e.hasMoreElements()) {
                        final NetworkInterface networkInterface = e.nextElement();
                        Enumeration<InetAddress> i = networkInterface.getInetAddresses();
                        while (i.hasMoreElements()) {
                            InetAddress address = i.nextElement();
                            if (address.equals(ipv4)) {
                                current_interface = networkInterface;
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    Log.wtf("WIFIIP", "Unable to NetworkInterface.getNetworkInterfaces()");
                }

                udpNetwork.start(wifiManager,
                        current_network, current_interface,
                        BootstrapCore.RECEIVE_PORT, bootstrapCore);
            }
            response.callback.wifiSuccess(connected);
        }

        return true;
    }

    /**
     * Take over the owenership of the access point / network settings. The
     * activity that was the owner before is not able to change the settings
     * anymore.
     * @param activity The new activity that is responsible for calling restoreWifi() in onStop().
     */
    public void takeOverAP(@NonNull Activity activity) {
        ap_owner = activity.hashCode();
    }

    /**
     * Call restoreWifi() in your activity in onStop(). In onStart() you should have called
     * takeOverAP(). If you are still the owner / responsible for the network changes, this
     * method will revert them.
     *
     * @param activity The current activity.
     */
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

    /**
     * Start an access point with the access_point_ssid and access_point_key stored in this service.
     * You may provide a callback to get notified about the outgoing.
     *
     * @param activity If you want to take the ownership/responsibility for this network.
     * @param callback The callback
     */
    public void startWifiAP(@Nullable Activity activity, NetworkConnectivityResponse.Callback callback) {
        if (activity != null)
            ap_owner = activity.hashCode();
        else
            ap_owner = 0;

        WifiApControl apControl = WifiApControl.getInstance(this);
        if (apControl == null)
            return;
        assert wifiManager != null;
        orig_networkId = WifiUtils.getCurrentNetworkID(wifiManager);
        NetworkConnectivityResponse response = new NetworkConnectivityResponse(callback,
                handler, TEST_AP_WIFI, access_point_ssid);

        WifiUtils.startAP(apControl, wifiManager, access_point_ssid, access_point_key);

        // Listen to network changes
        wifiChangedObserverAndroid4.addListener(response);
        wifiChangedObserverAndroid5.addListener(response);
        // Maximum time to wait and check if the AP is created
        handler.sendMessageDelayed(handler.obtainMessage(TEST_AP_WIFI, response), 5000);
    }

    /**
     * Tests if a wireless network is available and can be connected to with the given credentials.
     *
     * @param callback The callback.
     * @param ssid     The network ssid.
     * @param password The network password.
     */
    public void testWifi(NetworkConnectivityResponse.Callback callback, String ssid, String password) {
        WifiApControl apControl = WifiApControl.getInstance(this);
        if (apControl != null)
            apControl.disable();

        NetworkConnectivityResponse response = new NetworkConnectivityResponse(callback,
                handler, TEST_WIFI_AND_RESET, ssid);

        if (WifiUtils.connectToWifi(wifiManager, ssid, password)) {
            callback.wifiSuccess(true);
        } else {
            // Listen to network changes
            wifiChangedObserverAndroid4.addListener(response);
            wifiChangedObserverAndroid5.addListener(response);
            // Maximum time to wait and check if the connection is established
            handler.sendMessageDelayed(handler.obtainMessage(TEST_WIFI_AND_RESET, response), 5000);
        }
    }

    /**
     * Always call this before doing anything meaningful. Returns false if the required permissions
     * are not granted so far. CNotify the user and call openPermissionSettings() in that case.
     * @return Return true if everything is ok or false if permissions are missing.
     */
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

    /**
     * On android 6 we need the WRITE_SETTINGS permission to create an access point. This method
     * will guide the user to the settings context where he can grant access to the app.
     * @param context A context.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void openPermissionSettings(Context context) {
        Intent goToSettings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        goToSettings.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(goToSettings);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiChangedObserverAndroid5 = new WifiChangedObserverAndroid5(wifiManager);
        wifiChangedObserverAndroid4 = new WifiChangedObserverAndroid4(wifiManager);
        handler = new Handler(Looper.myLooper(), this);

        // Get notified of wifi changes
        if (!wifiChangedObserverAndroid5.register(connectivityManager))
            wifiChangedObserverAndroid4.register(this);

        String bound_key = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String unbound_key = this.getString(R.string.unbound_key);
        access_point_ssid = this.getString(R.string.access_point_ssid);
        access_point_key = this.getString(R.string.access_point_key);
        bootstrapCore = new BootstrapCore(null, bound_key.getBytes(), unbound_key.getBytes(),
                access_point_ssid);
        udpNetwork = new UDPMulticastSendReceive();

        try {
            InetAddress multicastGroup = InetAddress.getByName("239.0.0.57");
            udpNetwork.setBroadcastAddress(multicastGroup);
        } catch (UnknownHostException ignored) {
        }

        bootstrapCore.setNetwork(udpNetwork);
    }

    @Override
    public void onDestroy() {
        wifiChangedObserverAndroid5.unregister(connectivityManager);
        wifiChangedObserverAndroid4.unregister(this);

        restoreWifi(null);
        udpNetwork.tearDown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * @return Return the access point key.
     */
    public String getAccessPointKey() {
        return access_point_key;
    }

    /**
     * @return Return the access point ssid.
     */
    public String getAccessPointSsid() {
        return access_point_ssid;
    }

    /**
     * The binder to get the service instance in activities.
     */
    public class LocalBinder extends Binder {
        public BootstrapService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BootstrapService.this;
        }
    }

}
