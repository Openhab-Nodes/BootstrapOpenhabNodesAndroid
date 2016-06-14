package org.libbootstrapiotdevice.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.libbootstrapiotdevice.BootstrapData;
import org.libbootstrapiotdevice.BootstrapDevice;
import org.libbootstrapiotdevice.DeviceMode;
import org.libbootstrapiotdevice.WirelessNetwork;
import org.libbootstrapiotdevice.network.spritzJ.SpritzState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The communication object that implements the communication protocol for devices that use a compatible
 * bootstrap firmware. No network related functionality included. This class implements
 * {@see org.libbootstrapiotdevice.network.IUDPNetworkReceive}
 * to act as a receiver for a network class and uses the interface
 * {@see org.libbootstrapiotdevice.network.IUDPNetwork}, set with setNetwork, to send
 * encrypted data packages.
 */
public class BootstrapCore implements IUDPNetworkReceive, Handler.Callback {
    public static final int RECEIVE_PORT = 8711;
    public static final int SEND_PORT = 8711;
    ///// Encryption related /////
    final static public int BST_NONCE_SIZE = 8;
    final static public int BST_UID_SIZE = 6;
    final static public int BST_CRYPTO_KEY_MAX_SIZE = 32;
    final static public int BST_CHECKSUM_SIZE = 2;
    // Async
    final static int MSG_DETECT = 1;
    final static int MSG_DETECT_FINISHED = 2;
    final static int MSG_BOOTSTRAP = 3;
    final static int MSG_BOOTSTRAP_FINISHED = 4;
    final static int MSG_BIND_OR_UPDATE = 5;
    ////// Protocol related //////
    protected static byte[] header = "BSTwifi1".getBytes();
    public static int protocol_header_len = header.length + BST_CHECKSUM_SIZE + 1;
    // Debug
    private static String TAG = "BootstrapCore";
    ////// Network related /////
    protected ByteArrayOutputStream sendStream = new ByteArrayOutputStream(1024);
    byte[] bound_key = new byte[BST_CRYPTO_KEY_MAX_SIZE];
    int bound_key_len = 0;
    // Device list
    private List<BootstrapDevice> devices = new ArrayList<>();
    private List<BootstrapDeviceUpdateListener> changeListener = new ArrayList<>();
    private IUDPNetwork network;
    private byte[] unbound_key = new byte[BST_CRYPTO_KEY_MAX_SIZE];
    private byte[] app_nonce = new byte[BST_NONCE_SIZE];
    private int unbound_key_len = 0;
    private Random random = new Random();
    private SpritzState crypto = new SpritzState();
    private String current_ssid;
    private Handler handler;

    /**
     * Creates a communication object for communicating with devices that use a compatible
     * bootstrap firmware.
     * Sets encryption keys. If devices are not bound to this app they will use a generic key, the
     * unbound_key. If they are bound to this app (via the bindToDevice() method), they
     * will use the bound_key. The unbound_key have to be pre-shared across this app and device firmwares.
     * <p/>
     * A nonce value for a new encryption session is also generated in this method. Because it makes
     * sense to create a new app nonce value each time a communication session starts, you may call
     * generateAppNonce() before using sendHello() and other traffic generating methods.
     *
     * @param overwrite_handler For tests only. Replace the message handler.
     * @param bound_key         The key that is used if the app has bound the device.
     * @param unbound_key       The key that is initially used for the app<-->device communication.
     * @param current_ssid      If the app spans an access point to let devices connect to it, provide
     */
    public BootstrapCore(@Nullable Handler overwrite_handler, byte[] bound_key, byte[] unbound_key,
                         String current_ssid) {
        this.handler = overwrite_handler != null ? overwrite_handler : new Handler(Looper.myLooper(), this);
        this.current_ssid = current_ssid;
        unbound_key_len = unbound_key.length;
        bound_key_len = bound_key.length;

        if (unbound_key_len > BST_CRYPTO_KEY_MAX_SIZE || bound_key_len > BST_CRYPTO_KEY_MAX_SIZE) {
            throw new RuntimeException("Key too long!");
        }

        System.arraycopy(unbound_key, 0, this.unbound_key, 0, unbound_key_len);
        System.arraycopy(bound_key, 0, this.bound_key, 0, bound_key_len);

        generateAppNonce();
    }

