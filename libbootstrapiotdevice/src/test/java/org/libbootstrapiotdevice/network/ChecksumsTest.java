package org.libbootstrapiotdevice.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test if the checksum works in the same way as in the firmware
 */
public class ChecksumsTest {

    @Test
    public void testGenerateChecksumCRC16() throws Exception {
        byte data[] = {'1', '2', '3', '4', '5', '6', '7', '8', '9'};
        int crc = Checksums.GenerateChecksumCRC16(data, 0);
        int crcBytes[] = {0, 0};
        crcBytes[1] = crc & 0xff;
        crcBytes[0] = (crc >> 8) & 0xff;
        int shouldBe[] = {0x29, 0xB1};
        assertEquals(shouldBe[0], crcBytes[0]);
        assertEquals(shouldBe[1], crcBytes[1]);

        byte shouldBe2[] = {0x29, -0x4F};
        byte[] crcBytes2 = Checksums.CheckSumAsBytes(crc);
        assertEquals(shouldBe2[0], crcBytes2[0]);
        assertEquals(shouldBe2[1], crcBytes2[1]);
    }
}