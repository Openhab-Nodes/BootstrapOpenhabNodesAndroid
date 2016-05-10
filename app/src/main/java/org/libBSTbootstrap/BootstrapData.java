package org.libBSTbootstrap;

import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Bootstrap data that will be send to a BS device
 */
public class BootstrapData {
    String bst_ssid = null;
    String bst_pwd = null;
    Map<String, String> bst_additional_bst_data = null;

    public void getData(@NonNull ByteArrayOutputStream outputStream) {
        if (bst_ssid == null)
            throw new RuntimeException("bst_ssid may not be empty!");

        byte[] d;
        d = bst_ssid.getBytes(StandardCharsets.UTF_8);
        outputStream.write(d, 0, d.length);
        outputStream.write('\0');

        if (bst_pwd != null) {
            d = bst_pwd.getBytes(StandardCharsets.UTF_8);
            outputStream.write(d, 0, d.length);
            outputStream.write('\0');
        } else
            outputStream.write('\0');

        if (bst_additional_bst_data != null) {
            for (Map.Entry<String,String> entry: bst_additional_bst_data.entrySet()) {
                d = entry.getKey().replace('\t',' ').getBytes(StandardCharsets.UTF_8);
                outputStream.write(d, 0, d.length);
                outputStream.write('\t');
                d = entry.getValue().replace('\t',' ').getBytes(StandardCharsets.UTF_8);
                outputStream.write(d, 0, d.length);
                outputStream.write('\t');
            }
            outputStream.write('\0');
        } else
            outputStream.write('\0');
    }
}