    public static byte[] extractCRC(byte data[]) {
        return Arrays.copyOfRange(data, header.length, header.length + BST_CHECKSUM_SIZE);
    }

    public static DeviceState extractState(byte data[]) {
        int code = data[header.length + BST_CHECKSUM_SIZE];
        if (code > DeviceState.values().length) {
            return null;
        }
        return DeviceState.values()[code];
    }

    public static boolean isHeaderValid(byte data[]) {
        // We expect at least the header, a crc code, and a device mode
        if (data.length < protocol_header_len) {
            return false;
        }

        for (int i = 0; i < header.length; ++i)
            if (header[i] != data[i]) {
                return false;
            }
        return true;
    }

    private void generateAppNonce() {
        for (int i = 0; i < app_nonce.length; ++i) app_nonce[i] = (byte) random.nextInt(256);
    }

    /**
     * Used for tests only. Set the app nonce value to a specific value.
     *
     * @param new_app_nonce New app nonce value.
     */
    protected void setAppNonce(byte[] new_app_nonce) {
        System.arraycopy(new_app_nonce, 0, app_nonce, 0, app_nonce.length);
    }

    /**
     * Only for testing. Swap bound and unbound key.
     */
    protected void swapBoundUnboundKeys() {
        byte[] temp = this.unbound_key;
        this.unbound_key = this.bound_key;
        this.bound_key = temp;

        int temp2 = this.unbound_key_len;
        this.unbound_key_len = this.bound_key_len;
        this.bound_key_len = temp2;
    }

    /**
     * This class only implements the protocol, not the network part. You
     * have to provide a {@see IUDPNetwork} implementation to make sending work.
     * Receiving is addressed by the fact, that this class implements the
     * {@see IUDPNetworkReceive} interface.
     *
     * @param network The network class.
     */
    public void setNetwork(IUDPNetwork network) {
        this.network = network;
    }

    /**
     * @return Return the bootstrap device list.
     */
    @SuppressWarnings("unused")
    public List<BootstrapDevice> getDevices() {
        return devices;
    }

    @SuppressWarnings("unused")
    public void removeDevicesNotSelected() {
        for (int i = devices.size() - 1; i >= 0; --i) {
            BootstrapDevice device = devices.get(i);
            if (!device.isSelected()) {
                devices.remove(i);
                for (BootstrapDeviceUpdateListener listener : changeListener) {
                    listener.deviceRemoved(i);
                }
            }
        }
    }

    /**
     * Clear all devices to restart from scratch.
     */
    @SuppressWarnings("unused")
    public void clearDevices() {
        devices.clear();
        for (BootstrapDeviceUpdateListener listener : changeListener) {
            listener.deviceRemoveAll();
        }
    }

    /**
     * Get notified of device changes.
     *
     * @param changeListener Your listener
     */
    public void addChangeListener(BootstrapDeviceUpdateListener changeListener) {
        this.changeListener.add(changeListener);
    }

    /**
     * Don't get any further notifications for device changes.
     * @param changeListener Your listener
     */
    public void removeChangeListener(BootstrapDeviceUpdateListener changeListener) {
        this.changeListener.remove(changeListener);
    }

    /**
     * Prepares the internal ByteArrayOutputStream and adds the header fields.
     */
    private void initPacket(SendCommandEnum cmd) {
        sendStream.reset();
        sendStream.write(header, 0, header.length);
        sendStream.write(0); // crc
        sendStream.write(0); // crc
        sendStream.write(cmd.ordinal()); // command
    }

    /**
     * Compute crc and insert it into the packet and encrypt it.
     *
     * @return Return true if sending has been successfully.
     */
    private boolean encryptCrcAndSend(@Nullable BootstrapDevice device) {
        byte data[] = sendStream.toByteArray();
        // skip header and checksum and command field for checksum calculation
        byte crc[] = Checksums.CheckSumAsBytes(Checksums.GenerateChecksumCRC16(data, protocol_header_len));
        System.arraycopy(crc, 0, data, header.length, BST_CHECKSUM_SIZE);

        if (device == null) {
            return network.send(SEND_PORT, null, data);
        } else {
            device.cipherEncrypt(data, protocol_header_len);
            return network.send(SEND_PORT, device.address, data);
        }
    }

