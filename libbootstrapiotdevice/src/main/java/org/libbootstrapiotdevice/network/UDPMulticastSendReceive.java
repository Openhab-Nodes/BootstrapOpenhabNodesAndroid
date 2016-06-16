package org.libbootstrapiotdevice.network;

import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A multicast UDP network implementation with Multicast Lock and on-the-go restartable
 * network socket after a wifi change for example. Includes a second sending thread with
 * a send queue and filters broadcast/multicast messages in the receive part which were
 * send by us. Implement IUDPNetworkReceive and call start() with your implementation
 * to receive data. Call send() to send data.
 *
 * @author David Graeff <david.graeff@web.de>
 */
public class UDPMulticastSendReceive implements IUDPNetwork {
    private static String TAG = "UDPRECEIVE";
    public boolean shutdownThread = false;
    public boolean closeSocket = false;
    protected byte[] buffer = new byte[1024];
    protected MulticastSocket socket;
    protected DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    private int receivePort;
    private WifiManager.MulticastLock multicastLock;
    private UDPMulticastSendReceiveThread thread = null;
    ///// Sending /////
    private SendThread sendThread;
    private LinkedBlockingQueue<SendEntry> sendQueue = new LinkedBlockingQueue<>();
    private DatagramPacket sendPacket = new DatagramPacket(new byte[1], 1);
    private IUDPNetworkReceive receiver;
    private NetworkInterface networkInterface;
    private android.net.Network network;
    private List<InetAddress> localIPAddresses = new ArrayList<>();
    private InetAddress broadcastAddress;

    /**
     * If send() is called with a null address, this broadcastAddress will be used instead.
     *
     * @param broadcastAddress The broadcast/multicast address.
     */
    public void setBroadcastAddress(InetAddress broadcastAddress) {
        this.broadcastAddress = broadcastAddress;
    }

    @Override
    public boolean send(int sendPort, @Nullable InetAddress address, byte[] data) {
        if (address == null)
            address = broadcastAddress;
        sendQueue.add(new SendEntry(data, sendPort, address));
        return false;
    }

    @Override
    public boolean isValid() {
        return socket != null;
    }

    public void tearDown() {
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }

        shutdownThread = true;
        closeSocket = true;

        if (socket != null) {
            socket.close();
            socket = null;
        }

        if (thread != null && thread.isAlive()) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread.interrupt();
            thread = null;
        }

        if (sendThread != null && sendThread.isAlive()) {
            try {
                sendThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendThread.interrupt();
            sendThread = null;
        }
    }

    public void start(@NonNull WifiManager wifiManager,
                      @Nullable Network network, @Nullable NetworkInterface networkInterface,
                      int receivePort, IUDPNetworkReceive receiver) {
        this.network = network;
        this.networkInterface = networkInterface;
        this.receiver = receiver;

        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock("bootstrapCommunication");
            multicastLock.acquire();
        }

        this.receivePort = receivePort;

        shutdownThread = false;
        closeSocket = false;

        if (networkInterface != null) {
            List<InterfaceAddress> interfaceAddressList = networkInterface.getInterfaceAddresses();
            for (InterfaceAddress interfaceAddress : interfaceAddressList) {
                InetAddress address = interfaceAddress.getAddress();
                if (address == null)
                    continue;
                localIPAddresses.add(address);
            }
        }

        if (thread == null) {
            thread = new UDPMulticastSendReceiveThread("UDPMulticastSendReceive");
            thread.start();
        } else if (socket != null) {
            sendThread.interrupt();
            sendThread = null;
            socket.close();
        }

        if (sendThread == null) {
            sendThread = new SendThread();
            sendThread.start();
        }
    }

    /**
     * @return Return true if a socket is available.
     */
    private boolean createSocket() {
        if (socket != null)
            return true;

        try {
            socket = new MulticastSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.setLoopbackMode(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(receivePort));

            if (networkInterface != null)
                socket.setNetworkInterface(networkInterface);

            if (network != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    network.bindSocket(socket);
                } catch (IOException ignored) {
                }
            }

            if (sendThread == null) {
                sendThread = new SendThread();
                sendThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void udpReceive() {
        try {
            closeSocket = false;
            while (!closeSocket) {
                Log.w(TAG, "receive ready");
                multicastLock.acquire();
                socket.receive(packet);
                InetSocketAddress remoteAddress = (InetSocketAddress) packet.getSocketAddress();
                // Don't receive packets from ourself
                for (InetAddress localAddress : localIPAddresses) {
                    if (remoteAddress.getAddress().equals(localAddress)) {
                        remoteAddress = null;
                        break;
                    }
                }
                if (remoteAddress != null) {
                    Log.w(TAG, "socket receive " + String.valueOf(packet.getLength()) + " " + String.valueOf(remoteAddress.getPort()) + " " + remoteAddress.getAddress().toString());
                    receiver.parsePacket(buffer, packet.getLength(), remoteAddress);
                    // Reset the length of the packet before reusing it.
                    packet.setLength(buffer.length);
                }
            }
        } catch (IOException e) {
            socket = null;
            Log.w(TAG, "socket release");
            if (!closeSocket) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    private class SendEntry {
        public byte[] data;
        public int sendPort;
        public InetAddress address;

        SendEntry(byte[] data, int sendPort, InetAddress address) {
            this.data = data;
            this.sendPort = sendPort;
            this.address = address;
        }
    }

    private class SendThread extends Thread {
        SendThread() {
            super("sendThread");
        }
        @Override
        public void run() {
            while (!shutdownThread) {
                try {
                    SendEntry entry = sendQueue.take();

                    sendPacket.setPort(entry.sendPort);
                    sendPacket.setData(entry.data, 0, entry.data.length);

                    // We do not use the entry.address for now. Sending to all interface broadcast addresses
                    // is more reliable, see below.
//                    sendPacket.setAddress(entry.address);
//                    Log.w(TAG, "send data " + String.valueOf(sendPacket.getLength()) + " " + String.valueOf(sendPacket.getPort()) + " " + sendPacket.getAddress().toString());
//                    socket.send(sendPacket);

                    List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                    for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                        InetAddress address = interfaceAddress.getBroadcast();
                        if (address == null)
                            continue;
                        sendPacket.setAddress(address);
                        Log.w(TAG, "send data II " + String.valueOf(sendPacket.getLength()) + " " + String.valueOf(sendPacket.getPort()) + " " + sendPacket.getAddress().toString());
                        socket.send(sendPacket);
                    }
                } catch (IOException e) {
                    if (!shutdownThread)
                        e.printStackTrace();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private class UDPMulticastSendReceiveThread extends Thread {
        public UDPMulticastSendReceiveThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            if (receiver == null)
                return;

            shutdownThread = false;
            while (!shutdownThread) {
                if (createSocket())
                    udpReceive();
            }
        }
    }
}