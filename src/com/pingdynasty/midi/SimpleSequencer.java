package com.pingdynasty.midi;

import javax.sound.midi.*;

public class SimpleSequencer implements Receiver {

    public static final int DEFAULT_BPM = 120;
    private static final int TICKS_PER_BEAT = 24;
    private Receiver receiver;
    private Step[] steps;
    private boolean started = false;
    private int tick;
    private ShortMessage msg = new ShortMessage();
    private int channel = 0;

    public SimpleSequencer(int length){
        this(new Step[length]);
    }

    public SimpleSequencer(Step[] steps){
        this.steps = steps;
        for(int i=0; i<steps.length; ++i){
            if(steps[i] == null)
                steps[i] = new Step();                
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

    public void setLength(int length){
        Step[] newsteps = new Step[length];
        for(int i=0; i<length; ++i){
            if(i < steps.length)
                newsteps[i] = steps[i];
            else
                newsteps[i] = new Step();
        }
        steps = newsteps;
    }

    public Step getStep(int index){
        return steps[index];
    }

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

    public void noteon(Step step){
        System.out.println("noteon  "+step.getNote()+" \t"+tick);
        try{
            msg.setMessage(ShortMessage.NOTE_ON,  channel, step.getNote(), step.getVelocity());
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
        receiver.send(msg, -1);
    }

    public void noteoff(Step step){
        System.out.println("noteoff "+step.getLastNote()+" \t"+tick);
        try{
            msg.setMessage(ShortMessage.NOTE_OFF,  channel, step.getLastNote(), 0);
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
        receiver.send(msg, -1);
    }

    protected void tick(){
        int max = steps.length * TICKS_PER_BEAT;
        for(int i=0; i<steps.length; ++i){
            if((i*TICKS_PER_BEAT + steps[i].getDelayTicks()) % max == tick){
                if(steps[i].getDuration() > 0){
                    steps[i].setNoteOffTick(tick + steps[i].getDurationTicks());
                    noteon(steps[i]);
                }
            }else if(steps[i].getNoteOffTick() % max == tick){
                noteoff(steps[i]);
            }
        }
        if(++tick == max)
            tick = 0;
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
        tick = 0;
        started = true;
        tick();
    }

    /**
     * Stop endless play.
     */
    public void stop(){
        started = false;
        for(int i=0; i<steps.length; ++i)
            noteoff(steps[i]);
    }
}