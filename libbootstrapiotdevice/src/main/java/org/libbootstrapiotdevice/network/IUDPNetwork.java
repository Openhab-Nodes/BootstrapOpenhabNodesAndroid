package org.libbootstrapiotdevice.network;

import java.net.InetAddress;

/**
 * Interface to describe network actions. Used by {@see org.libbootstrapiotdevice.network.BootstrapCore}.
 */
public interface IUDPNetwork {
    /**
     * Send given data to the destination you have set in {@see setDestination}.
     *
     * @param data The data to send.
     * @return Return true if a valid socket is opened and if a destionation
     * has been set up before.
     */
    boolean send(int sendPort, InetAddress address, byte[] data);

    /**
     * @return Return true if a socket is opened and ready to send.
     */
    boolean isValid();
}
