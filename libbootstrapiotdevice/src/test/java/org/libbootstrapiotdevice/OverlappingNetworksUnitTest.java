package org.libbootstrapiotdevice;

import org.junit.Test;
import org.libbootstrapiotdevice.network.DeviceState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class OverlappingNetworksUnitTest {
    OverlappingNetworks networks = new OverlappingNetworks();
    @Test
    public void overlap_test() throws Exception {
        WirelessNetwork net1 = new WirelessNetwork();
        net1.strength = 90;
        net1.ssid = "net1";
        net1.pwd = "pwd";
        WirelessNetwork net2a = new WirelessNetwork();
        net2a.strength = 30;
        net2a.ssid = "net2";
        net2a.pwd = "pwd";
        WirelessNetwork net2b = new WirelessNetwork();
        net2b.strength = 70;
        net2b.ssid = "net2";
        net2b.pwd = "pwd";
        WirelessNetwork net3 = new WirelessNetwork();
        net3.strength = 50;
        net3.ssid = "net3";
        net3.pwd = "pwd";

        {
            BootstrapDevice testDevice1 = new BootstrapDevice("uid1", "name1", null);
            List<WirelessNetwork> networks1 = new ArrayList<>();
            networks1.add(net1);
            networks1.add(net2a);
            testDevice1.updateState(DeviceMode.Bound, DeviceState.STATE_OK, networks1, "", new byte[0], new byte[0], 0, 0);
            networks.addDevice(testDevice1);
        }

        {
            BootstrapDevice testDevice1 = new BootstrapDevice("uid2", "name2", null);
            List<WirelessNetwork> networks1 = new ArrayList<>();
            networks1.add(net2b);
            networks1.add(net3);
            testDevice1.updateState(DeviceMode.Bound, DeviceState.STATE_OK, networks1, "", new byte[0], new byte[0], 0, 0);
            networks.addDevice(testDevice1);
        }

        assertEquals(2, networks.devices.size());
        assertEquals(3, networks.networks.size());
        assertEquals(net1.ssid, networks.networks.get(0).ssid);
        assertEquals(net2a.ssid, networks.networks.get(1).ssid);
        assertEquals(net3.ssid, networks.networks.get(2).ssid);

        assertTrue(networks.networks.get(1).getStrength() >= net2a.strength);
        assertTrue(networks.networks.get(1).getStrength() <= net2b.strength);

        int avg = (net2a.strength + net2b.strength) / 2;
        int net_avg = networks.networks.get(1).getStrength();
        assertTrue(net_avg == avg);
    }
}