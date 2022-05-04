//package com.googlecode.androidilbc;
package com.example.phonedroid;

import android.util.Log;

public class ILBCCodec {
    static final private String TAG = "ILBCCodec";
    
    static final private ILBCCodec INSTANCE = new ILBCCodec();
    //static final private ILBCCodec INSTANCEX = new ILBCCodec(20);
    //static final private ILBCCodec INSTANCEY = new ILBCCodec(30);
    //static final private Codec INSTANCE_Y = new Codec(false);
    //static final private Codec INSTANCE = new Codec();
    
    public native int encode(byte[] data, int dataOffset, int dataLength,
            byte[] samples, int samplesOffset);
    
    public native int decode(byte[] samples, int samplesOffset,
            int samplesLength, byte[] data, int dataOffset);
    
    private native int init(int mode);
    
    
    private ILBCCodec() {
        System.loadLibrary("ilbc-codec");
    	//System.loadLibrary("libiLBC-rfc3951");
    	//if (mode==20){
    	//	init(20);
    	//}else {
    		//if (mode==30)
    		init(20);
        	//init(20);
    	//}
        
    }
    /*
    private ILBCCodec(int mode) {
        System.loadLibrary("ilbc-codec");
    	//System.loadLibrary("libiLBC-rfc3951");
    	//if (mode==20){
    	//	init(20);
    	//}else {
    		//if (mode==30)
    		init(mode);
        	//init(20);
    	//}
        
    }

    */
    static public ILBCCodec instance() {
        return INSTANCE;
    	/*
        if (mode==20){
    	return INSTANCEX;
        }else 
        	return INSTANCEY;
        */
    }
    
    
    
    
    /*
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
    */
}
