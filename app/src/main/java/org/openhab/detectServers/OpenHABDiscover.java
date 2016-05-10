package org.openhab.detectServers;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.View;

import org.openhab.bootstrap.R;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

public class OpenHABDiscover {
    Set<OpenHABDiscoverListener> observers = Collections.newSetFromMap(
            new WeakHashMap<OpenHABDiscoverListener, Boolean>());

    private final NsdManager mNsdManager;
    private final NsdManager.DiscoveryListener mDiscoveryListenerHttp = new DiscoveryListener();
    private final NsdManager.DiscoveryListener mDiscoveryListenerHttps = new DiscoveryListener();

    public static final String SERVICE_HTTP = "_openhab-server._tcp.";
    public static final String SERVICE_HTTPS = "_openhab-server-ssl._tcp.";

    public static final String TAG = "OpenHABDiscover";
    private static final int MSG_DISCOVER_SUCCESS = 0;
    private static final int MSG_DISCOVER_STOPPED = 1;
    private static final int MSG_DISCOVER_START = 3;
    private static final int MSG_STARTED_DISCOVERY = 4;

    private ArrayBlockingQueue<ResolveListener> resolveQueue = new ArrayBlockingQueue<>(10);
    private final Semaphore resolveLock = new Semaphore(1, true);

    private class ResolveListener implements NsdManager.ResolveListener {
        private final NsdServiceInfo service;
        private boolean secure;

        ResolveListener(NsdServiceInfo service, boolean secure) {
            this.service = service;
            this.secure = secure;
        }
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed. " + serviceInfo + String.valueOf(errorCode));
            resolveLock.release();
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            //sLog.e(TAG, "Resolve Succeeded. " + serviceInfo);

            OpenHABServer server = new OpenHABServer(serviceInfo.getHost().getHostAddress(), serviceInfo.getPort(), secure);
            handler.sendMessage(handler.obtainMessage(0,server));

            resolveLock.release();
        }
    };

    private class DiscoveryListener implements NsdManager.DiscoveryListener {

        @Override
        public void onDiscoveryStarted(String regType) {
            handler.sendEmptyMessage(MSG_STARTED_DISCOVERY);
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            //sLog.w(TAG, "Service discovery success " + service);
            try {
                if (service.getServiceType().equals(SERVICE_HTTP)) {
                    resolveQueue.put(new ResolveListener(service, false));
                } else if (service.getServiceType().equals(SERVICE_HTTPS)){
                    resolveQueue.put(new ResolveListener(service, true));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            //Log.e(TAG, "service lost " + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            //Log.i(TAG, "Discovery stopped: " + serviceType);
            handler.sendEmptyMessage(MSG_DISCOVER_STOPPED);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            //Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        }
    }

    public void addObserver(OpenHABDiscoverListener observer) {
        observers.add(observer);
    }

    Thread thread = null;

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISCOVER_SUCCESS:
                    for (OpenHABDiscoverListener observer: observers) {
                        observer.onOpenHABDiscovered((OpenHABServer) msg.obj);
                        observer.onOpenHABDiscoveryFinished(OpenHABDiscoverListener.FinishedState.Success);
                    }
                    break;
                case MSG_DISCOVER_STOPPED:
                    for (OpenHABDiscoverListener observer: observers)
                        observer.onOpenHABDiscoveryFinished(OpenHABDiscoverListener.FinishedState.Stopped);
                    break;
                case MSG_DISCOVER_START:
                    Context context = (Context)msg.obj;
                    discoverNow(context);
                    break;
                case MSG_STARTED_DISCOVERY:
                    for (OpenHABDiscoverListener observer: observers)
                        observer.onOpenHABDiscoveryStarted();
                    break;
            }
        }
    };

    public OpenHABDiscover(NsdManager mNsdManager) {
        this.mNsdManager = mNsdManager;
        if (mNsdManager == null)
            throw new RuntimeException("NSD_SERVICE not found");
    }

    private boolean isRunning = false;


    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null)
                return;

            if (info.getSupplicantState() == SupplicantState.COMPLETED && info.getBSSID() != null && info.getIpAddress() != 0) {
                discoverNow(context);
            }
        }
    }

    private WifiReceiver wifiReceiver = new WifiReceiver();


    public synchronized void discoverNow(Context context) {
        stopDiscoveryIntern(context);
        startDiscoveryIntern(context);
    }

    public interface OpenHABPermissions {
        void openhabPermissionsGranted();
    }

    public static final int REQUEST_LOCATION=123;

    public boolean startDiscoveryWithPermissions(@NonNull final Activity activity, @NonNull final View targetViewForSnackbar) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (PermissionChecker.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Snackbar.make(targetViewForSnackbar, R.string.permission_rationale_location, Snackbar.LENGTH_INDEFINITE)
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

        startDiscovery(activity);

        return true;
    }

    public void startDiscovery(Context context) {
        if (isRunning)
            return;

        isRunning = true;

        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        if (thread == null) {
            thread = new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            resolveLock.acquire();
                            ResolveListener l = resolveQueue.take();
                            if (l.service == null)
                                break;
                            mNsdManager.resolveService(l.service, l);
                        }
                    } catch (InterruptedException ignored) {
                    }
                    thread = null;
                }
            };
            thread.start();
        }

        startDiscoveryIntern(context);
    }

    private void startDiscoveryIntern(Context context) {
        try {
            mNsdManager.discoverServices(
                    SERVICE_HTTP, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListenerHttp);
            mNsdManager.discoverServices(
                    SERVICE_HTTPS, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListenerHttps);
        } catch (IllegalArgumentException ignored) {  }

        handler.sendMessageDelayed(handler.obtainMessage(MSG_DISCOVER_START, context), 20000);
        handler.sendEmptyMessageDelayed(MSG_DISCOVER_STOPPED, 6000);
    }

    private void stopDiscoveryIntern(Context context) {
        handler.removeMessages(MSG_DISCOVER_START);
        handler.removeMessages(MSG_DISCOVER_STOPPED);
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListenerHttp);
            mNsdManager.stopServiceDiscovery(mDiscoveryListenerHttps);
        } catch (IllegalArgumentException ignored) {  }

    }

    public void stopDiscovery(Context context) {
        if (!isRunning)
            return;

        try {
            context.unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException ignored) {
        }


        if (thread != null) {
            try {
                resolveQueue.put(new ResolveListener(null, false));
                if (thread != null)
                    thread.join(500);
            } catch (InterruptedException ignored) {
            }
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            resolveQueue.clear();
        }
        resolveLock.release();

        stopDiscoveryIntern(context);

        isRunning = false;
    }
}