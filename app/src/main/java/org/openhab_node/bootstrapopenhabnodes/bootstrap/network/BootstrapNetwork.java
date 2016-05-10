package org.openhab_node.bootstrapopenhabnodes.bootstrap.network;

import android.content.Context;
import android.util.Log;

import org.openhab_node.bootstrapopenhabnodes.R;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapData;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDevice;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.WirelessNetwork;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 26.04.16.
 */
public class BootstrapNetwork extends UDPSendReceive implements BootstrapNetworkSendAPI {
    protected byte[] header = "BSTwifi1".getBytes();
    private BootstrapDevice device = null;

    private BootstrapNetworkReceiveAPI observer;

    private static String TAG = "BootstrapNetwork";
    public static final int RECEIVE_PORT = 8711;
    public static final int SEND_PORT = 8711;

    public BootstrapNetwork(Context context) {
        super(context, RECEIVE_PORT);
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    public static String getErrorString(Context c, int errorCode) {
        receive_code_error code = errorCode>receive_code_error.values().length ? receive_code_error.RSP_ERROR_UNSPECIFIED : receive_code_error.values()[errorCode];
        switch (code) {
            case RSP_ERROR_BINDING:
                return c.getString(R.string.error_binding_failed);
            case RSP_ERROR_BOOTSTRAP_DATA:
                return c.getString(R.string.error_bootstrap_data_invalid);
            case RSP_ERROR_WIFI_LIST:
                return c.getString(R.string.error_no_wifi_list_available);
            case RSP_ERROR_WIFI_NOT_FOUND:
                return c.getString(R.string.error_wifi_not_found);
            case RSP_ERROR_WIFI_PWD_WRONG:
                return c.getString(R.string.error_credentials_wrong);
            case RSP_ERROR_ADVANCED:
                return c.getString(R.string.error_additional_data);
            case RSP_ERROR_UNSPECIFIED:
            default:
                return c.getString(R.string.error_unknown);
        }
    }

    private enum send_command {
        CMD_UNKNOWN,
        CMD_HELLO,
        CMD_SET_DATA,
        CMD_RESET_FACTORY,
        CMD_REQUEST_WIFI,
        CMD_REQUEST_ERROR_LOG,
        CMD_BIND
    }

    private enum receive_code_error {
        RSP_ERROR_UNSPECIFIED,
        RSP_ERROR_BINDING,
        RSP_ERROR_BOOTSTRAP_DATA,
        RSP_ERROR_WIFI_LIST,
        RSP_ERROR_WIFI_NOT_FOUND,
        RSP_ERROR_WIFI_PWD_WRONG,
        RSP_ERROR_ADVANCED
    }

    private enum receive_code_success {
        RSP_WIFI_LIST,
        RSP_BINDING_ACCEPTED,
        RSP_DATA_ACCEPTED,
        RSP_WELCOME_MESSAGE
    }

    public void setObserver(BootstrapNetworkReceiveAPI observer) {
        this.observer = observer;
    }

    @Override
    public void useDevice(BootstrapDevice device) {
        this.device = device;
//        if (device.addresses.size()>0)
//            setDestination(SEND_PORT, device.addresses.iterator().next());
//        else
//            setDestination(SEND_PORT, multicastGroup);
        setDestination(SEND_PORT, multicastGroup);
    }

    int mSessionID = new Random().nextInt();

    private void initPacket(send_command cmd) {
        sendStream.reset();
        sendStream.write(header, 0, header.length);
        sendStream.write(mSessionID & 0xff);
        sendStream.write((mSessionID >> 8) & 0xff);
        sendStream.write(cmd.ordinal());
    }

    @Override
    public boolean sendHello() {
        if (device == null || !isSendingAllowed() || socket.isClosed()){
            Log.e(TAG, "sendHello send failed");
            return false;
        }
        initPacket(send_command.CMD_HELLO);
        return send();
    }

    @Override
    public boolean bindToDevice(String new_secret) {
        if (device == null || !isSendingAllowed() || socket.isClosed()) {
            Log.e(TAG, "bindToDevice send failed");
            return false;
        }

        initPacket(send_command.CMD_BIND);
        byte[] d;
        d = new_secret.getBytes(StandardCharsets.UTF_8);
        sendStream.write(d, 0, d.length);
        return send();
    }

    @Override
    public boolean sendBootstrapData(BootstrapData data) {
        if (device == null || !isSendingAllowed() || socket.isClosed()){
            Log.e(TAG, "sendBootstrapData send failed");
            return false;
        }
        initPacket(send_command.CMD_SET_DATA);
        data.getData(sendStream);
        return send();
    }

    @Override
    public boolean factoryReset() {
        if (device == null || !isSendingAllowed() || socket.isClosed()){
            Log.e(TAG, "factoryReset send failed");
            return false;
        }

        initPacket(send_command.CMD_RESET_FACTORY);
        return send();
    }

    @Override
    public boolean requestWifiList() {
        if (device == null || !isSendingAllowed() || socket.isClosed()){
            Log.e(TAG, "requestWifiList send failed");
            return false;
        }

        initPacket(send_command.CMD_REQUEST_WIFI);
        return send();
    }

    @Override
    public boolean requestLastError() {
        if (device == null || !isSendingAllowed() || socket.isClosed()){
            Log.e(TAG, "requestLastError send failed");
            return false;
        }

        initPacket(send_command.CMD_REQUEST_ERROR_LOG);
        return send();
    }

    @Override
    protected void parsePacket(byte[] message, int length, InetSocketAddress peer) {
        if (device == null || socket.isClosed()) {
            throw new RuntimeException("Received something but no device set before");
        }

        if (observer == null) {
            Log.w(TAG, "No observer");
            return;
        }

        if (length < header.length + 1) {
            Log.e(TAG, "Received packet to small!");
        }

        for (int i = 0; i < header.length; ++i)
            if (header[i] != message[i]) {
                Log.e(TAG, "Header not equal!");
                return;
            }

        int code = message[header.length];
        if (code <= receive_code_error.values()[receive_code_error.values().length-1].ordinal()) {
            Log.e(TAG, "device_lastError");
            observer.device_lastError(device, code, new String(message,header.length,length-header.length, Charset.defaultCharset()));
            return;
        }

        if (code < 50) {
            Log.e(TAG, "Command unknown!");
            return;
        }

        code -= 50;

        if (code > receive_code_success.values()[receive_code_success.values().length-1].ordinal()) {
            Log.e(TAG, "Command unknown!");
            return;
        }

        receive_code_success r = receive_code_success.values()[code];
        ByteArrayInputStream inputStream = new ByteArrayInputStream(message,header.length+1,length-header.length-1);
        switch (r) {
            case RSP_WIFI_LIST:
                List<WirelessNetwork> list = new ArrayList<>();
                while (inputStream.available()>=3) {
                    int temp;
                    WirelessNetwork network = new WirelessNetwork();
                    network.strength = inputStream.read();
                    temp = inputStream.read();
                    if (network.strength < 0 || network.strength > 100 || temp > WirelessNetwork.EncryptionMode.values().length) {
                        Log.e(TAG, "Parsing error for RSP_WIFI_LIST");
                        break;
                    }
                    network.mode = WirelessNetwork.EncryptionMode.values()[temp];
                    // Look for next \0 character
                    temp = 0;
                    inputStream.mark(0);
                    while (inputStream.available()>0) {
                        if (inputStream.read() == 0)
                            break;
                        else
                            ++temp;
                    }
                    inputStream.reset();
                    byte ssid_bytes[] = new byte[temp];
                    if (inputStream.read(ssid_bytes, 0, temp) != temp) {
                        Log.e(TAG, "Parsing error for RSP_WIFI_LIST");
                        break;
                    }
                    network.ssid = new String(ssid_bytes,0,ssid_bytes.length, Charset.defaultCharset());
                    list.add(network);
                }

                observer.device_wifiList(device, list);
                break;
            case RSP_BINDING_ACCEPTED:
                observer.device_bindingAccepted(device);
                break;
            case RSP_DATA_ACCEPTED:
                observer.device_dataAccepted(device);
                break;
            case RSP_WELCOME_MESSAGE:
                if (inputStream.available()!=2) {
                    Log.e(TAG, "Welcome message to short! "+String.valueOf(length)+" "+String.valueOf(header.length));
                    return;
                }
                mSessionID = inputStream.read()| inputStream.read() << 8 ;
                Log.w(TAG, "Session is "+String.valueOf(mSessionID));
                observer.device_welcomeMessage(device, peer.getAddress(), mSessionID);
                break;
        }
    }
}
