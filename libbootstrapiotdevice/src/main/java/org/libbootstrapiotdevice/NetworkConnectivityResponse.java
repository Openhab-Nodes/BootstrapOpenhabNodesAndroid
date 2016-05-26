package org.libbootstrapiotdevice;

import android.os.Handler;

/**
 * To be able to delay a callback with a ssid information (e.g. to check
 * if a wifi connection / access point establishing works)
 * Used by {@see org.libbootstrapiotdevice.network.WifiReceiver} and
 * {@see org.libbootstrapiotdevice.network.WifiReceiverConnectivityManager}.
 */
public class NetworkConnectivityResponse {
    BootstrapService.Callback callback;
    String ssid;
    Handler handler;
    boolean isDone = false;
    int what;

    public NetworkConnectivityResponse(BootstrapService.Callback callback, Handler handler, int what,
                                       String ssid) {
        this.callback = callback;
        this.handler = handler;
        this.what = what;
        this.ssid = ssid;
    }

    public void notifyWifiChanged(String ssid) {
        handler.sendMessage(handler.obtainMessage(what, this));
    }
}
