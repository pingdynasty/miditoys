package com.pingdynasty.midi;

import javax.sound.midi.*;

public class StepSequencerPlayer extends StepSequencer {

    private static final Step norm = new Step();
    private Step global;

    public StepSequencerPlayer(StepSequencer sequencer){
        super(sequencer.player, sequencer.steps);
        global = new Step();
    }

    private int range(int value){
        if(value < 0)
            return 0;
        if(value > 127)
            return 127;
        return value;
    }

    /**
     * Play one step (blocking call).
     */
    public void play(Step step){
        // define a 'normal' step, use global +- deviation from norm
        try{
            player.setDuration(((range(global.getDuration() + step.getDuration() - norm.getDuration())) * period) / 64);
            player.setVelocity(range(global.getVelocity() + step.getVelocity() - norm.getVelocity()));
            player.modulate(range(global.getModulation() + step.getModulation() - norm.getModulation()));
            player.bend(range(global.getBend() + step.getBend() - norm.getBend()));
            player.play(range(global.getNote() + step.getNote() - norm.getNote()));
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    /**
     * Start playing the sequence with the given offset
     */
    public void start(int note, int velocity){
        global.setNote(note);
        global.setVelocity(velocity);
        start();
    }
}