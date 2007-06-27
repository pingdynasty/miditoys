package com.pingdynasty.midi;

import javax.sound.sampled.*;

public class AudioOutput {

    private double scale;
    private int mode;
    private boolean clipping;
    protected byte[] databuffer;

    public static final int PCM8S = 1;
    public static final int PCM16SL = 2;
    public static final int PCM24SL = 3;
    public static final int PCM32SL = 4;
    private static final double twoPower7 = 128.0d;
    private static final double twoPower15 = 32768.0d;
    private static final double twoPower23 = 8388608.0d;
    private static final double twoPower31 = 2147483648.0d;

    public AudioOutput(int samples, int mode)
        throws Exception{
        assert mode == PCM8S || mode == PCM16SL || mode == PCM32SL;
        this.mode = mode;
        switch(mode) {
        case PCM32SL:
            databuffer = new byte[samples * 4];
            break;
        case PCM16SL:
            databuffer = new byte[samples * 2];
            break;
        case PCM8S:
        default:
            databuffer = new byte[samples];
        }
        setScaleFactor(63);
    }

    public AudioFormat getAudioFormat(float sampleRate){
        switch(mode) {
        case PCM32SL:
            return new AudioFormat(sampleRate, 32, 1, true, false);
        case PCM16SL:
            return new AudioFormat(sampleRate, 16, 1, true, false);
        case PCM8S:
        default:
            return new AudioFormat(sampleRate, 8, 1, true, false);
        }
    }

    public boolean clipping(){
        return clipping;
    }

    private int quantize8(double sample) {
        if(sample >= 127.0d){
            clipping = true;
            return 127;
        }else if(sample <= -127.0d){
            clipping = true;
            return -127;
        }else
            return (int)sample;
    }

    private int quantize16(double sample) {
        if(sample >= 32767.0d){
            clipping = true;
            return 32767;
        }else if(sample <= -32767.0d){
            clipping = true;
            return -32768;
        }else
            return (int)sample;
//             return (int)(sample < 0 ? (sample - 0.5d) : (sample + 0.5d));
    }

    private static final int quantize32(double sample) {
        if(sample >= 2147483647.0d)
            return 2147483647;
        else if(sample <= -2147483648.0d)
            return -2147483648;
        else
            return (int)(sample < 0 ? (sample - 0.5d) : (sample + 0.5d));
    }

    public void write(double[] values){
        clipping = false;
        switch(mode){
        case PCM32SL:
            write32bitSignedLittleEndian(values);
            break;
        case PCM16SL:
            write16bitSignedLittleEndian(values);
            break;
        case PCM8S:
            write8bitLittleEndian(values);
            break;
        }
    }

    public void write8bitLittleEndian(double[] values){
        assert values.length == databuffer.length;
        for(int i=0; i<databuffer.length; i++)
            databuffer[i] = (byte)(quantize8(values[i]*scale));
    }

    public void write16bitSignedLittleEndian(double[] values){
        assert values.length * 2 == databuffer.length;
        int iSample;
        for(int i=0; i<databuffer.length; i+=2){
            // 16 bit signed little endian
            iSample = quantize16(values[i/2]*scale);
            databuffer[i+1]=(byte) (iSample >> 8);
            databuffer[i]=(byte) (iSample & 0xFF);
        }
    }

    public void write32bitSignedLittleEndian(double[] values){
        assert values.length * 4 == databuffer.length;
        int iSample;
        for(int i=0; i<databuffer.length; i+=4){
            // 32 bit signed little endian
            iSample = quantize32(values[i/4]*scale);
            databuffer[i+3] = (byte) (iSample >> 24);
            databuffer[i+2] = (byte) ((iSample >>> 16) & 0xFF);
            databuffer[i+1] = (byte) ((iSample >>> 8) & 0xFF);
            databuffer[i] = (byte) (iSample & 0xFF);
        }
    }

    public int getScaleFactor(){
        switch(mode){
        case PCM32SL:
            return (int)(scale / twoPower31 * 127.0d / 2.0d);
        case PCM16SL:
            return (int)(scale / twoPower15 * 127.0d / 2.0d);
        case PCM8S:
            return (int)(scale / twoPower7 * 127.0d / 2.0d);
        }
        return 0;
    }

    public void setScaleFactor(int scale){
        scale *= 2;
        switch(mode){
        case PCM32SL:
            this.scale = scale * twoPower31 / 127.0d;
            break;
        case PCM16SL:
            this.scale = scale * twoPower15 / 127.0d;
            break;
        case PCM8S:
            this.scale = scale * twoPower7 / 127.0d;
            break;
        }
        System.out.println("scale: "+this.scale);
    }
}