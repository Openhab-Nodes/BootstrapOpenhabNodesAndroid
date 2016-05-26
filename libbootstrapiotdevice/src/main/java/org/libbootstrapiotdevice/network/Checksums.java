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

        int crc = 0xFFFF;
        int temp;
        int crc_byte;

        for (int i = offset; i < bytes.length; ++i) {
            crc_byte = bytes[i];

            for (int bit_index = 0; bit_index < 8; bit_index++) {

                temp = ((crc >> 15)) ^ ((crc_byte >> 7));

                crc <<= 1;
                crc &= 0xFFFF;

                if (temp > 0) {
                    crc ^= 0x1021;
                    crc &= 0xFFFF;
                }

                crc_byte <<= 1;
                crc_byte &= 0xFF;

            }
        }

        return crc;
    }
}
