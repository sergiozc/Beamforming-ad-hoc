//package com.googlecode.androidilbc;
package com.example.phonedroid;

import android.util.Log;

public class Codec {
    static final private String TAG = "Codec";
    
    static final private Codec INSTANCE = new Codec();
    
    
    public native int encode(byte[] data, int dataOffset, int dataLength,
            byte[] samples, int samplesOffset);
    
    public native int decode(byte[] samples, int samplesOffset,
            int samplesLength, byte[] data, int dataOffset);
    
    private native int init(int mode);
    
     

    private Codec() {
        System.loadLibrary("ilbc-codec");
    	//System.loadLibrary("libiLBC-rfc3951");
    	//if (mode==20){
    	//	init(20);
    	//}else {
    		//if (mode==30)
    		//init(30);
        	init(20);
    	//}
        
    }

    
    static public Codec instance() {
        return INSTANCE;
    }
    
    
    
    
    
    public byte[] encode(byte[] samples, int offset, int len) {
        byte[] data = new byte[4096 * 10];
        
        int bytesEncoded = 0;
        
        bytesEncoded += encode(samples, offset, len, data, 0);
        
        Log.e(TAG, "Encode " + bytesEncoded);
        
        return data;
    }
    
    public byte[] decode(byte[] data, int offset, int len) {
        return null;
    }
}
