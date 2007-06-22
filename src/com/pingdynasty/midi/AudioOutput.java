package com.pingdynasty.midi;

import javax.sound.sampled.*;

public class AudioOutput {

    FloatControl samplerateControl;
//     double scale = 255.0d;
    double scale = 100.0d; // 
    SourceDataLine line;

    byte[] databuffer;

    public AudioOutput(int samples)
        throws Exception{
//         float sampleRate = 8000.0f;
//         float sampleRate = 11025.0f;
        float sampleRate = 22050.0f;
        //8000,11025,16000,22050,44100
        AudioFormat format = 
            new AudioFormat(sampleRate, 8, 1, true, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine)AudioSystem.getLine(info);
        line.open(format, samples * 10);
        samplerateControl = (FloatControl)line.getControl(FloatControl.Type.SAMPLE_RATE);
        line.start();
        databuffer = new byte[samples];
    }

    public void write(double[] values){
        line.drain();
        for(int i=0; i<databuffer.length; ++i)
            databuffer[i] = (byte)(values[i]*scale);
//             line.write((int)(values[i]*scale));
        line.write(databuffer, 0, databuffer.length);
    }

    public void setScaleFactor(double scale){
        System.out.println("scale: "+scale);
        this.scale = scale;
    }

    public void setSampleRate(int samplerate){
        samplerateControl.setValue(((float)samplerate) * (samplerateControl.getMaximum() - samplerateControl.getMinimum()) / 127.0f + samplerateControl.getMinimum());
        System.out.println("samplerate: "+samplerateControl.getValue());
    }
}