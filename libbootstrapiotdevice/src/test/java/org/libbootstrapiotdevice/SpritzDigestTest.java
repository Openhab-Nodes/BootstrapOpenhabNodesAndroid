package org.libbootstrapiotdevice;

import java.security.MessageDigest;
import java.util.Arrays;

import org.junit.Test;
import org.libbootstrapiotdevice.network.spritzJ.SpritzDigest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class SpritzDigestTest {
    
    @Test
    public void test0() {
        for (byte[][] refData: new byte[][][] {
            { "ABC"    .getBytes(), new byte[] { 0x02, (byte)0x8f, (byte)0xa2, (byte)0xb4, (byte)0x8b, (byte)0x93, 0x4a, 0x18 }},
            { "spam"   .getBytes(), new byte[] { (byte)0xac, (byte)0xbb, (byte)0xa0, (byte)0x81, 0x3f, 0x30, 0x0d, 0x3a }},
            { "arcfour".getBytes(), new byte[] { (byte)0xff, (byte)0x8c, (byte)0xf2, 0x68, 0x09, 0x4c, (byte)0x87, (byte)0xb9 }}}) 
        {
            MessageDigest md = new SpritzDigest();
            byte[] buf = refData[0];
            md.update(buf[0]);
            for (int ofs = 1; ofs < buf.length; ofs++) {
                md.update(buf, ofs, 1);
            }
            byte[] digest = md.digest(); 
            assertEquals(digest.length, 32);
            byte[] must = refData[1];
            byte[] a = Arrays.copyOfRange(must  , 0, must.length);
            byte[] b = Arrays.copyOfRange(digest, 0, must.length);
            assertTrue(Arrays.equals(a,  b));
        }
    }
}
