package com.example.phonedroid;

public class ILBCCodecY {
    static final private String TAG = "ILBCCodec";
    
    static final private ILBCCodecY INSTANCE = new ILBCCodecY();
    
    public native int encode(byte[] data, int dataOffset, int dataLength,
            byte[] samples, int samplesOffset);
    
    public native int decode(byte[] samples, int samplesOffset,
            int samplesLength, byte[] data, int dataOffset);
    
    public native int initEncode();
    public native int initDecode();
    

    private ILBCCodecY() {
        System.loadLibrary("ilbc-codec");
    }
    
    static public ILBCCodecY instance() {
        return INSTANCE;
    }

}
