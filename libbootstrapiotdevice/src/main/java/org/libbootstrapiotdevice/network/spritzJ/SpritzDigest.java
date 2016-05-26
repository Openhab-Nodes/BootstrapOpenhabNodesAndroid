package org.libbootstrapiotdevice.network.spritzJ;

import java.security.MessageDigest;

/**
 * Spritz message digest implementation. Creates 256 bits of output. Use it as
 * you would any other MessageDigest instance in Java, except of the direction
 * construction instead of a name lookup.
 */
public class SpritzDigest extends MessageDigest {

    public final static int DIGEST_LENGTH = 32;
    
    private SpritzState ss;

    public SpritzDigest() {
        this("Spritz");
    }
    
    protected SpritzDigest(String algorithm) {
        super(algorithm);
        engineReset();
    }

    @Override
    protected void engineUpdate(byte input) {
        this.ss.hashUpdate(input & 255);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        this.ss.hashUpdate(input, offset, len);
    }

    @Override
    protected byte[] engineDigest() {
        byte[] result = new byte[DIGEST_LENGTH];
        this.ss.hashFinal(result, 0, result.length);
        return result;
    }

    @Override
    protected void engineReset() {
        if (null != this.ss)
            this.ss.erase();
        this.ss = new SpritzState();
        this.ss.hashInit();
    }

    @Override
    protected int engineGetDigestLength() {
        return DIGEST_LENGTH;
    }
}
