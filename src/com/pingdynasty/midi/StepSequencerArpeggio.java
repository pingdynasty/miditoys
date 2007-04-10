package com.pingdynasty.midi;

import javax.sound.midi.*;

public class StepSequencerArpeggio extends StepSequencer {

    private StepSequencer.Step global;

    public StepSequencerArpeggio(StepSequencer sequencer){
        super(sequencer.player, sequencer.steps);
        global = new Step();
        global.note = 0;
        global.velocity = 0;
        global.duration = 0;
        global.modulation = 0;
        global.bend = 0;
    }

    // todo: find better name
    public StepSequencer.Step getGlobal(){
        return global;
    }

    /**
     * Play one step (blocking call).
     */
    public void play(Step step){
        try{
            player.setDuration(((step.duration + global.duration) * period) / 64);
            player.setVelocity(step.velocity + global.velocity);
            player.modulate(step.modulation + global.modulation);
            player.bend(step.bend + global.bend);
            player.play(step.note + global.note);
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    /**
     * Start playing the sequence with the given offset
     */
    public void start(int note){
        global.note = note;
        start();
    }
}