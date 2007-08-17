package com.pingdynasty.midi;

import javax.sound.midi.*;

public class GridSequencer implements Receiver {

    public static final int DEFAULT_BPM = 120;
    private static final int TICKS_PER_BEAT = 24;
    private Receiver receiver;
    private boolean started = false;
    private int tick;
    private int[] steps;
    private int[] bits;
    private int[] notes;
//     private int[] bits = new int[]{0x0001, 0x0002, 0x0004, 0x0008, 
//                                    0x0010, 0x0020, 0x0040, 0x0080};
// //                                    0x0100, 0x0200, 0x0400, 0x0800,
// //                                    0x1000, 0x2000, 0x4000, 0x8000,
//     private int[] notes = new int[]{60, 61, 62, 63, 64, 65, 66, 67};
    private int velocity = 60;
    private ShortMessage msg = new ShortMessage();
    private int channel = 0;

    public GridSequencer(int length){
        steps = new int[length];
        bits = new int[length];
        notes = new int[length];
        for(int i=0; i<length; ++i){
            bits[i] = 0x0001 << i;
            notes[i] = 60 + i;
        }
    }

    public void setReceiver(Receiver receiver){
        this.receiver = receiver;
    }

    public boolean isStarted(){
        return started;
    }

    public int getLength(){
        return steps.length;
    }

//     public void setLength(int length){
//         int[] newsteps = new int[length];
//         for(int i=0; i<length; ++i){
//             if(i < steps.length)
//                 newsteps[i] = steps[i];
//         }
//         steps = newsteps;
//     }

    public boolean isNoteOn(int step, int note){
        assert step < steps.length;
        for(int i=0; i<notes.length; ++i)
            if(notes[i] == note)
                if((steps[step] & bits[i]) == bits[i])
                    return true;
        return false;
    }

    public void setNoteOn(int step, int note){
        assert step < steps.length;
        for(int i=0; i<notes.length; ++i)
            if(notes[i] == note)
                steps[step] |= bits[i]; // turn on note
    }

    public void setNoteOff(int step, int note){
        assert step < steps.length;
        for(int i=0; i<notes.length; ++i)
            if(notes[i] == note)
                steps[step] &= ~bits[i]; // turn off note
    }

    public void setNote(int step, int note, boolean on){
        assert step < steps.length;
        for(int i=0; i<notes.length; ++i)
            if(notes[i] == note)
                if(on)
                    steps[step] |= bits[i]; // turn on note
                else
                    steps[step] &= ~bits[i]; // turn off note
    }

    public void toggleNote(int step, int note){
        assert step < steps.length;
        for(int i=0; i<notes.length; ++i)
            if(notes[i] == note)
                steps[step] ^= bits[i];
    }

    public void noteon(int note){
//         System.out.println("noteon  "+note+" \t"+tick);
        try{
            msg.setMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
        receiver.send(msg, -1);
    }

    // note that noteoff is not normally called by the GridSequencer
    public void noteoff(int note){
//         System.out.println("noteoff "+note+" \t"+tick);
        try{
            msg.setMessage(ShortMessage.NOTE_OFF,  channel, note, 0);
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
        receiver.send(msg, -1);
    }

    protected void tick(){
        if(tick % TICKS_PER_BEAT == 0){
            int step = tick / TICKS_PER_BEAT;
            for(int i=0; i<bits.length; ++i)
                if((steps[step] & bits[i]) == bits[i])
                    noteon(notes[i]);
        }
        if(++tick == TICKS_PER_BEAT * steps.length)
            tick = 0;
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
        case ShortMessage.TIMING_CLOCK: {
            if(started)
                tick();
            break;
        }
        case ShortMessage.START: {
            start();
            break;
        }
        case ShortMessage.STOP: {
            stop();
            break;
        }
        }
    }

    public void close(){
        started = false;
        if(receiver != null)
            receiver.close();
    }

    /**
     * Start endless play (until stop is called).
     */
    public void start(){
        System.out.println("start grid "+tick);
        tick = 0;
        started = true;
        tick();
    }

    /**
     * Stop endless play.
     */
    public void stop(){
        System.out.println("stop grid "+tick);
        started = false;
        for(int i=0; i<notes.length; ++i)
            noteoff(notes[i]);
    }

    /**
     * pause or unpause play.
     */
    public void pause(boolean paused){
        started = !paused;
    }
}