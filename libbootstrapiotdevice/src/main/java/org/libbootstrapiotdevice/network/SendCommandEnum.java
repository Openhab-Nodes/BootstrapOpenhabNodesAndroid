package org.libbootstrapiotdevice.network;

/**
 * Whenever you send a packet to a device, it must be one of the following
 * commands.
 */
enum SendCommandEnum {
    CMD_UNKNOWN,
    CMD_HELLO,
    CMD_SET_DATA,
    CMD_BIND
}
