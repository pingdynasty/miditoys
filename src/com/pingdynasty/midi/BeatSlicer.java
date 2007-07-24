package com.pingdynasty.midi;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
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
        private int volume;
        private FloatControl volumeControl;
        private int pan;
        private FloatControl panControl;
        private int samplerate;
        private FloatControl samplerateControl;
//         private int ramp;
        private boolean looping;

        public static final int MAX_VALUE = 127;

        public Slice(int start, int length){
            this.start = start;
            this.length = length;
            volume = 108; // 108 is equivalent of 0-level Master Gain
            pan = 63; // center stereo pan
            data = new byte[0];
        }

        public void setData(byte[] data, SourceDataLine line){
            this.data = data;
            this.line = line;
            framesize = line.getFormat().getFrameSize();
            setStart(start);
            setLength(length);
            try{
                volumeControl = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
            }catch(IllegalArgumentException exc){
                volumeControl = (FloatControl)line.getControl(FloatControl.Type.VOLUME);
            }
            panControl = (FloatControl)line.getControl(FloatControl.Type.PAN);
            samplerateControl = (FloatControl)line.getControl(FloatControl.Type.SAMPLE_RATE);
            samplerate = (int)(((samplerateControl.getValue() - samplerateControl.getMinimum()) / (samplerateControl.getMaximum() - samplerateControl.getMinimum())) * 127.0f);
//             if(volume == -1)
//                 volume = (int)(((volumeControl.getValue() - volumeControl.getMinimum()) / (volumeControl.getMaximum() - volumeControl.getMinimum())) * 127.0f);
//             else
//                 setVolume(volume);
//             System.out.println("volume "+volume+"/"+volumeControl.getValue());
//             setVolume(MAX_VALUE);
//             line.addLineListener(this);
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
//             if(ramp > 0)
//                 volumeControl.shift(0, ((float)volume) * (volumeControl.getMaximum() - volumeControl.getMinimum()) / 127.0f + volumeControl.getMinimum(), ramp * 100);
            // todo: FloatControl shift implementation does not work, implement with separate thread
            line.start();
            line.write(data, offset, len);
//             line.drain();
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

        /** Set start position of slice to a value between 0 (beginning of clip)
         *  and MAX_VALUE (end of clip). 
         */
        public void setStart(int start){
            this.start = start;
            offset = (((data.length / framesize) * start) / MAX_VALUE) * framesize;
            len = (((data.length / framesize) * length) / MAX_VALUE) * framesize;
            if(offset + len > data.length)
                len = data.length - offset;
            retrigger();
        }

        /** Set length of slice to a value between 0 (beginning of slice)
         *  and MAX_VALUE (end of slice). 
         */
        public void setLength(int length){
            this.length = length;
            len = (((data.length / framesize) * length) / MAX_VALUE) * framesize;
            if(offset + len > data.length)
                len = data.length - offset;
            retrigger();
        }

        public void setVolume(int volume){
            this.volume = volume;
            if(volumeControl != null)
                volumeControl.setValue(((float)volume) * (volumeControl.getMaximum() - volumeControl.getMinimum()) / 127.0f + volumeControl.getMinimum());
        }

        public int getVolume(){
            return volume;
        }

        public void setPan(int pan){
            this.pan = pan;
            if(panControl != null)
                panControl.setValue(((float)pan) * (panControl.getMaximum() - panControl.getMinimum()) / 127.0f + panControl.getMinimum());
        }

        public int getPan(){
            return pan;
        }

        public void setSampleRate(int samplerate){
            this.samplerate = samplerate;
            if(samplerateControl != null)
                samplerateControl.setValue(((float)samplerate) * (samplerateControl.getMaximum() - samplerateControl.getMinimum()) / 127.0f + samplerateControl.getMinimum());
        }

        public int getSampleRate(){
            return samplerate;
        }

//         public void setRamp(int ramp){
//             this.ramp = ramp;
//         }

//         public int getRamp(){
//             return ramp;
//         }

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
            slices[i] = new Slice(i * Slice.MAX_VALUE / length, Slice.MAX_VALUE / length);
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
//             AudioFormat format = audioInputStream.getFormat();
//             Clip clip = (Clip)AudioSystem.getLine(info);
//             clip.open(audioInputStream);
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

//     public void setTransmitter(Transmitter transmitter){
//         if(this.transmitter == transmitter)
//             return;
//         if(this.transmitter != null)
//             this.transmitter.close();
//         this.transmitter = transmitter;
//         transmitter.setReceiver(this);
//     }

    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }else{
            return;
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