    /**
     * Handle messages from other threads and delayed messages.
     *
     * @param msg The message that was send with handler.sendMessage*().
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DETECT:
                sendHello();
                break;
            case MSG_DETECT_FINISHED:
            case MSG_BOOTSTRAP_FINISHED:
                for (BootstrapDeviceUpdateListener listener : changeListener) {
                    listener.deviceChangesFinished();
                }
                break;
            case MSG_BIND_OR_UPDATE: {
                int index = msg.arg1;
                boolean added = false;
                BootstrapDevice device = (BootstrapDevice) msg.obj;
                if (index == -1) {
                    index = devices.size();
                    devices.add(device);
                    added = true;
                }

                for (BootstrapDeviceUpdateListener listener : changeListener) {
                    listener.deviceUpdated(index, added);
                }

                if (device.getMode() == DeviceMode.Binding)
                    bindToDevice(device);
                break;
            }
            case MSG_BOOTSTRAP:
                for (int i = 0; i < devices.size(); i++) {
                    BootstrapDevice device = devices.get(i);
                    if (device.getMode() == DeviceMode.Bound) {
                        device.setMode(DeviceMode.Bootstrapping);
                        for (BootstrapDeviceUpdateListener listener : changeListener) {
                            listener.deviceUpdated(msg.arg1, false);
                        }
                        bootstrapDevice(device, (BootstrapData) msg.obj);
                    }
                }
            default:
                break;
        }
        return true;
    }

    /**
     * Start detecting devices in the current network by sending HELLO
     * packets periodically. A new app nonce is generated for this session.
     * <p/>
     * Use default values for interval and attempts.
     */
    public boolean detectDevices() {
        return detectDevices(2000, 3);
    }

    /**
     * Start detecting devices in the current network by sending HELLO
     * packets periodically. A new app nonce is generated for this session.
     *
     * @param intervalMS Interval between broadcast detect packets.
     * @param attempts   How many attempts? Should be at least 1.
     */
    public boolean detectDevices(int intervalMS, int attempts) {
        if (!network.isValid()) {
            Log.e(TAG, "!network.isValid()");
            return false;
        }

        generateAppNonce();

        for (int i = 0; i < attempts; ++i)
            handler.sendEmptyMessageDelayed(MSG_DETECT, i * intervalMS);
        handler.sendEmptyMessageDelayed(MSG_DETECT_FINISHED, attempts * intervalMS);
        return true;
    }

    /**
     * Start bootstrapping devices which are listed in the device list and which
     * are in the bound mode.
     * <p/>
     * Use default values for interval and attempts and the singleton instance of BootstrapData.
     */
    public boolean bootstrapDevices() {
        return bootstrapDevices(2000, 3, BootstrapData.instance());
    }

