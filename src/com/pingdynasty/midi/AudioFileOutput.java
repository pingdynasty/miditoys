package com.pingdynasty.midi;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import javax.sound.sampled.*;

public class AudioFileOutput extends AudioOutput {
    private ByteArrayOutputStream out;

    public AudioFileOutput(int samples, int mode)
        throws Exception{
        super(samples, mode);
        out = new ByteArrayOutputStream();
    }

    public void write(double[] values){
        super.write(values);
        out.write(databuffer, 0, databuffer.length);
    }

    public void reset(){
        out.reset();
    }

    public void write(float sampleRate, File file)
        throws IOException {
        AudioFormat format = getAudioFormat(sampleRate);
        byte[] data = out.toByteArray();
        int length = data.length / format.getSampleSizeInBits();
        ByteArrayInputStream bytestream = new ByteArrayInputStream(data);
        AudioInputStream stream = new AudioInputStream(bytestream, format, length);
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
    }
}