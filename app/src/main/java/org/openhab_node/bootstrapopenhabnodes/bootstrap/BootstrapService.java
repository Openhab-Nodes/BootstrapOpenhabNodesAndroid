package org.openhab_node.bootstrapopenhabnodes.bootstrap;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.PermissionChecker;
import android.view.View;

import org.openhab_node.bootstrapopenhabnodes.R;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.network.BootstrapNetwork;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.network.WifiUtils;

import java.net.SocketException;

public class BootstrapService extends Service {
    final BootstrapDeviceList bootstrapDeviceList = new BootstrapDeviceList();
    WifiUtils wifiUtils;
    BootstrapNetwork bootstrapNetwork;
    final BootstrapData data = new BootstrapData();
    private final IBinder mBinder = new LocalBinder();
    private BootstrapTask bootstrapTask;
    private WirelessNetwork targetNetwork;
    BootstrapProgressListener observer;

    String default_pwd = "app_secret";
    String specific_pwd;

    public void restoreWifi() {
        wifiUtils.restoreNetwork();
        for (BootstrapDevice device: bootstrapDeviceList.devices) {
            wifiUtils.removeStoredNetwork(device.ssid);
        }
    }

    public enum Mode {
        UnknownMode,
        BindMode,
        DeviceInfoMode,
        BootstrapMode
    }
    Mode mode = Mode.UnknownMode;

    public interface BootstrapProgressListener {
        void BootstrapProgress(BootstrapDevice device, int current, int total, int waitTimeMS);
        void BootstrapFinished();
    }

    public String getDefaultPwd() {
        return default_pwd;
    }

    public void setDefaultPwd(String default_pwd) {
        this.default_pwd = default_pwd;
    }

    public String getSpecificPwd() {
        return specific_pwd;
    }

    public void setSpecificPwd(String specific_pwd) {
        this.specific_pwd = specific_pwd;
    }


    public void startProcess(Mode mode) {
        if (this.mode != Mode.UnknownMode)
            return;
        this.mode = mode;
        bootstrapTask = new BootstrapTask(this);
        bootstrapTask.execute();
    }

    public void checkWifi(WirelessNetwork network, WifiUtils.Callback callback) {
        wifiUtils.ConnectToWifi(network.ssid, network.pwd,true, callback);
    }

    public BootstrapDeviceList getBootstrapDeviceList() {
        return bootstrapDeviceList;
    }

    public void updateBSWifi(WirelessNetwork network) {
        data.bst_ssid = network.ssid;
        data.bst_pwd = network.pwd;
        targetNetwork = network;
    }

    public void updateBSDataAdditional(String bootstrapString) {
        data.bst_additional_bst_data = bootstrapString;
    }

    public void setProgressListener(BootstrapProgressListener listener) {
        this.observer = listener;
    }

    private boolean mStarted = false;

    public interface BootstrapServicePermissions {
        void servicePermissionsGranted();
    }

    public boolean isStarted() {
        return mStarted;
    }

    public boolean checkPermissionsAndStart(@NonNull final Activity activity, @NonNull final View mView, @Nullable final BootstrapServicePermissions callback) {
        if (mStarted)
            return true;

        // Android 6.0 Bug. ConnectivityManager.requestNetwork() needs WRITE_SETTINGS permissions granted
        // by the user
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ConnectivityManager cm = (ConnectivityManager)  activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();

            try {
                ConnectivityManager.NetworkCallback t = new ConnectivityManager.NetworkCallback() { };
                cm.requestNetwork(request, t);
                cm.unregisterNetworkCallback(t);
            } catch (SecurityException e) {
                Snackbar.make(mView, R.string.permission_rationale_write_settings, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.M)
                            @Override
                            public void onClick(View v) {
                                Intent goToSettings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                goToSettings.setData(Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivity(goToSettings);
                            }
                    }).show();
                return false;
            }

            if (PermissionChecker.checkSelfPermission(activity,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Snackbar.make(mView, R.string.permission_rationale_location, Snackbar.LENGTH_INDEFINITE)
                            .setAction(android.R.string.ok, new View.OnClickListener() {
                                @Override
                                @TargetApi(Build.VERSION_CODES.M)
                                public void onClick(View v) {
                                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                                }
                            }).show();
                    return false;
                } else {
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            }
        }

        mStarted = true;
        wifiUtils.start(this);
        if (callback != null)
            callback.servicePermissionsGranted();

        return true;
    }

    public static final int REQUEST_LOCATION=123;

    @Override
    public void onCreate() {
        super.onCreate();
        wifiUtils = new WifiUtils(this);
        bootstrapNetwork = new BootstrapNetwork(this);
        specific_pwd = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public void onDestroy() {
        wifiUtils.tearDown(this);
        bootstrapNetwork.tearDown();
        mStarted = false;
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public BootstrapService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BootstrapService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
