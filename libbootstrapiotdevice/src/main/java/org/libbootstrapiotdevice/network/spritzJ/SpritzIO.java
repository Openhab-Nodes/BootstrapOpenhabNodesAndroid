package org.libbootstrapiotdevice.network.spritzJ;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Spritz cipher streams. Uses 10 bytes of IV/salt/nonce.
 */
public class SpritzIO {

    final static int IV_LENGTH = 10;
    
    private static void forceRead(InputStream is, byte[] buf) throws IOException{
        for (int i = 0; i < buf.length; i++) {
            int b = is.read();
            if (-1 == b)
                throw new IOException("unexpected end of stream");
            buf[i] = (byte)b;
        }
    }
    
    public static InputStream newDecryptInputStream(final InputStream is,
            byte[] key, int keyOfs, int keyLen) throws IOException {
        final SpritzState ss = new SpritzState();
        byte[] iv = new byte[IV_LENGTH];
        forceRead(is, iv);
        ss.cipherInit(key, keyOfs, keyLen, iv, 0, iv.length);
        return new InputStream() {
            byte[] aByte = new byte[1];

            @Override
            public int read() throws IOException {
                int b = is.read();
                if (-1 == b)
                    return -1;
                this.aByte[0] = (byte)b;
                ss.cipherDecrypt(this.aByte, 0, 1, this.aByte, 0);
                return this.aByte[0] & 255;
            }

            @Override
            public void close() throws IOException {
                ss.erase();
                is.close();
            }
        };
    }
    
    public static OutputStream newEncryptOutputStream(final OutputStream os,
            byte[] key, int keyOfs, int keyLen, Random rnd) throws IOException {
        byte[] iv = new byte[IV_LENGTH];
        (null == rnd ? new SecureRandom() : rnd).nextBytes(iv);
        final SpritzState ss = new SpritzState();
        ss.cipherInit(key, keyOfs, keyLen, iv, 0, iv.length);
        os.write(iv);
        return new OutputStream() {
            byte[] aByte = new byte[1];

            @Override
            public void write(int b) throws IOException {
                ss.cipherEncrypt(this.aByte, 0, 1, this.aByte, 0);
                os.write(this.aByte[0] & 255);
            }

            @Override
            public void close() throws IOException {
                ss.erase();
                os.close();
            }
        };
    }
}