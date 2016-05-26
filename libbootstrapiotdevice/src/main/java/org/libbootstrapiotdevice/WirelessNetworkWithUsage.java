package org.libbootstrapiotdevice;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extends the WirelessNetwork class by a computed average signal strength. Bootstrap devices
 * can be added at any time.
 */
public class WirelessNetworkWithUsage extends WirelessNetwork {
    private Map<BootstrapDevice, Integer> devices_with_signal_strength = new TreeMap<>();

    WirelessNetworkWithUsage(WirelessNetwork original, BootstrapDevice user) {
        super(original);
        devices_with_signal_strength.put(user, original.getStrength());
    }

    // Compute average
    @Override
    public int getStrength() {
        Iterator<Map.Entry<BootstrapDevice, Integer>> it = devices_with_signal_strength.entrySet().iterator();
        int total = 0;
        while (it.hasNext()) {
            total += it.next().getValue();
        }
        return total / devices_with_signal_strength.size();
    }

    public int deviceCount() {
        return devices_with_signal_strength.size();
    }

    public void addDevice(BootstrapDevice device, int strength) {
        devices_with_signal_strength.put(device, strength);
    }
}
