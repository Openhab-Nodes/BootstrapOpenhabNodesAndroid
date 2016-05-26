package org.libbootstrapiotdevice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Allows to add BootstrapDevices and computes a list of all their wireless networks and the
 * overlapping.
 */
public class OverlappingNetworks {
    public List<WirelessNetworkWithUsage> networks = new ArrayList<>();
    public Set<BootstrapDevice> devices = new TreeSet<>();

    public List<Integer> addDevice(BootstrapDevice device) {
        List<Integer> changes = new ArrayList<>();

        if (devices.contains(device))
            return changes;

        devices.add(device);

        List<WirelessNetwork> reachableNetworks = new ArrayList<>(device.getReachableNetworks());

        for (int i = networks.size()-1; i>=0 ; --i) {
            WirelessNetworkWithUsage network = networks.get(i);
            Iterator<WirelessNetwork> deviceNetworkIterator = reachableNetworks.iterator();
            while (deviceNetworkIterator.hasNext()) {
                WirelessNetwork deviceNetwork = deviceNetworkIterator.next();
                if (deviceNetwork.equals(network)) {
                    deviceNetworkIterator.remove();
                    network.addDevice(device, deviceNetwork.getStrength());
                    changes.add(i);
                    break;
                }
            }
        }

        for (WirelessNetwork entry: reachableNetworks) {
            networks.add(new WirelessNetworkWithUsage(entry, device));
        }
        return changes;
    }
}
