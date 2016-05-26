package org.libbootstrapiotdevice.network.spritzJ;

import java.util.Arrays;

// NOTE: implemented for clarity right now, certain quick optimization attempts
//       didn't yield any real performance gains, the JIT seems to be already
//       producing decent code now; aggressive unrolling hasn't been tested yet
//       though ...

public class SpritzState {

    final static int N = 256;

    final static int[] S_INIT = new int[N];
    static {
        for (int i = 0; i < S_INIT.length; i++)
            S_INIT[i] = i;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    private int[] s;
    private int a, i, j, k, w, z;

    private void initializeState() {
        this.s = S_INIT.clone();
        this.a = this.i = this.j = this.k = this.z = 0;
        this.w = 1;
    }

    private void update() {
        this.i += this.w;
        this.i &= 255;
        int y = (this.j + this.s[this.i]) & 255;
        this.j = (this.k + this.s[y]) & 255;
        this.k = (this.i + this.k + this.s[this.j]) & 255;
        int t = this.s[this.i & 0xff];
        this.s[this.i] = this.s[this.j];
        this.s[this.j] = t;
    }

    private int output() {
        int y1 = (this.z + this.k) & 255;
        int x1 = (this.i + this.s[y1]) & 255;
        int y2 = (this.j + this.s[x1]) & 255;
        this.z = this.s[y2];
        return this.z;
    }

    private void crush() {
        for (int v = 0; v < N / 2; v++) {
            int y = (N - 1) - v;
            int x1 = this.s[v];
            int x2 = this.s[y];
            if (x1 > x2) {
                this.s[v] = x2;
                this.s[y] = x1;
            } else {
                this.s[v] = x1;
                this.s[y] = x2;
            }
        }
    }

    private void whip() {
        for (int v = 0; v < N * 2; v++) {
            update();
        }
        this.w += 2;
    }

    private void shuffle() {
        whip();
        crush();
        whip();
        crush();
        whip();
        this.a = 0;
    }

    private void absorbStop() {
        if (this.a == N / 2) {
            shuffle();
        }
        this.a++;
    }

    private void absorbNibble(int x) {
        if (this.a == N / 2) {
            shuffle();
        }
        int y = (N / 2 + x) & 255;
        int t = this.s[this.a];
        this.s[this.a] = this.s[y];
        this.s[y] = t;
        this.a++;
    }

    private void absorbByte(int b) {
        absorbNibble(b & 15);
        absorbNibble((b >>> 4) & 15);
    }

    private void absorb(byte[] msg, int ofs, int len) {
        for (int end = ofs + len; ofs < end; ofs++) {
            absorbByte(msg[ofs] & 255);
        }
    }

    private int drip() {
        if (this.a > 0) {
            shuffle();
        }
        update();
        return output();
    }

    private void squeeze(byte[] out, int ofs, int len) {
        if (this.a > 0) {
            shuffle();
        }
        for (int end = ofs + len; ofs < end; ofs++) {
            out[ofs] = (byte)drip();
        }
    }

    private void keySetup(byte[] key, int ofs, int len) {
        initializeState();
        absorb(key, ofs, len);
        // One C implementation does this, but it's not in the paper... 
        //if (this.a > 0) {
        //    shuffle();
        //}
    }

    ///////////////////////////////////////////////////////////////////////////

    public void erase() {
       Arrays.fill(this.s, 0);
       this.a = this.i = this.j = this.k = this.w = this.z = 0;
    }

    ///////////////////////////////////////////////////////////////////////////

    // streaming API ...
    
    public void streamInit(byte[] key, int ofs, int len) {
        initializeState();
        absorb(key, ofs, len);
    }
    
    public void streamRead(byte[] out, int ofs, int len) {
        squeeze(out, ofs, len);
    }

    ///////////////////////////////////////////////////////////////////////////

    // hash API ...

    public void hashInit() {
        initializeState();
        this.a = 0;
        this.i = 0;
        this.j = 0;
        this.k = 0;
        this.w = 1;
        this.z = 0;
    }

    public void hashUpdate(int aByte) {
        absorbByte(aByte);
    }
    
    public void hashUpdate(byte[] buf, int ofs, int len) {
        for (int end = ofs + len; ofs < end; ofs++) {
            absorbByte(buf[ofs]);
        }
    }
    
    public void hashFinal(byte[] hash, int ofs, int len) {
        absorbStop();
        absorbByte(len & 255);
        squeeze(hash, ofs, len);
    }

    ///////////////////////////////////////////////////////////////////////////

    // encryption API ...
    
    public void cipherInit(byte[] key, int keyOfs, int keyLen,
                           byte[] iv , int ivOfs , int ivLen) {
        keySetup(key, keyOfs, keyLen);
        absorbStop();
        absorb(iv, ivOfs, ivLen);
    }
    
    public void cipherEncrypt(byte[] in , int inOfs , int len,
                              byte[] out, int outOfs) {
        for (int i = 0; i < len; i++)
            out[outOfs + i] = (byte)(in[inOfs + i] + drip());
    }

    public void cipherDecrypt(byte[] in , int inOfs , int len,
                              byte[] out, int outOfs) {
        for (int i = 0; i < len; i++)
            out[outOfs + i] = (byte)(in[inOfs + i] - drip());
    }
    
    ///////////////////////////////////////////////////////////////////////////

//  void printState() {
//      System.out.println(String.format("STATE i=%d j=%d k=%d z=%d a=%d w=%d",
//              this.i, this.j, this.k, this.z, this.a, this.w));
//      for (int s: this.s)
//          System.out.print(String.format("%02x:", s));
//      System.out.println("-----------");
//  }

}
