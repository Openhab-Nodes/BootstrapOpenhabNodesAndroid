package org.libbootstrapiotdevice;

import org.junit.Test;
import org.libbootstrapiotdevice.network.spritzJ.SpritzState;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpritzStateTest {
    
    final static byte[][][] REF_DATA_STREAM = {
        { "ABC"    .getBytes(), new byte[]{(byte)0x77,(byte)0x9a,(byte)0x8e,(byte)0x01,(byte)0xf9,(byte)0xe9,(byte)0xcb,(byte)0xc0}},
        { "spam"   .getBytes(), new byte[]{(byte)0xf0,(byte)0x60,(byte)0x9a,(byte)0x1d,(byte)0xf1,(byte)0x43,(byte)0xce,(byte)0xbf}},
        { "arcfour".getBytes(), new byte[]{(byte)0x1a,(byte)0xfa,(byte)0x8b,(byte)0x5e,(byte)0xe3,(byte)0x37,(byte)0xdb,(byte)0xc7}}
    };

    final static byte[][][] REF_DATA_HASH = {
        { "ABC"    .getBytes(), new byte[]{(byte)0x02,(byte)0x8f,(byte)0xa2,(byte)0xb4,(byte)0x8b,(byte)0x93,(byte)0x4a,(byte)0x18}},
        { "spam"   .getBytes(), new byte[]{(byte)0xac,(byte)0xbb,(byte)0xa0,(byte)0x81,(byte)0x3f,(byte)0x30,(byte)0x0d,(byte)0x3a}},
        { "arcfour".getBytes(), new byte[]{(byte)0xff,(byte)0x8c,(byte)0xf2,(byte)0x68,(byte)0x09,(byte)0x4c,(byte)0x87,(byte)0xb9}}
    };
    
    @Test
    public void testStream() {
        for (byte[][] refData : REF_DATA_STREAM) {
            SpritzState ss = new SpritzState();
            byte[] key = refData[0];
            ss.streamInit(key, 0, key.length);
            byte[] expected = refData[1];
            byte[] out = new byte[3];
            for (byte anExpected : expected) {
                out[0] = 55;
                out[2] = 111;
                ss.streamRead(out, 1, 1);
                assertEquals(out[1], anExpected);
            }
        }
    }

    @Test
    public void testHash() {
        for (byte[][] refData : REF_DATA_HASH) {
            SpritzState ss = new SpritzState();
            ss.hashInit();
            byte[] buf = refData[0];
            for (int ofs = 0; ofs < buf.length; ofs++) {
                ss.hashUpdate(buf, ofs, 1);
            }
            byte[] hash = new byte[33];
            hash[0] = (byte)0xcc;
            ss.hashFinal(hash, 1, 32);
            byte[] must = refData[1];
            for (int i = 0; i < must.length; i++) {
                assertEquals(hash[i + 1], must[i]);
            }
            assertEquals(hash[0], (byte)0xcc);
       }
    }
    
    
    @Test
    public void testCipher() {
        final byte[] key = new byte[101];
        for (int i = 1; i < key.length; i++)
            key[i] = (byte)(80 + i);
        final byte[] iv = new byte[51];
        for (int i = 1; i < iv.length; i++)
            iv[i] = (byte)i;
        byte[] plainText = new byte[257];
        for (int i = 1; i < plainText.length; i++)
            plainText[i] = (byte)i;
        SpritzState ss = new SpritzState();
        ss.cipherInit(key, 1, 100, iv, 1, 50);
        byte[] cipherText = new byte[1 + plainText.length + 1];
        ss.cipherEncrypt(plainText, 1, 256, cipherText, 1);
        assertEquals(0, cipherText[0]);
        assertEquals(0, cipherText[257]);
        for (int i = 1; i < plainText.length; i++)
            assertEquals(plainText[i], (byte)i);
        key[0] = iv[0] = (byte)0xcc;
        ss = new SpritzState();
        ss.cipherInit(key, 1, 100, iv, 1, 50);
        byte[] plainText2 = new byte[256];
        ss.cipherDecrypt(cipherText, 1, 256, plainText2, 0);
        byte[] a = Arrays.copyOfRange(plainText , 1, 257);
        byte[] b = Arrays.copyOfRange(plainText2, 0, 256);
        assertTrue(Arrays.equals(a, b));
        ss.erase();
    }

    @Test
    public void testCipher2() {
        final int IV_LEN = 10;
        byte[] ref = new byte[] {
            (byte)0x04,(byte)0x15,(byte)0xaa,(byte)0xf4,(byte)0x1e,(byte)0xd4,
            (byte)0xc9,(byte)0x20,(byte)0xec,(byte)0xcc,(byte)0x7e,(byte)0x1c,
            (byte)0x8a,(byte)0x0e,(byte)0x80,(byte)0x43,(byte)0x21,(byte)0xe2,
            (byte)0x4d,(byte)0xb2,(byte)0xae};
        byte[] key = "test123".getBytes(); 
        SpritzState ss = new SpritzState();
        ss.cipherInit(key, 0, key.length, ref, 0, IV_LEN);
        byte[] must = "spritzsaber".getBytes();
        assertEquals(IV_LEN + must.length, ref.length);
        byte[] decr = new byte[must.length];
        ss.cipherDecrypt(ref, IV_LEN, must.length, decr, 0);
        byte[] a = Arrays.copyOfRange(must, 0, must.length);
        byte[] b = Arrays.copyOfRange(decr, 0, must.length);
        assertTrue(Arrays.equals(a, b));
    }

    // Same test as in TEST(TestCrypto, BasicTest) in test_spritz.c of the bootstrapWifiTests suite.
    @Test
    public void testCipher_firmware() {
        byte msg[] = {'a', 'r', 'c', 'f', 'o', 'u', 'r'};
        byte result[] = {0, 0, 0, 0, 0, 0, 0};
        byte expect[] = {(byte) 0xff, (byte) 0x7b, (byte) 0xd7, (byte) 0xc7, 'D', (byte) 0x81, (byte) 0xb6,};
        byte nonce[] = "nonce\0".getBytes();
        byte key[] = "secret\0".getBytes();

        SpritzState ss = new SpritzState();
        ss.cipherInit(key, 0, key.length, nonce, 0, nonce.length);
        ss.cipherEncrypt(msg, 0, msg.length, result, 0);

        assertTrue(Arrays.equals(result, expect));
    }
}
