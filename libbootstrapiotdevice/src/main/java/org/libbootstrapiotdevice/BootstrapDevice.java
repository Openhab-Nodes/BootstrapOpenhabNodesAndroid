package org.libbootstrapiotdevice;

import android.support.annotation.NonNull;

import org.libbootstrapiotdevice.network.DeviceState;
import org.libbootstrapiotdevice.network.spritzJ.SpritzState;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This class describes a bootstrap device.
 */
public class BootstrapDevice implements Comparable<BootstrapDevice> {
    public final String uid;
    public final String device_name;
    public final InetAddress address;
    private String errorString = "";
    private byte[] device_nonce;
    private byte[] crypto_key;
    private int crypto_key_len;
    private List<WirelessNetwork> reachableNetworks = new ArrayList<>();
    private DeviceMode mode;
    private DeviceState state;
    private SpritzState crypto = new SpritzState();
    private boolean selected = true;
    private WirelessNetwork wirelessNetwork = null;

    public BootstrapDevice(String uid, String device_name, InetAddress address) {
        this.uid = uid;
        this.device_name = device_name;
        this.address = address;
    }

    public void updateState(@NonNull DeviceMode mode,
                            @NonNull DeviceState state,
                            List<WirelessNetwork> reachableNetworks,
                            @NonNull String errorString,
                            @NonNull byte[] device_nonce, @NonNull byte[] crypto_key, int crypto_key_len) {
        this.mode = mode;
        this.state = state;
        if (reachableNetworks != null)
            this.reachableNetworks = reachableNetworks;
        this.errorString = errorString;
        this.device_nonce = device_nonce;
        this.crypto_key = crypto_key;
        this.crypto_key_len = crypto_key_len;
    }

    public boolean isValid() {
        return mode != DeviceMode.ErrorDeviceAlreadyBound;
    }

    @Override
    public int compareTo(@NonNull BootstrapDevice bootstrapDevice) {
        return equals(bootstrapDevice) ? 0 : 1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BootstrapDevice)) return false;
        BootstrapDevice other = (BootstrapDevice) o;
        return other.uid.equals(uid);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void cipherEncrypt(byte[] in_out, int offset) {
        crypto.cipherInit(crypto_key, 0, crypto_key_len, device_nonce, 0, device_nonce.length);
        crypto.cipherEncrypt(in_out, offset, in_out.length - offset, in_out, offset);
    }

    public void cipherDecrypt(byte[] in_out, int offset) {
        crypto.cipherInit(crypto_key, 0, crypto_key_len, device_nonce, 0, device_nonce.length);
        crypto.cipherDecrypt(in_out, offset, in_out.length - offset, in_out, offset);
    }

    public DeviceMode getMode() {
        return mode;
    }

    public void setMode(DeviceMode mode) {
        this.mode = mode;
    }

    public DeviceState getState() {
        return state;
    }

    public List<WirelessNetwork> getReachableNetworks() {
        return reachableNetworks;
    }

    public WirelessNetwork getWirelessNetwork() {
        return wirelessNetwork;
    }

    public void setWirelessNetwork(WirelessNetwork wirelessNetwork) {
        this.wirelessNetwork = wirelessNetwork;
    }
}
