package org.libbootstrapiotdevice.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test if the checksum works in the same way as in the firmware
 */
public class ChecksumsTest {

    //int input[] = {0x42 ,0x53 ,0x54 ,0x77 ,0x69 ,0x66 ,0x69 ,0x31 ,0xc5 ,0x86 ,0x01 ,0xa9 ,0x20 ,0xa9 ,0x38 ,0x1a ,0x31 ,0x9d ,0x32};
    byte input[] = {66, 83, 84, 119, 105, 102, 105, 49, -59, -122, 1, -87, 32, -87, 56, 26, 49, -99, 50};

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

    public void convertFromIntToBytes(int input[]) {
        System.out.printf("byte input[] = {");
        for (int i = 0; i < input.length; ++i)
            System.out.printf("%d,", (byte) input[i]);
        System.out.printf("}\n");
        System.out.flush();
    }

    @Test
    public void testHelloInputChecksumCRC16() throws Exception {
        int offset = 8 + 2 + 1; // skip header + crc field and command field
        int crc = Checksums.GenerateChecksumCRC16(input, offset);
        int crcBytes[] = {0, 0};
        crcBytes[1] = crc & 0xff;
        crcBytes[0] = (crc >> 8) & 0xff;

        int shouldBe[] = {222, 30};

        assertEquals(11, offset);
        assertEquals(8, input.length - offset);
        assertEquals(shouldBe[0], crcBytes[0]);
        assertEquals(shouldBe[1], crcBytes[1]);
    }
}