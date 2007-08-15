package com.pingdynasty.midi;

import javax.sound.midi.*;

public class StepSequencer implements Receiver {

    protected Player player;
    protected Step[] steps;
    private boolean started = false;
    private int tick;
    private int bpm = DEFAULT_BPM;
    public static final int DEFAULT_BPM = 120;
    private static final int TICKS_PER_BEAT = 24;

    public StepSequencer(Player player, int length){
        this(player, new Step[length]);
    }

    public StepSequencer(Player player, Step[] steps){
        this.player = player;
        this.steps = steps;
        for(int i=0; i<steps.length; ++i){
            if(steps[i] == null)
                steps[i] = new Step();                
        }
    }

    public void setBPM(int bpm){
        this.bpm = bpm;
    }

    public int getBPM(){
        return bpm;
    }

    public Step[] getSteps(){
        return steps;
    }

    public void setSteps(Step[] steps){
        this.steps = steps;
    }

    public boolean isStarted(){
        return started;
    }

    public int getLength(){
        return steps.length;
    }

    public void setPlayer(Player player){
        this.player = player;
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

//     protected Step getNextStep(int index){
//         if(index == steps.length)
//             return steps[0];
//         return steps[++index];
//     }
    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    public void noteon(Step step){
//         System.out.println("noteon  "+step.getNote()+" \t"+tick);
        try{
            player.modulate(step.getModulation());
            // Step bend is in the range 0-127
            player.bend(step.getBend() * 128);
            player.setVelocity(step.getVelocity());
            player.noteon(step.getNote());
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    public void noteoff(Step step){
//         System.out.println("noteoff "+step.getLastNote()+" \t"+tick);
        try{
            player.noteoff(step.getLastNote());
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    public int getStepPosition(){
        return tick / TICKS_PER_BEAT;
    }

    public void setStepPosition(int index){
        assert index < steps.length;
        tick = index * TICKS_PER_BEAT;
    }

    public void tick(){
        if(!started)
            return;
//         if(progress != null)
//             progress.setValue(tick);
        int max = steps.length * TICKS_PER_BEAT;
        int on;

//         noteoff = new int[]{(tick + steps[i].getDurationTicks()) % max, steps.getNote()};

        for(int i=0; i<steps.length; ++i){
            if((i*TICKS_PER_BEAT + steps[i].getDelayTicks()) % max == tick){
                if(steps[i].getDuration() > 0){
                    steps[i].setNoteOffTick(tick + steps[i].getDurationTicks());
                    noteon(steps[i]);
                }
            }else if(steps[i].getNoteOffTick() % max == tick){
                noteoff(steps[i]);
            }
//             if(steps[i].getNoteOnTick() == tick)
//                 noteon(steps[i]);
//             if(steps[i].getNoteOffTick() == tick)
//                 noteoff(steps[i]);
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
    }

    /**
     * Play one step.
     */
    public void play(Step step){
        try{
            // duration : a value of 64 should be 1/2 note:
            // (duration * period) / (64 * 4)
//             player.setDuration((step.getDuration() * period) / 256);
            player.modulate(step.getModulation());
            player.bend(step.getBend());
            int duration = (step.getDuration() * 60000 / bpm) / 64;
            if(duration > 0){
                player.setDuration(duration);
                player.setVelocity(step.getVelocity());
                player.play(step.getNote());
            }
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    /**
     * Start endless play (until stop is called).
     */
    public void start(){
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