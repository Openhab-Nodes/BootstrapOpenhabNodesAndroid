package org.libbootstrapiotdevice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.libbootstrapiotdevice.network.spritzJ.SpritzIO;

import static org.junit.Assert.assertEquals;

public class SpritzIOTest {

    final static byte[] REF_DATA = new byte[] {
        0x04, 0x15, (byte)0xaa, (byte)0xf4, 0x1e, (byte)0xd4, (byte)0xc9, 0x20, 
        (byte)0xec, (byte)0xcc, 0x7e, 0x1c, (byte)0x8a, 0x0e, (byte)0x80, 0x43, 
        0x21, (byte)0xe2, 0x4d, (byte)0xb2, (byte)0xae };
    
    @Test
    public void test0() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(REF_DATA.clone());
        byte[] k = "test123".getBytes();
        InputStream sis = SpritzIO.newDecryptInputStream(bais, k, 0, k.length);
        byte[] must = "spritzsaber".getBytes();
        for (byte aMust : must) assertEquals(sis.read() & 255, aMust & 255);
        assertEquals(sis.read(), -1);
        sis.close();
    }
}
