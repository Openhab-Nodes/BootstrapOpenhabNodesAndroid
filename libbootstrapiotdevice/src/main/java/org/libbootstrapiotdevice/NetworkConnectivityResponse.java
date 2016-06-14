package org.libbootstrapiotdevice;

import android.os.Handler;

/**
 * To be able to delay a callback with a ssid information (e.g. to check
 * if a wifi connection / access point establishing works).
 * Also used by {@see org.libbootstrapiotdevice.network.WifiChangedObserverAndroid4} and
 * {@see org.libbootstrapiotdevice.network.WifiChangedObserverAndroid5}.
 */
public class NetworkConnectivityResponse {
    Callback callback;
    String ssid;
    Handler handler;
    boolean isDone = false;
    int what;

    public NetworkConnectivityResponse(Callback callback, Handler handler, int what,
                                       String ssid) {
        this.callback = callback;
        this.handler = handler;
        this.what = what;
        this.ssid = ssid;
    }

    public void notifyWifiChanged(String ssid) {
        handler.sendMessage(handler.obtainMessage(what, this));
    }

    public interface Callback {
        void wifiSuccess(boolean success);
    }
}
