package org.libBSTbootstrap;

import android.content.Context;
import android.support.annotation.NonNull;

import org.openhab.bootstrap.R;

import java.util.List;

/**
 * This class describes a bootstrap device.
 */
public class BootstrapDevice extends WirelessNetwork implements Comparable<BootstrapDevice> {
    public final String uid;
    public final String device_name;
    String errorString = "";
    public List<WirelessNetwork> reachableNetworks;
    public enum DeviceState {
        Detected,
        SelectedForBootstrap,

        Connect,
        ConnectionError,
        ErrorDeviceAlreadyBound,

        WaitForWelcome,
        Binding,         // Bind bs node to this app by supplying a new ap password

        BindingSuccess,  // if Connect succeed
        BindingError,

        DeviceReady,
        WaitForNetworks,

        Bootstrapping,
        BootstrappingError,

        BootstrappingSuccess, NotInRange
    }
    public DeviceState state = DeviceState.Detected;

    public BootstrapDevice(String uid, String device_name, String ssid, int strength, boolean is_bound) {
        this.uid = uid;
        this.device_name = device_name;
        this.ssid = ssid;
        this.strength = strength;
        this.is_bound = is_bound;
    }

    static public String UidFromSSID(String ssid) {
        int i = ssid.lastIndexOf('_');
        if (i == -1) return null;
        return ssid.substring(i+1);
    }

    public String getStateText(Context c) {
        switch (state) {
            case Detected:
                return c.getString(R.string.bs_device_state_detected);
            case SelectedForBootstrap:
                return c.getString(R.string.bs_device_state_selected);
            case Connect:
                return c.getString(R.string.bs_device_state_connect_specific);
            case ConnectionError:
                return c.getString(R.string.bs_device_state_connection_error);
            case ErrorDeviceAlreadyBound:
                return c.getString(R.string.bs_device_state_connection_error_specific);
            case WaitForWelcome:
                return c.getString(R.string.bs_device_state_wait_for_welcome);
            case Binding:
                return c.getString(R.string.bs_device_state_binding);
            case BindingSuccess:
                return c.getString(R.string.bs_device_state_binding_success);
            case BindingError:
                return c.getString(R.string.bs_device_state_binding_error, errorString);
            case DeviceReady:
                return c.getString(R.string.bs_device_state_ready);
            case WaitForNetworks:
                return c.getString(R.string.bs_device_state_wait_for_networks);
            case Bootstrapping:
                return c.getString(R.string.bs_device_state_bs);
            case BootstrappingError:
                return c.getString(R.string.bs_device_state_bs_failed, errorString);
            case BootstrappingSuccess:
                return c.getString(R.string.bs_device_state_bootstrap_success);
            case NotInRange:
                return c.getString(R.string.bs_device_state_not_in_range);
            default:
                return "";
        }
    }

    public boolean isSelected() {
        return state == DeviceState.SelectedForBootstrap;
    }

    public void setSelected(boolean selected) {
        state = selected ? DeviceState.SelectedForBootstrap : DeviceState.Detected;
    }

    @Override
    public int compareTo(@NonNull BootstrapDevice bootstrapDevice) {
        return equals(bootstrapDevice)?0:1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BootstrapDevice)) return false;
        BootstrapDevice other = (BootstrapDevice)o;
        return other.uid.equals(uid) && other.reachableNetworks.equals(reachableNetworks);
    }
}
