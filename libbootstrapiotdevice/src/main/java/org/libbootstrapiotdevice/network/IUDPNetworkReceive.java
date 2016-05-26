package org.libbootstrapiotdevice.network;

import java.net.InetSocketAddress;

/**
 * Interface to describe network receiving. Used by {@see org.libbootstrapiotdevice.network.BootstrapDevices}.
 */
public interface IUDPNetworkReceive {
    /**
     * An incoming packet has been received. This method is called in the main thread context.
     *
     * @param message The message
     * @param length  Message length
     * @param peer    The peer address info
     */
    void parsePacket(byte[] message, int length, InetSocketAddress peer);
}
