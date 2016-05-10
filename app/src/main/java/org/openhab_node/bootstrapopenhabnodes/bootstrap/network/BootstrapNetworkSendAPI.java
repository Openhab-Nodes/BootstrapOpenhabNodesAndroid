package org.openhab_node.bootstrapopenhabnodes.bootstrap.network;

import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapData;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDevice;

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
