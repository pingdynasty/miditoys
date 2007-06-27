package com.pingdynasty.midi;

import java.util.List;
import java.util.ArrayList;
import javax.sound.sampled.*;

public class AudioLineOutput extends AudioOutput {

    private FloatControl[] controls;
    private SourceDataLine line;
    private static final float MAX_VALUE = 127.0f;

    public AudioLineOutput(int samples, int mode)
        throws Exception{
        super(samples, mode);
    }

    public void openLine(float sampleRate, int buffersize)
        throws Exception{
//         sampleRate = 8000,11025,16000,22050,44100
        AudioFormat format = getAudioFormat(sampleRate);
//              new AudioFormat(sampleRate, 8, 1, true, true);
        System.out.println("format: "+format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine)AudioSystem.getLine(info);
        // setting the buffer to 4 times the data length
        // means that we may end up with audio 4 frames late
        line.open(format, buffersize);

        AudioFormat[] formats = info.getFormats();
        for(int k=0; k<formats.length; ++k)
            System.out.println("format: "+formats[k]);
        List list = new ArrayList();
        Control[] cs = line.getControls();
        for(int i=0; i<cs.length; ++i){
            System.out.println("control: "+cs[i].getType());
            if(cs[i] instanceof FloatControl)
                list.add(cs[i]);
        }
        controls = new FloatControl[list.size()];
        list.toArray(controls);
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
        line.start();
    }

    public void write(double[] values){
        super.write(values);
        // this blocks if the buffer is full (currently 4 frames)
        line.write(databuffer, 0, databuffer.length);
    }

    public int getNumberOfControls(){
        return controls.length;
    }

    public String getControlName(int index){
        if(index < controls.length)
            return controls[index].getType().toString();
        return null;
    }

    public String getControlValueString(int index){
        if(index < controls.length)
            return controls[index].getValue() + " " + controls[index].getUnits();
        return null;
    }

    public int getControlValue(int index){
        if(index < controls.length)
            return (int)((controls[index].getValue() - controls[index].getMinimum()) / (controls[index].getMaximum() - controls[index].getMinimum()) * MAX_VALUE);
        return 0;
    }
//                 scaled = ( norm * ( max - min ) ) + min
//                 norm = ( scaled - min ) / ( max - min )

    public void setControlValue(int index, int value){
        if(index < controls.length)
            controls[index].setValue((value / MAX_VALUE ) * (controls[index].getMaximum() - controls[index].getMinimum()) + controls[index].getMinimum());
    }
}