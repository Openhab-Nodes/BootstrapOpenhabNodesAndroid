package org.libbootstrapiotdevice;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * This class describes a wireless network of a bootstrap node.
 */
public class WirelessNetwork {
    public String ssid;
    public String pwd;
    public int strength;
    public enum EncryptionMode {
        NoEncryption,
        WEP,
        WPA,
        Unknown
    }
    public EncryptionMode mode;
    public Set<InetAddress> addresses = new HashSet<>();

    public boolean is_bound = false;

    public int getStrength() {
        return strength;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WirelessNetwork)) return false;
        WirelessNetwork other = (WirelessNetwork)o;
        return other.ssid.equals(ssid);
    }

    public WirelessNetwork() {

    }

    protected WirelessNetwork(WirelessNetwork other) {
        this.ssid = other.ssid;
        this.pwd = other.pwd;
        this.strength = other.strength;
        this.mode = other.mode;
    }
}
