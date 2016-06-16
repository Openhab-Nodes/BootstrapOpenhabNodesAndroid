package org.libbootstrapiotdevice.network;

import android.content.Context;

import org.libbootstrapiotdevice.R;

/**
 * Whenever a device responses, it is in one of the given states.
 */
public enum DeviceState {
    STATE_OK,
    STATE_HELLO,
    STATE_BOOTSTRAP_OK,

    STATE_ERROR_UNSPECIFIED,
    STATE_ERROR_BINDING,
    STATE_ERROR_BOOTSTRAP_DATA,
    STATE_ERROR_WIFI_LIST,
    STATE_ERROR_WIFI_NOT_FOUND,
    STATE_ERROR_WIFI_CREDENTIALS_WRONG,
    STATE_ERROR_ADVANCED;

    public static String toString(Context c, DeviceState state) {
        switch (state) {
            case STATE_OK:
                return null;
            case STATE_HELLO:
                return c.getString(R.string.device_wait_for_data);
            case STATE_BOOTSTRAP_OK:
                return c.getString(R.string.bs_device_state_bs);
            case STATE_ERROR_BINDING:
                return c.getString(R.string.error_binding_failed);
            case STATE_ERROR_BOOTSTRAP_DATA:
                return c.getString(R.string.error_bootstrap_data_invalid);
            case STATE_ERROR_WIFI_LIST:
                return c.getString(R.string.error_no_wifi_list_available);
            case STATE_ERROR_WIFI_NOT_FOUND:
                return c.getString(R.string.error_wifi_not_found);
            case STATE_ERROR_WIFI_CREDENTIALS_WRONG:
                return c.getString(R.string.error_credentials_wrong);
            case STATE_ERROR_ADVANCED:
                return c.getString(R.string.error_additional_data);
            case STATE_ERROR_UNSPECIFIED:
            default:
                return c.getString(R.string.error_unknown);
        }
    }
}
