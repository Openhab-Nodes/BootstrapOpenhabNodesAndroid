package org.libbootstrapiotdevice.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An instance of this class listens to the UDP port PORT_RECEIVE and will call
 * the inheriting
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
    private Context context;
    private int receivePort;
    private WifiManager.MulticastLock multicastLock;
    private UDPMulticastSendReceiveThread thread = null;
    ///// Sending /////
    private SendThread sendThread;
    private LinkedBlockingQueue<SendEntry> sendQueue = new LinkedBlockingQueue<>();
    private DatagramPacket sendPacket = new DatagramPacket(new byte[1], 1);
    private IUDPNetworkReceive receiver;

    @Override
    public boolean send(int sendPort, InetAddress address, byte[] data) {
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

    public void start(Context context, int receivePort, IUDPNetworkReceive receiver) {
        this.context = context;
        this.receiver = receiver;
        if (multicastLock == null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            multicastLock = wifiManager.createMulticastLock("bootstrapCommunication");
            multicastLock.acquire();
        }

        this.receivePort = receivePort;

        shutdownThread = false;
        closeSocket = false;

        if (thread == null) {
            thread = new UDPMulticastSendReceiveThread("UDPMulticastSendReceive");
            thread.start();
        }

        if (sendThread == null) {
            sendThread = new SendThread();
            sendThread.start();
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
        @Override
        public void run() {
            while (!shutdownThread) {
                try {
                    SendEntry entry = sendQueue.take();
                    sendPacket.setPort(entry.sendPort);
                    sendPacket.setAddress(entry.address);
                    sendPacket.setData(entry.data, 0, entry.data.length);
                    Log.w(TAG, "send data " + String.valueOf(sendPacket.getLength()) + " " + String.valueOf(sendPacket.getPort()) + " " + sendPacket.getAddress().toString());
                    socket.send(sendPacket);
                } catch (InterruptedException | IOException e) {
                    if (!shutdownThread)
                        e.printStackTrace();
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
                if (socket == null) {
                    try {
                        socket = new MulticastSocket(null);
                        socket.setReuseAddress(true);
                        socket.setBroadcast(true);
                        socket.setLoopbackMode(true);
                        socket.bind(new InetSocketAddress(receivePort));

                        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            Network n[] = cm.getAllNetworks();
                            for (Network network : n) {
                                NetworkInfo networkInfo = cm.getNetworkInfo(network);
                                if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                                    network.bindSocket(socket);
                                    break;
                                }
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cm.getBoundNetworkForProcess().bindSocket(socket);
                        }

                        if (sendPacket.getAddress() != null && sendPacket.getAddress().isMulticastAddress())
                            socket.joinGroup(sendPacket.getAddress());
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                try {
                    closeSocket = false;
                    while (!closeSocket) {
                        Log.w(TAG, "receive ready");
                        multicastLock.acquire();
                        socket.receive(packet);
                        Log.w(TAG, "socket receive");
                        InetSocketAddress address = (InetSocketAddress) packet.getSocketAddress();
                        receiver.parsePacket(buffer, packet.getLength(), address);
                        // Reset the length of the packet before reusing it.
                        packet.setLength(buffer.length);
                    }
                } catch (IOException e) {
                    socket = null;
                    Log.w(TAG, "socket release");
                    if (!closeSocket) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
            }
        }
    }
}