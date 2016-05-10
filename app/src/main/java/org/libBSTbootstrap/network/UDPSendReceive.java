package org.libBSTbootstrap.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

/**
 * A singleton UDP Receiver class. SimpleudpBridgeHandler objects can register and unregister.
 * If the first one is registered and there is no singleton instance, an instance will be created and the
 * receiver thread will be started. If the last SimpleudpBridgeHandler is removed, the thread will be stopped
 * after the receive socket is closed. This instance listens to the UDP port PORT_RECEIVE and will call
 * SimpleudpBridgeHandler.parsePacket(...) of the peer address matching SimpleudpBridgeHandler object.
 *
 * @author David Graeff <david.graeff@web.de>
 */
public abstract class UDPSendReceive extends Thread {
    private final Context context;
    private final int receivePort;
    private final WifiManager.MulticastLock multicastLock;
    protected byte[] buffer = new byte[1024];
    protected MulticastSocket socket;
    protected DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    protected InetAddress multicastGroup;
    public boolean shutdownThread = false;
    public boolean closeSocket = false;
    private static String TAG = "UDPRECEIVE";
    private Semaphore semSocketChange = new Semaphore(1);
    private Semaphore semSocketReady = new Semaphore(1);

    protected ByteArrayOutputStream sendStream = new ByteArrayOutputStream( 1024 );
    private DatagramPacket sendPacket = new DatagramPacket(new byte[1], 1);

    public boolean isSendingAllowed() {
        return sendPacket.getAddress() != null;
    }

    protected void setDestination(int sendPort, InetAddress address) {
        sendPacket.setPort(sendPort);
        sendPacket.setAddress(address);
    }

    public boolean send() {
        sendPacket.setData(sendStream.toByteArray(), 0, sendStream.size());
        Log.w(TAG, "send data "+String.valueOf(sendPacket.getLength())+" "+String.valueOf(sendPacket.getPort())+" "+sendPacket.getAddress().toString());
        try {
            socket.send(sendPacket);
            return true;
        } catch (SocketException ignored) {
            // ENETUNREACH
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

//    private Semaphore semReceive = new Semaphore(1);
//    static class MessageHandler extends android.os.Handler {
//        private final UDPSendReceive udpReceive;
//
//        MessageHandler(UDPSendReceive udpReceive) {
//            super(Looper.getMainLooper());
//            this.udpReceive = udpReceive;
//        }
//        @Override
//        public void handleMessage(Message msg) {
//            try {
//                InetSocketAddress address = (InetSocketAddress) udpReceive.packet.getSocketAddress();
//                udpReceive.parsePacket(udpReceive.buffer, udpReceive.packet.getLength(), address);
//                // Reset the length of the packet before reusing it.
//                udpReceive.packet.setLength(udpReceive.buffer.length);
//            } finally {
//                udpReceive.semReceive.release();
//            }
//        }
//    }
//
//    MessageHandler messageHandler = new MessageHandler(this);

    public void tearDown() {
        multicastLock.release();

        if (isAlive()) {
            shutdownThread = true;
            closeSocket = true;
            if (socket != null) {
                socket.close();
            }
            try {
                join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            interrupt();
            socket = null;
        }
    }

    public UDPSendReceive(Context context, int receivePort)  {
        super("UDPReceive");
        this.context = context;
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("bootstrapCommunication");
        multicastLock.acquire();
        this.receivePort = receivePort;
        try {
            semSocketChange.acquire();
            multicastGroup = InetAddress.getByName("239.0.0.57");
        } catch (InterruptedException | UnknownHostException ignored) {
        }
        start();
    }

    public void recreateSocket() {
        try {
            semSocketReady.tryAcquire();
            closeListenerSocket();
            sleep(200);
            semSocketChange.release();
            semSocketReady.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void closeListenerSocket() {
        if (socket != null) {
            closeSocket = true;
            socket.close();
        }
    }

    public boolean isSocketOpen() {
        return socket != null;
    }

    /**
     * An incoming packet has been received. This method is called in the main thread context.
     * @param message The message
     * @param length Message length
     * @param peer The peer address info
     */
    abstract protected void parsePacket(byte[] message, int length, InetSocketAddress peer);

    @Override
    public void run() {
        shutdownThread = false;
        while(!shutdownThread) {
            try {
                semSocketChange.acquire();
            } catch (InterruptedException ignored) {
            }

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
                        for (Network network: n) {
                            NetworkInfo networkInfo = cm.getNetworkInfo(network);
                            if (networkInfo.isConnected() && networkInfo.getType()==ConnectivityManager.TYPE_WIFI) {
                                network.bindSocket(socket);
                                break;
                            }
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cm.getBoundNetworkForProcess().bindSocket(socket);
                    }

                    socket.joinGroup(multicastGroup);
                } catch (IOException e) {
                    e.printStackTrace();
                    semSocketReady.release();
                    continue;
                }
            }

            try {
                // Now loop forever, waiting to receive packets and printing them.
                closeSocket = false;
                semSocketReady.release();
                while (!closeSocket) {
                    Log.w(TAG, "receive ready");
                    multicastLock.acquire();
                    socket.receive(packet);
                    Log.w(TAG, "socket receive");

                    //semReceive.acquire();
                    //messageHandler.sendEmptyMessage(0);
                    InetSocketAddress address = (InetSocketAddress) packet.getSocketAddress();
                    parsePacket(buffer, packet.getLength(), address);
                    // Reset the length of the packet before reusing it.
                    packet.setLength(buffer.length);
                }
            } catch (IOException e) {
                socket = null;
                semSocketReady.release();
                Log.w(TAG, "socket release");
                if (!closeSocket) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        }
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}