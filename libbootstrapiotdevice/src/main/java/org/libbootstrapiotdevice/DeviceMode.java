package org.libbootstrapiotdevice;

import android.content.Context;

/**
 * Device firmware mode
 */
public enum DeviceMode {
    ErrorDeviceAlreadyBound,
    Unbound,
    Binding,
    BindingError,
    Bound,

    BootstrappingDone,
    BootstrappingError,

    NotInRange;

    public static String toString(Context c, DeviceMode mode, String customErrorText) {
        switch (mode) {
            case ErrorDeviceAlreadyBound:
                return c.getString(R.string.bs_device_state_connection_error_specific);
            case Bound:
                return c.getString(R.string.bs_device_state_bound);
            case Binding:
                return c.getString(R.string.bs_device_state_binding);
            case BindingError:
                return c.getString(R.string.bs_device_state_binding_error, customErrorText);
            case Unbound:
                return c.getString(R.string.bs_device_state_unbound);
            case BootstrappingDone:
                return c.getString(R.string.bs_device_state_bs);
            case BootstrappingError:
                return c.getString(R.string.bs_device_state_bs_failed, customErrorText);
            case NotInRange:
                return c.getString(R.string.bs_device_state_not_in_range);
            default:
                return "";
        }
    }
}
