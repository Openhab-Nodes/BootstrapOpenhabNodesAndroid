package org.openhab_node.bootstrapopenhabnodes.bootstrap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lists all bootstrap devices in range. Shows the name and wifi signal strengths and a checkbox
 * per device. To be filled with the helper class {@see org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDevicesViaWifiScan}
 */
public class BootstrapDeviceList {
    public List<BootstrapDevice> devices = new ArrayList<>();

    public List<Integer> updateFinished(List<WirelessNetwork> entries) {
        List<Integer> changes = new ArrayList<>();

        if (entries.isEmpty())
            return changes;

        for (int i=devices.size()-1; i>=0 ; --i) {
            boolean found = false;
            for (Iterator<WirelessNetwork> iterator = entries.iterator(); iterator.hasNext(); ) {
                WirelessNetwork entry = iterator.next();
                if (devices.get(i).uid.equals(BootstrapDevice.UidFromSSID(entry.ssid))) {
                    iterator.remove();
                    found = true;

                    // Changed
                    if (devices.get(i).state != BootstrapDevice.DeviceState.SelectedForBootstrap)
                        devices.get(i).state = BootstrapDevice.DeviceState.Detected;
                    devices.get(i).strength = entry.strength;
                    devices.get(i).is_bound = entry.is_bound;
                    changes.add(i);

                }
            }

            if (!found) {
                devices.get(i).state = BootstrapDevice.DeviceState.NotInRange;
                devices.get(i).strength = 0;
                changes.add(i);
            }
        }

        for (WirelessNetwork entry: entries) {
            int i = entry.ssid.lastIndexOf('_');
            String uid = entry.ssid.substring(i+1);
            String device_name = entry.ssid.substring(entry.ssid.indexOf('_')+1,i);

            // Insert
            devices.add(new BootstrapDevice(uid, device_name, entry.ssid,entry.strength, entry.is_bound));
        }

        return changes;
    }

    public boolean containsUnbound() {
        for(BootstrapDevice device: devices)
            if (!device.is_bound)
                return true;
        return false;
    }

    public void clear() {
        devices.clear();
    }
}