    /**
     * Start bootstrapping devices which are listed in the device list and which
     * are in the bound mode.
     *
     * @param intervalMS Interval between bootstrap attempts.
     * @param attempts   How many attempts? Should be at least 1.
     */
    public boolean bootstrapDevices(int intervalMS, int attempts, BootstrapData data) {
        if (!network.isValid()) {
            Log.e(TAG, "!network.isValid()");
            return false;
        }

        for (int i = 0; i < attempts; ++i)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_BOOTSTRAP, data), i * intervalMS);
        handler.sendEmptyMessageDelayed(MSG_BOOTSTRAP_FINISHED, attempts * intervalMS);
        return true;
    }

    private boolean sendHello() {
        initPacket(SendCommandEnum.CMD_HELLO);
        sendStream.write(app_nonce, 0, BST_NONCE_SIZE);
        return encryptCrcAndSend(null);
    }

    private boolean bindToDevice(@NonNull BootstrapDevice device) {
        initPacket(SendCommandEnum.CMD_BIND);

        // We always write the entire length of the bound_key byte array. Everything after
        // the real key is random junk. This ensures that a network sniffer is not
        // able to determine the length of the key.
        sendStream.write(bound_key_len);
        sendStream.write(bound_key, 0, bound_key_len);
        return encryptCrcAndSend(device);
    }

    private boolean bootstrapDevice(@NonNull BootstrapDevice device, @NonNull BootstrapData data) {
        initPacket(SendCommandEnum.CMD_SET_DATA);
        data.addDataToStream(sendStream);
        return encryptCrcAndSend(device);
    }

    @Override
    public void parsePacket(byte[] message, int length, InetSocketAddress peer) {
        if (!isHeaderValid(message)) {
            Log.e(TAG, "Header not equal!");
            return;
        }

//        String str = "byte msg[] = ";
//        for(byte b: message)
//            str += String.valueOf((int)b)+",";
//        Log.w(TAG, str);
//        str = "byte app_nonce = ";
//        for(byte b: app_nonce)
//            str += String.valueOf((int)b)+",";
//        Log.w(TAG, str);
//        str = "byte unbound_key_len = ";
//        for(int i=0;i<unbound_key_len;++i)
//            str += String.valueOf((int)unbound_key[i])+",";
//        Log.w(TAG, str);

        byte crc[] = extractCRC(message);
        DeviceState state = extractState(message);

        if (state == null) {
            Log.e(TAG, "Command unknown!");
            return;
        }

        ////////// Corresponding device //////////
        // If there is a device known with this IP, find it.
        int index = -1;
        BootstrapDevice device = null;
        for (int i = 0; i < devices.size(); i++) {
            BootstrapDevice d = devices.get(i);
            if (Arrays.equals(d.address.getAddress(), peer.getAddress().getAddress())) {
                device = d;
                index = i;
                break;
            }
        }

        ////////// Decrypt and CRC //////////
        byte[] decrypted_msg = message.clone();

        boolean is_unbound = true;
        if (device != null) {
            if (device.getMode() == DeviceMode.Unbound) {
                crypto.cipherInit(unbound_key, 0, unbound_key_len, app_nonce, 0, app_nonce.length);
            } else {
                crypto.cipherInit(bound_key, 0, bound_key_len, app_nonce, 0, app_nonce.length);
                is_unbound = false;
            }
        } else {
            crypto.cipherInit(unbound_key, 0, unbound_key_len, app_nonce, 0, app_nonce.length);
        }
        crypto.cipherDecrypt(decrypted_msg, protocol_header_len, length - protocol_header_len,
                decrypted_msg, protocol_header_len);

        byte computed_crc[] = Checksums.CheckSumAsBytes(Checksums.GenerateChecksumCRC16(decrypted_msg, protocol_header_len));

        if (!Arrays.equals(computed_crc, crc)) {
            is_unbound = !is_unbound;
            if (is_unbound) {
                crypto.cipherInit(unbound_key, 0, unbound_key_len, app_nonce, 0, app_nonce.length);
            } else {
                crypto.cipherInit(bound_key, 0, bound_key_len, app_nonce, 0, app_nonce.length);
            }
            decrypted_msg = message.clone();
            crypto.cipherDecrypt(decrypted_msg, protocol_header_len, length - protocol_header_len,
                    decrypted_msg, protocol_header_len);
            computed_crc = Checksums.CheckSumAsBytes(Checksums.GenerateChecksumCRC16(decrypted_msg, protocol_header_len));
        }

        if (!Arrays.equals(computed_crc, crc)) {
            Log.e(TAG, "CRC not accepted!");
            return;
        }

        ////////// Device nonce, uid //////////
        ByteArrayInputStream inputStream = new ByteArrayInputStream(decrypted_msg, protocol_header_len,
                length - protocol_header_len);

        byte[] device_nonce = new byte[BST_NONCE_SIZE];

        if (inputStream.read(device_nonce, 0, BST_NONCE_SIZE) != BST_NONCE_SIZE) {
            Log.e(TAG, "Welcome message to short! Nonce is missing. " + String.valueOf(length));
            return;
        }

        byte[] uid = new byte[BST_UID_SIZE];

        if (inputStream.read(uid, 0, BST_UID_SIZE) != BST_UID_SIZE) {
            Log.e(TAG, "Welcome message to short! uid is missing. " + String.valueOf(length));
            return;
        }

        ////////// wifi_list_size_in_bytes, wifi_list_entries //////////
        if (inputStream.available() < 2) {
            Log.e(TAG, "Welcome message to short! wifi list info missing. " + String.valueOf(length));
            return;
        }

        int wifi_list_size_in_bytes = inputStream.read();
        int wifi_list_entries = inputStream.read();

        if (inputStream.available() < wifi_list_size_in_bytes) {
            Log.e(TAG, "Welcome message to short! wifi_list_size_in_bytes wrong. " + String.valueOf(length));
            return;
        }

        ////////// wifi list //////////
        WirelessNetwork currentNetworkInList = null;
        List<WirelessNetwork> list = new ArrayList<>();
        int temp;
        while (wifi_list_entries-- > 0 && inputStream.available() >= 3) {
            WirelessNetwork network = new WirelessNetwork();
            network.strength = inputStream.read();
            temp = inputStream.read();
            if (network.strength < 0 || network.strength > 100) {
                Log.e(TAG, "Parsing error for RSP_WIFI_LIST " + String.valueOf(network.strength) + " " + String.valueOf(temp));
                break;
            }

            if (temp > 0 && temp < WirelessNetwork.EncryptionMode.values().length)
                network.mode = WirelessNetwork.EncryptionMode.values()[temp];
            else
                network.mode = WirelessNetwork.EncryptionMode.Unknown;

            // Look for next \0 character
            temp = 0;
            inputStream.mark(0);
            while (inputStream.available() > 0) {
                ++temp;
                if (inputStream.read() == 0)
                    break;
            }
            inputStream.reset();

            if (temp == 0)
                break;

            byte ssid_bytes[] = new byte[temp - 1];
            if (inputStream.read(ssid_bytes, 0, ssid_bytes.length) != ssid_bytes.length ||
                    inputStream.read() != 0) {
                Log.e(TAG, "Parsing error for RSP_WIFI_LIST");
                break;
            }
            network.ssid = new String(ssid_bytes, 0, ssid_bytes.length, Charset.defaultCharset());
            list.add(network);

            wifi_list_size_in_bytes -= temp + 2;

            if (network.ssid.equals(current_ssid)) {
                currentNetworkInList = network;
            }
        }

        if (wifi_list_size_in_bytes != 0) {
            Log.e(TAG, "Could not parse wifi list");
            return;
        }

        ////////// name_or_log //////////
        temp = 0;
        inputStream.mark(0);
        while (inputStream.available() > 0) {
            ++temp;
            if (inputStream.read() == 0)
                break;
        }
        inputStream.reset();

        byte[] name_or_log;
        if (temp > 0) {
            name_or_log = new byte[temp - 1];

            if (inputStream.read(name_or_log, 0, name_or_log.length) != name_or_log.length) {
                Log.e(TAG, "Welcome message to short! name_or_log is missing. " + String.valueOf(length));
                return;
            }
        } else
            name_or_log = new byte[0];

        if (device == null) {
            device = new BootstrapDevice(new String(uid),
                    new String(name_or_log), peer.getAddress());
        }

        device.setWirelessNetwork(currentNetworkInList);

        if (is_unbound) {
            device.updateState(DeviceMode.Unbound,
                    state, list, state == DeviceState.STATE_OK ? "" : new String(name_or_log),
                    device_nonce, unbound_key, unbound_key_len);
            device.setMode(DeviceMode.Binding);
        } else {
            device.updateState(DeviceMode.Bound,
                    state, list, state == DeviceState.STATE_OK ? "" : new String(name_or_log),
                    device_nonce, bound_key, bound_key_len);

        }
        Message msg = handler.obtainMessage(MSG_BIND_OR_UPDATE, index, 0, device);
        handler.sendMessage(msg);
    }

    public void setUnboundKey(byte[] unboundKey) {
        this.unbound_key = unboundKey;
        this.unbound_key_len = unboundKey.length;
    }
}
