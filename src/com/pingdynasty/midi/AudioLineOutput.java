package com.pingdynasty.midi;

import javax.sound.sampled.*;

public class AudioLineOutput extends AudioOutput {

    private FloatControl samplerateControl;
    private SourceDataLine line;

    public AudioLineOutput(int samples, int mode)
        throws Exception{
        super(samples, mode);
    }

    public void openLine(float sampleRate)
        throws Exception{
//         sampleRate = 8000,11025,16000,22050,44100
        AudioFormat format = getAudioFormat(sampleRate);
//              new AudioFormat(sampleRate, 8, 1, true, true);
        System.out.println("format: "+format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine)AudioSystem.getLine(info);
        // setting the buffer to 4 times the data length
        // means that we may end up with audio 4 frames late
        // todo: make buffer size configurable
        line.open(format, databuffer.length * 4);

        AudioFormat[] formats = info.getFormats();
        for(int k=0; k<formats.length; ++k)
            System.out.println("format: "+formats[k]);
        Control[] controls = line.getControls();
        for(int i=0; i<controls.length; ++i)
            System.out.println("control: "+controls[i].getType());
        Mixer.Info[] minfos = AudioSystem.getMixerInfo();
        for(int i=0; i<minfos.length; ++i){
            System.out.println("mixer: "+minfos[i]);
            Mixer mixer = AudioSystem.getMixer(minfos[i]);
            Line.Info[] linfos = mixer.getSourceLineInfo();
            for(int j=0; j<linfos.length; ++j){
                System.out.println("source line: "+linfos[j]);
                if(linfos[j] instanceof DataLine.Info){
                    info = (DataLine.Info)linfos[i];
                    formats = info.getFormats();
                    for(int k=0; k<formats.length; ++k)
                        System.out.println("format: "+formats[k]);
                }
            }
        }
        try{
            samplerateControl = (FloatControl)line.getControl(FloatControl.Type.SAMPLE_RATE);
        }catch(Exception exc){exc.printStackTrace();}
        line.start();
    }

    public void write(double[] values){
        super.write(values);
        // this blocks if the buffer is full (currently 4 frames)
        line.write(databuffer, 0, databuffer.length);
    }

    public float getSampleRate(){
        return samplerateControl.getValue();
    }

    public void setSampleRate(int samplerate){
        samplerateControl.setValue(((float)samplerate) * (samplerateControl.getMaximum() - samplerateControl.getMinimum()) / 127.0f + samplerateControl.getMinimum());
    }
}