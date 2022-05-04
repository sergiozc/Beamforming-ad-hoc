package com.example.phonedroid;

public class ILBCCodecX {
    static final private String TAG = "ILBCCodec";
    
    static final private ILBCCodecX INSTANCE = new ILBCCodecX();
    
    public native int encode(byte[] data, int dataOffset, int dataLength,
            byte[] samples, int samplesOffset);
    
    public native int decode(byte[] samples, int samplesOffset,
            int samplesLength, byte[] data, int dataOffset);
    
    public native int initEncode();
    public native int initDecode();
    
    
    private ILBCCodecX() {
        System.loadLibrary("ilbc-codec");
    }
    
    static public ILBCCodecX instance() {
        return INSTANCE;
    }
    
}
