package org.libbootstrapiotdevice;

import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bootstrap data that will be send to a BS device. This can be used as singleton,
 * to allow activities to add additional data.
 */
public class BootstrapData {
    static private BootstrapData i = new BootstrapData();
    Map<String, String> additional = new TreeMap<>();
    private String bst_ssid = null;
    private String bst_pwd = null;

    public static BootstrapData instance() {
        return i;
    }

    public void setWifiData(String bst_ssid, String bst_pwd) {
        this.bst_ssid = bst_ssid;
        this.bst_pwd = bst_pwd;
    }

    public void setWifiData(WirelessNetwork network) {
        this.bst_ssid = network.ssid;
        this.bst_pwd = network.pwd;
    }


    public void addAdditionalData(String key, String value) {
        if (key == null || value == null)
            throw new RuntimeException("Empty value not allowed");
        additional.put(key, value);
    }

    public int addDataToStream(@NonNull ByteArrayOutputStream outputStream) {
        if (bst_ssid == null)
            throw new RuntimeException("bst_ssid may not be empty!");

        byte[] d;
        d = bst_ssid.getBytes();
        outputStream.write(d, 0, d.length);
        outputStream.write(0);
        int written = d.length + 1;

        if (bst_pwd != null) {
            d = bst_pwd.getBytes();
            outputStream.write(d, 0, d.length);
            outputStream.write(0);
            written += d.length;
        } else
            outputStream.write(0);
        ++written;

        for (Map.Entry<String, String> entry : additional.entrySet()) {
            d = entry.getKey().replace('\t', ' ').getBytes();
            outputStream.write(d, 0, d.length);
            outputStream.write('\t');
            written += d.length + 1;
            d = entry.getValue().replace('\t', ' ').getBytes();
            outputStream.write(d, 0, d.length);
            outputStream.write('\t');
            written += d.length + 1;
        }
        outputStream.write('\0');
        ++written;
        return written;
    }
}
