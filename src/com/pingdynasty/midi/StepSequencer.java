package com.pingdynasty.midi;

import javax.sound.midi.*;
import javax.swing.JComponent;
import javax.swing.JProgressBar;

public class StepSequencer implements Runnable {

    protected Player player;
    protected Step[] steps;
    private Thread thread;
    protected int period = 500; // time between notes. 500ms == 120bpm.
    private boolean playing = false;
    private long next; // dispatch time for next note
    private static final long MARGIN = 5; // 5ms margin when dispatching notes

    private JProgressBar progress;

    public StepSequencer(Player player, int length){
        this(player, new Step[length]);
        for(int i=0; i<length; ++i)
            steps[i] = new Step();
    }

    public StepSequencer(Player player, Step[] steps){
        this.player = player;
        this.steps = steps;
        thread = new Thread(this);
        thread.start();

        progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, steps.length * 24);
        progress.setValue(0);
    }


    public JComponent getProgressBar(){
        return progress;
    }

    public boolean isStarted(){
        return playing;
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

    public void setPeriod(int period){
        this.period = period;
    }

    public int getPeriod(){
        return period;
    }

    public Step getStep(int index){
        return steps[index];
    }

    protected Step getNextStep(int index){
        if(index == steps.length)
            return steps[0];
        return steps[++index];
    }

    /**
     * Play one iteration (blocking call)
     */
    public void play(){
        long now; // = System.currentTimeMillis();
        progress.setValue(0);
        for(int i=0; i<steps.length; ++i){
            now = System.currentTimeMillis();
//             System.out.println("diff: "+(now-next));
            // todo: dispatch to player to play note at given time (system ms)
            // this will allow delays past the next step
            // a delay of 128 is one whole note
            int delay = steps[i].getDelay() * period / 128;
            if(delay > MARGIN)
                try{
                    Thread.sleep(delay);
                }catch(InterruptedException exc){
                    return;
                }
            progress.setValue(i*24);
            play(steps[i]);
            // set the delivery time for next step
            next = now + period - (now - next);
            try{
                while(now < next){
//                     System.out.println("sleep "+period+"/24="+(period/24));
                    Thread.sleep(period / 24);
                    now = System.currentTimeMillis() + MARGIN;
                    progress.setValue(progress.getValue() + 1);
                }
            }catch(InterruptedException exc){
                return;
            }
        }
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
            int duration = (step.getDuration() * period) / 64;
            if(duration > 0){
                player.setDuration(duration);
//             player.setDuration((step.getDuration() * period) / 16);
//             System.out.println(NoteParser.getStringNote(step.getNote())+" duration: "+(step.getDuration() * period) / 16 +" velocity: "+step.getVelocity());
                player.setVelocity(step.getVelocity());
                player.play(step.getNote());
            }
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    /**
     * Start endless play (until stop is called). Plays in separate thread.
     */
    public void start(){
        playing = true;
        next = System.currentTimeMillis();
        thread.interrupt();
    }

    /**
     * Stop endless play.
     */
    public void stop(){
        playing = false;
        thread.interrupt();
    }

    public void run(){
        for(;;){
            if(playing){
                play();
            }else{
                try{
                    Thread.sleep(period / 48); // sleep half a frame
                }catch(InterruptedException exc){}
            }
        }
    }
}