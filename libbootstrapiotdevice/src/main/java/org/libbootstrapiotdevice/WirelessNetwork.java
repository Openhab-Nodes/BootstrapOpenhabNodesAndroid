package org.libbootstrapiotdevice;

/**
 * This class describes a wireless network of a bootstrap node.
 */
public class WirelessNetwork {
    public String ssid;
    public String pwd;
    public int strength;
    public EncryptionMode mode;
    public boolean is_bound = false;

    public WirelessNetwork() {

    }

    protected WirelessNetwork(WirelessNetwork other) {
        this.ssid = other.ssid;
        this.pwd = other.pwd;
        this.strength = other.strength;
        this.mode = other.mode;
    }

    public int getStrength() {
        return strength;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WirelessNetwork)) return false;
        WirelessNetwork other = (WirelessNetwork)o;
        return other.ssid.equals(ssid);
    }

    public enum EncryptionMode {
        NoEncryption,
        WEP,
        WPA,
        Unknown
    }
}
