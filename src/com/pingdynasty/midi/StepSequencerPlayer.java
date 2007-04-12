package com.pingdynasty.midi;

import javax.sound.midi.*;

public class StepSequencerPlayer extends StepSequencer {

    private static final StepSequencer.Step norm = new StepSequencer.Step();
    private StepSequencer.Step global;

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
            player.setDuration(((range(global.duration + step.duration - norm.duration)) * period) / 64);
            player.setVelocity(range(global.velocity + step.velocity - norm.velocity));
            player.modulate(range(global.modulation + step.modulation - norm.modulation));
            player.bend(range(global.bend + step.bend - norm.bend));
            player.play(range(global.note + step.note - norm.note));
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    /**
     * Start playing the sequence with the given offset
     */
    public void start(int channel, int note, int velocity){
        player.setChannel(channel);
        global.note = note;
        global.velocity = velocity;
        start();
    }
}