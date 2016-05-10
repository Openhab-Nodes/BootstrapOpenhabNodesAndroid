package org.openhab_node.bootstrapopenhabnodes.bootstrap.network;

import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDevice;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.WirelessNetwork;

import java.net.InetAddress;
import java.util.List;

/**
 * API to communicate with devices which use the bootstrap library.
 */
public interface BootstrapNetworkReceiveAPI {
    void device_wifiList(BootstrapDevice device, List<WirelessNetwork> networkList);
    void device_lastError(BootstrapDevice device, int errorCode, String log);
    void device_bindingAccepted(BootstrapDevice device);
    void device_dataAccepted(BootstrapDevice device);
    void device_welcomeMessage(BootstrapDevice device, InetAddress address, int newSessionID);
}
