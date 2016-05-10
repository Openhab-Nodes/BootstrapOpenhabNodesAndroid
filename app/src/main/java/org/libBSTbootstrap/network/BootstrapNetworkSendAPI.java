package org.libBSTbootstrap.network;

import org.libBSTbootstrap.BootstrapData;
import org.libBSTbootstrap.BootstrapDevice;

/**
 * API to communicate with devices which use the bootstrap library.
 */
public interface BootstrapNetworkSendAPI {
    void useDevice(BootstrapDevice device);

    boolean sendHello();
    boolean bindToDevice(String new_secret);
    boolean sendBootstrapData(BootstrapData data);
    boolean factoryReset();
    boolean requestWifiList();
    boolean requestLastError();
}
