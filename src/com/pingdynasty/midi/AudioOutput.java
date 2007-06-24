package com.pingdynasty.midi;

import javax.sound.sampled.*;

public class AudioOutput {

    private FloatControl samplerateControl;
    private double scale;
    private SourceDataLine line;

    private byte[] databuffer;
    private int mode;
    private boolean clipping;

    private static final int PCM8SL = 1;
    private static final int PCM16SL = 2;
    private static final int PCM24SL = 3;
    private static final int PCM32SL = 4;
    private static final double twoPower7 = 128.0d;
    private static final double twoPower15 = 32768.0d;
    private static final double twoPower23 = 8388608.0d;
    private static final double twoPower31 = 2147483648.0d;

    public AudioOutput(int samples)
        throws Exception{
        this.mode = PCM16SL;
        int bitsPerSample;
        switch(mode) {
        case PCM32SL:
            databuffer = new byte[samples * 4];
            bitsPerSample = 32;
            break;
        case PCM16SL:
            databuffer = new byte[samples * 2];
            bitsPerSample = 16;
            break;
        case PCM8SL:
        default:
            databuffer = new byte[samples];
            bitsPerSample = 8;
        }
        setScaleFactor(63);
        float sampleRate = 22050.0f;
        //8000,11025,16000,22050,44100
        AudioFormat format = 
            new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
//              new AudioFormat(sampleRate, 8, 1, true, true);
        System.out.println("format: "+format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine)AudioSystem.getLine(info);
        line.open(format, databuffer.length);
        Control[] controls = line.getControls();
        for(int i=0; i<controls.length; ++i)
            System.out.println("control: "+controls[i].getType());
        try{
            samplerateControl = (FloatControl)line.getControl(FloatControl.Type.SAMPLE_RATE);
        }catch(Exception exc){exc.printStackTrace();}
        line.start();
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
        line.drain();
        switch(mode){
        case PCM32SL:
            write32SL(values);
            break;
        case PCM16SL:
            write16SL(values);
            break;
        case PCM8SL:
            write8SL(values);
            break;
        }
        line.write(databuffer, 0, databuffer.length);
    }

    public void write8SL(double[] values){
        assert values.length == databuffer.length;
        for(int i=0; i<databuffer.length; i++)
            databuffer[i] = (byte)(quantize8(values[i]*scale));
    }

    public void write16SL(double[] values){
        assert values.length * 2 == databuffer.length;
        int iSample;
        for(int i=0; i<databuffer.length; i+=2){
            // 16 bit signed little endian
            iSample = quantize16(values[i/2]*scale);
            databuffer[i+1]=(byte) (iSample >> 8);
            databuffer[i]=(byte) (iSample & 0xFF);
        }
    }

    public void write32SL(double[] values){
        assert values.length * 4 == databuffer.length;
        int iSample;
        for(int i=0; i<databuffer.length; i+=4){
            // 32 bit signed little endian
            iSample = quantize32(values[i / 4]*scale);
            databuffer[i+3] = (byte) (iSample >> 24);
            databuffer[i+2] = (byte) ((iSample >>> 16) & 0xFF);
            databuffer[i+1] = (byte) ((iSample >>> 8) & 0xFF);
            databuffer[i] = (byte) (iSample & 0xFF);
        }
    }

    public void setScaleFactor(int scale){
        scale *= 2;
        switch(mode){
        case PCM32SL:
            this.scale = scale * (twoPower31 / 127.0d);
            break;
        case PCM16SL:
            this.scale = scale * (twoPower15 / 127.0d);
            break;
        case PCM8SL:
            this.scale = scale * (twoPower7 / 127.0d);
            break;
        }
        System.out.println("scale: "+this.scale);
    }

    public float getSampleRate(){
        return samplerateControl.getValue();
    }

    public void setSampleRate(int samplerate){
        samplerateControl.setValue(((float)samplerate) * (samplerateControl.getMaximum() - samplerateControl.getMinimum()) / 127.0f + samplerateControl.getMinimum());
    }
}