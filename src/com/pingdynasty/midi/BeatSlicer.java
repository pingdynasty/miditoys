package com.pingdynasty.midi;

import java.io.File;
import java.io.IOException;
import javax.sound.midi.*;
import javax.sound.sampled.*;

public class BeatSlicer implements Receiver {

    private Slice[] slices;
    private int tick;

    public class Slice implements LineListener {
        private int start;
        private int length;
        private int startframe;
        private int endframe;
        public static final int MAX_VALUE = 127;
        private Clip clip;

        public Slice(Clip clip) {
            this.clip = clip;
            setStart(0);
            setLength(MAX_VALUE);
            System.out.println("frames: "+clip.getFrameLength());
            clip.addLineListener(this);
        }

    public void update(LineEvent event){
        System.out.println("line event: "+event);
        if(event.getType().equals(LineEvent.Type.STOP)){
            stop();
//             for(int i=0; i<slices.length; ++i)
//                 slices[i].stop();
        }else if(event.getType().equals(LineEvent.Type.CLOSE)){
            close();
//             for(int i=0; i<slices.length; ++i)
//                 slices[i].close();
            /*
             *	There is a bug in the jdk1.3/1.4.
             *	It prevents correct termination of the VM.
             *	So we have to exit ourselves.
             */
            System.out.println("Asking to do system exit!");
            // System.exit(0);
        }
    }

//         private void updateClip(){
//             clip.setLoopPoints(startframe, endframe);
//             clip.setFramePosition(startframe);
//         }

        // start playing the clip from the start position
        public void start(){
            clip.setFramePosition(startframe);
            clip.start();
        }

        public void play(){
            clip.setFramePosition(startframe);
            clip.loop(1);
        }

        public void loop(){
            clip.setFramePosition(startframe);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        public void stop(){
            clip.stop();
        }

        public void close(){
            clip.close();
        }

//         public void reset(){
//             clip.setFramePosition(startframe);
//         }

        public void retrigger(){
            clip.setFramePosition(startframe);
            if(clip.isActive()){
//             if(clip.isRunning()){
                clip.stop();
//                 clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.loop(1);
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
            int frames = clip.getFrameLength();
            startframe = (frames * start) / MAX_VALUE;
            endframe = (frames * length) / MAX_VALUE + startframe;
            if(endframe > frames)
                endframe = frames;
            clip.setLoopPoints(startframe, endframe);
//             if(clip.isActive())
                retrigger();
//             clip.setFramePosition(startframe);
        }

        /** Set length of slice to a value between 0 (beginning of slice)
         *  and MAX_VALUE (end of slice). 
         */
        public void setLength(int length){
            this.length = length;
            int frames = clip.getFrameLength();
            endframe = (frames * length) / MAX_VALUE + startframe;
            if(endframe > frames)
                endframe = frames;
            clip.setLoopPoints(startframe, endframe);
//             if(clip.isActive())
                retrigger();
//             System.out.println("frames "+startframe+"-"+endframe);
//             clip.setFramePosition(startframe);
        }
    }

    public BeatSlicer(File file, int length)
        throws Exception {
        slices = new Slice[length];
        for(int i=0; i<length; ++i){
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            if(audioInputStream == null)
                throw new IllegalArgumentException("invalid sound file "+file.getName());
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip)AudioSystem.getLine(info);
            clip.open(audioInputStream);
            slices[i] = new Slice(clip);
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
        case ShortMessage.TIMING_CLOCK: {
            if(++tick == 24){
                for(int i=0; i<slices.length; ++i)
                    slices[i].retrigger();
                System.err.println("retriggered");
                tick = 0;
            }
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