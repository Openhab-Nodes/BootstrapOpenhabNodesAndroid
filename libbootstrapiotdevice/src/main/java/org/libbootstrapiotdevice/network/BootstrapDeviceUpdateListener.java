package org.libbootstrapiotdevice.network;

/**
 * Get notified of device update events.
 */
public interface BootstrapDeviceUpdateListener {
    void deviceUpdated(int index, boolean added);

    void deviceRemoved(int index);

    void deviceRemoveAll();

    void deviceChangesFinished();
}
