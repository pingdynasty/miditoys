package com.pingdynasty.midi;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import javax.sound.midi.*;
import javax.sound.sampled.*;

public class BeatSlicer implements Receiver {

    private static final int NOTE_OFFSET = 60;
    private Slice[] slices;

    // alternative: subclass AudioInputStream to play only slice, use Clip to play stream.
    public class Slice implements LineListener {
        private int offset;
        private int len;
        private byte[] data;
        private SourceDataLine line;
        private int framesize;
        private int start;
        private int length;
        private boolean looping;
        private FloatControl[] controls;
        private static final int MAX_INT_VALUE = 127;
        private static final float MAX_FLOAT_VALUE = 127.0f;

        public Slice(int start, int length){
            this.start = start;
            this.length = length;
            data = new byte[0];
        }

        public void setData(byte[] data, SourceDataLine line){
            this.data = data;
            this.line = line;
            framesize = line.getFormat().getFrameSize();
            setStart(start);
            setLength(length);

            // get the mixer controls
            List list = new ArrayList();
            Control[] cs = line.getControls();
            for(int i=0; i<cs.length; ++i){
                System.out.println("control: "+cs[i].getType());
                if(cs[i] instanceof FloatControl)
                    list.add(cs[i]);
            }
            controls = new FloatControl[list.size()];
            list.toArray(controls);
        }

        public void update(LineEvent event){
            System.out.println("line event: "+event);
            //         if(event.getType().equals(LineEvent.Type.STOP)){
            //             stop();
            //         }else if(event.getType().equals(LineEvent.Type.CLOSE)){
            //             close();
            //         }
        }

        // start playing the clip from the start position
        public void start(){
            line.flush();
            line.start();
            line.write(data, offset, len);
        }

        public void stop(){
            looping = false;
            line.stop();
        }

        public void play(){
            start();
        }

        public void loop(){
            looping = true;
            start();
        }

        public void close(){
            line.close();
        }

        public void retrigger(){
            //             if(line.isRunning()){
            //             if(line.isActive()){
            if(looping){
                line.stop();
                start();
            }
        }

        public int getStart(){
            return start;
        }

        public int getLength(){
            return length;
        }

        public int getByteOffset(){
            return offset;
        }

        public int getByteLength(){
            return len;
        }

        /** Set start position of slice to a value between 0 (beginning of clip)
         *  and MAX_INT_VALUE (end of clip). 
         */
        public void setStart(int start){
            this.start = start;
            offset = (((data.length / framesize) * start) / MAX_INT_VALUE) * framesize;
            len = (((data.length / framesize) * length) / MAX_INT_VALUE) * framesize;
            if(offset + len > data.length)
                len = data.length - offset;
            retrigger();
        }

        /** Set length of slice to a value between 0 (beginning of slice)
         *  and MAX_INT_VALUE (end of slice). 
         */
        public void setLength(int length){
            this.length = length;
            len = (((data.length / framesize) * length) / MAX_INT_VALUE) * framesize;
            if(offset + len > data.length)
                len = data.length - offset;
            retrigger();
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
                return (int)((controls[index].getValue() - controls[index].getMinimum()) / (controls[index].getMaximum() - controls[index].getMinimum()) * MAX_FLOAT_VALUE);
            return 0;
        }

        public void setControlValue(int index, int value){
            if(index < controls.length)
                controls[index].setValue((value / MAX_FLOAT_VALUE) * (controls[index].getMaximum() - controls[index].getMinimum()) + controls[index].getMinimum());
        }

        public byte[] getData(){
            return data;
        }

        public AudioFormat getAudioFormat(){
            return line.getFormat();
        }
    }

    public BeatSlicer(int length)
        throws Exception {
        slices = new Slice[length];
        for(int i=0; i<length; ++i)
            slices[i] = new Slice(i * Slice.MAX_INT_VALUE / length, Slice.MAX_INT_VALUE / length);
    }

    public void loadSample(URL url)
        throws Exception {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
        if(audioInputStream == null)
            throw new IllegalArgumentException("invalid sound file URL "+url);
        loadSample(audioInputStream);
    }

    public void loadSample(File file)
        throws Exception {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        if(audioInputStream == null)
            throw new IllegalArgumentException("invalid sound file "+file.getName());
        loadSample(audioInputStream);
    }

    public void loadSample(AudioInputStream audioInputStream)
        throws Exception {
        AudioFormat format = audioInputStream.getFormat();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        //         int offset = 0;
        for(int len = audioInputStream.read(buf); len > 0;
            len = audioInputStream.read(buf)){
            outputStream.write(buf, 0, len);
            //             offset += len;
        }
        byte[] data = outputStream.toByteArray();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        for(int i=0; i<slices.length; ++i){
            SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
            line.open(format, data.length); // use total sample size as buffer size
            slices[i].setData(data, line);
        }
    }

    public Slice getSlice(int pos){
        assert pos < slices.length : "invalid slice number: "+pos;
        return slices[pos];
    }

    public void play(int pos)
        throws Exception{
        assert pos < slices.length : "invalid slice number: "+pos;
        slices[pos].play();
    }

    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    public void send(ShortMessage msg, long time)
        throws InvalidMidiDataException {
        switch(msg.getStatus()){
        case ShortMessage.NOTE_ON:{
            int slice = msg.getData1() - NOTE_OFFSET;
            //             System.out.println("note on: "+msg.getData1());
            if(slice >= 0 && slice < slices.length)
                if(msg.getData2() == 0)
                    slices[slice].stop();
                else
                    slices[slice].start();
            break;
        }
        case ShortMessage.NOTE_OFF:{
            int slice = msg.getData1() - NOTE_OFFSET;
            //             System.out.println("note off: "+msg.getData1());
            if(slice >= 0 && slice < slices.length)
                slices[slice].stop();
            break;
        }
        }
    }

    public void close(){
        for(int i=0; i<slices.length; ++i)
            slices[i].close();
    }

    //     public static void main(String[] args)
    //         throws Exception{
    //         if(args.length != 2){
    //             System.out.println("BeatSlicer: usage:");
    //             System.out.println("\tjava BeatSlicer <soundfile> <#loops>");
    //         }else{
    //             File	clipFile = new File(args[0]);
    //             int		nLoopCount = Integer.parseInt(args[1]);
    //             BeatSlicer	clipPlayer = new BeatSlicer(clipFile, nLoopCount);
    //         }
    //     }
}