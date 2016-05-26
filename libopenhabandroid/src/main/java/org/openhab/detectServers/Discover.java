package org.openhab.detectServers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import de.duenndns.ssl.MemorizingTrustManager;

public class Discover {
    public static final String SERVICE_HTTP = "_openhab-server._tcp.";
    public static final String SERVICE_HTTPS = "_openhab-server-ssl._tcp.";
    public static final String TAG = "Discover";
    private static final int MSG_DISCOVER_SUCCESS = 0;
    private static final int MSG_DISCOVER_STOPPED = 1;
    private static final int MSG_DISCOVER_START = 3;
    private static final int MSG_STARTED_DISCOVERY = 4;
    private final NsdManager mNsdManager;
    private final NsdManager.DiscoveryListener mDiscoveryListenerHttp = new NsDDiscoveryListener();
    private final NsdManager.DiscoveryListener mDiscoveryListenerHttps = new NsDDiscoveryListener();
    private final Semaphore resolveLock = new Semaphore(1, true);
    Set<DiscoverListener> observers = Collections.newSetFromMap(
            new WeakHashMap<DiscoverListener, Boolean>());
    Thread thread = null;
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISCOVER_SUCCESS:
                    for (DiscoverListener observer : observers) {
                        observer.onOpenHABDiscovered((OpenHABServer) msg.obj);
                        observer.onOpenHABDiscoveryFinished(DiscoverListener.FinishedState.Success);
                    }
                    break;
                case MSG_DISCOVER_STOPPED:
                    for (DiscoverListener observer : observers)
                        observer.onOpenHABDiscoveryFinished(DiscoverListener.FinishedState.Stopped);
                    break;
                case MSG_DISCOVER_START:
                    Context context = (Context)msg.obj;
                    discoverNow(context);
                    break;
                case MSG_STARTED_DISCOVERY:
                    for (DiscoverListener observer : observers)
                        observer.onOpenHABDiscoveryStarted();
                    break;
            }
        }
    };
    private ArrayBlockingQueue<ResolveListener> resolveQueue = new ArrayBlockingQueue<>(10);
    private MemorizingTrustManager memorizingTrustManager;
    private boolean isRunning = false;
    private WifiReceiver wifiReceiver = new WifiReceiver();

    public Discover(NsdManager mNsdManager) {
        this.mNsdManager = mNsdManager;
        if (mNsdManager == null)
            throw new RuntimeException("NSD_SERVICE not found");
    }

    public void addObserver(DiscoverListener observer) {
        observers.add(observer);
    }

    public void clearObservers() {
        observers.clear();
    }

    public synchronized void discoverNow(Context context) {
        stopDiscoveryIntern();
        try {
            mNsdManager.discoverServices(
                    SERVICE_HTTP, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListenerHttp);
            mNsdManager.discoverServices(
                    SERVICE_HTTPS, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListenerHttps);
        } catch (IllegalArgumentException ignored) {
        }

        handler.sendMessageDelayed(handler.obtainMessage(MSG_DISCOVER_START, context), 20000);
        handler.sendEmptyMessageDelayed(MSG_DISCOVER_STOPPED, 6000);
    }

    public void startDiscovery(Context context, MemorizingTrustManager memorizingTrustManager) {
        this.memorizingTrustManager = memorizingTrustManager;
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

        handler.sendMessage(handler.obtainMessage(MSG_DISCOVER_START, context));
    }

    private void stopDiscoveryIntern() {
        handler.removeMessages(MSG_DISCOVER_START);
        handler.removeMessages(MSG_DISCOVER_STOPPED);
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListenerHttp);
            mNsdManager.stopServiceDiscovery(mDiscoveryListenerHttps);
        } catch (IllegalArgumentException ignored) {  }

    }

    public void stopDiscovery(Context context) {
        this.memorizingTrustManager = null;
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

        stopDiscoveryIntern();

        isRunning = false;
    }

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

            OpenHABServer server = new OpenHABServer(serviceInfo.getHost().getHostAddress(),
                    serviceInfo.getPort(), secure);
            server.setMemorizingTrustManager(memorizingTrustManager);
            handler.sendMessage(handler.obtainMessage(MSG_DISCOVER_SUCCESS, server));

            resolveLock.release();
        }
    }

    private class NsDDiscoveryListener implements NsdManager.DiscoveryListener {

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
                } else if (service.getServiceType().equals(SERVICE_HTTPS)) {
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
            //Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.i(TAG, "Discovery failed: Error code:" + errorCode);
        }
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null)
                return;

            if (info.getSupplicantState() == SupplicantState.COMPLETED && info.getBSSID() != null && info.getIpAddress() != 0) {
                discoverNow(context);
            }
        }
    }
}