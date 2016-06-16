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
    public final InetAddress address;
    public String uid;
    public String device_name;
    private byte[] device_nonce;
    private byte[] crypto_key;
    private int crypto_key_len;
    private List<WirelessNetwork> reachableNetworks = new ArrayList<>();
    private DeviceMode mode;
    private DeviceState state;
    private int external_confirmation_state;
    private SpritzState crypto = new SpritzState();
    private boolean selected = true;
    private WirelessNetwork wirelessNetwork = null;
    private String errorMessage = "";
    private long lastSeen = 0;

    /**
     * Links an address to a device, every other field is invalid.
     *
     * @param address An IP address
     */
    public BootstrapDevice(InetAddress address) {
        this.uid = "";
        this.device_name = "";
        this.address = address;
        this.state = DeviceState.STATE_ERROR_UNSPECIFIED;
        this.mode = DeviceMode.Unbound;
        updateLastSeen();
    }

    public void setName(String device_name) {
        this.device_name = device_name;
    }

    public void updateState(String uid,
                            @NonNull DeviceMode mode,
                            @NonNull DeviceState state,
                            List<WirelessNetwork> reachableNetworks,
                            @NonNull byte[] device_nonce, @NonNull byte[] crypto_key, int crypto_key_len,
                            int external_confirmation_state) {
        this.uid = uid;
        this.mode = mode;
        this.state = state;
        this.external_confirmation_state = external_confirmation_state;
        if (reachableNetworks != null)
            this.reachableNetworks = reachableNetworks;
        this.device_nonce = device_nonce;
        this.crypto_key = crypto_key;
        this.crypto_key_len = crypto_key_len;
        updateLastSeen();
    }

    public boolean isAlreadyBound() {
        return mode == DeviceMode.ErrorDeviceAlreadyBound;
    }

    public boolean isValid() {
        return uid.length()>0;
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

    public int getExternalConfirmationState() {
        return external_confirmation_state;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
