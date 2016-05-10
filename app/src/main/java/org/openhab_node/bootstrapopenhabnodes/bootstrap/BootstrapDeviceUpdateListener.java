package org.openhab_node.bootstrapopenhabnodes.bootstrap;

import java.util.List;

/**
 * Created by david on 25.04.16.
 */
public interface BootstrapDeviceUpdateListener {
    void updateFinished(List<WirelessNetwork> entries);
    void updateStarted();
}
