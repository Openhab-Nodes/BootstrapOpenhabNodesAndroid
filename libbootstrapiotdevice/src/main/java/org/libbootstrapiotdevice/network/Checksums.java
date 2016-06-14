package org.libbootstrapiotdevice.network;

/**
 * Checksum methods.
 */
public class Checksums {
    static byte[] CheckSumAsBytes(int crc16) {
        byte computed_crc[] = {0, 0};
        computed_crc[1] = (byte) (crc16 & 0xff);
        computed_crc[0] = (byte) ((crc16 >> 8) & 0xff);
        return computed_crc;
    }

    /**
     * @param bytes  The input data.
     * @param offset Offset for input data.
     * @return Return the checksum as int16.
     */
    static int GenerateChecksumCRC16(byte bytes[], int offset) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)
        int crc_byte;

        for (int j = offset; j < bytes.length; ++j) {
            crc_byte = bytes[j];

            for (int i = 0; i < 8; i++) {
                boolean bit = ((crc_byte >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }
}
