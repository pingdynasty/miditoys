package com.pingdynasty.midi;

import javax.sound.midi.*;
import javax.swing.JComponent;
import javax.swing.JProgressBar;

public class StepSequencer implements Receiver {

    protected Player player;
    protected Step[] steps;
    private boolean started = false;
    public static final int DEFAULT_BPM = 120;
    private int tick;
    private MidiSync sync; // internal midi sync/scheduling thread
    private JProgressBar progress;

    public StepSequencer(Player player, int length){
        this(player, new Step[length]);
    }

    public StepSequencer(Player player, Step[] steps){
        this.player = player;
        this.steps = steps;
        sync = new MidiSync(DEFAULT_BPM);
        sync.setReceiver(this);
        progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, steps.length * 24);
        progress.setValue(0);
//         noteon = new int[steps.length * 24];
//         noteoff = new int[steps.length * 24];
        for(int i=0; i<steps.length; ++i){
            if(steps[i] == null)
                steps[i] = new Step();                
//             noteon[i] = i*24 + steps[i].getDelayTicks();
//             noteoff[i] = noteon[i] + steps[i].getDurationTicks();
        }
    }

    public void setBPM(int bpm){
        sync.setBPM(bpm);
    }

    public int getBPM(){
        return sync.getBPM();
    }

    public JComponent getProgressBar(){
        return progress;
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

//     public void setPeriod(int period){
//         this.period = period;
//     }

//     public int getPeriod(){
//         return period;
//     }

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
        }else{
            return;
        }
    }

    public void noteon(Step step){
//         System.out.println("noteon  "+step.getNote()+" \t"+tick);
        try{
            player.modulate(step.getModulation());
            player.bend(step.getBend());
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

    protected void tick(){
        if(progress != null)
            progress.setValue(tick);
        int max = steps.length * 24;
        int on;

//         noteoff = new int[]{(tick + steps[i].getDurationTicks()) % max, steps.getNote()};

        for(int i=0; i<steps.length; ++i){
            if((i*24 + steps[i].getDelayTicks()) % max == tick){
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
            int duration = (step.getDuration() * 60000 / sync.getBPM()) / 64;
            if(duration > 0){
                player.setDuration(duration);
                player.setVelocity(step.getVelocity());
                player.play(step.getNote());
            }
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    public void enableInternalSync(){
        sync.enable();
    }

    public void disableInternalSync(){
        sync.disable();
    }

    /**
     * Start endless play (until stop is called).
     */
    public void start(){
        tick = 0;
        started = true;
        sync.start();
        tick();
    }

    /**
     * Stop endless play.
     */
    public void stop(){
        started = false;
        for(int i=0; i<steps.length; ++i)
            noteoff(steps[i]);
        sync.stop();
    }
}