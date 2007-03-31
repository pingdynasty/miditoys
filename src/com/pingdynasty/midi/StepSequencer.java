package com.pingdynasty.midi;

import javax.sound.midi.*;

// todo: check if pong applet on Firefox delays without using Scheduling Player.
public class StepSequencer implements Runnable {

    private Player player;
    private Step[] steps;
    private Thread thread;
    private int period = 500; // time between notes. 500ms == 120bpm.
    private int acceleration;
    private boolean playing = false;

    public class Step {
        int note = 60;
        int velocity = 80;
        int duration = 80;
        int modulation = 0;
        int bend = 64;
    }

    public StepSequencer(Player player){
        this(player, 8);
    }

    public StepSequencer(Player player, int length){
        this.player = player;
        steps = new Step[0];
        setLength(length);
        thread = new Thread(this);
        thread.start();
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

    /**
     * Play one iteration (blocking call)
     */
    public void play(){
        int delay = period;
        for(int i=0; i<steps.length; ++i){
            play(steps[i]);
            try{
                Thread.sleep(delay);
            }catch(InterruptedException exc){
                return;
            }
            if(acceleration != 0)
                delay += acceleration;
        }
    }

    /**
     * Play one step (blocking call). Todo: shouldn't block.
     */
    public void play(Step step){
        try{
            player.setDuration(step.duration);
            player.setVelocity(step.duration);
            player.modulate(step.modulation);
            player.bend(step.bend);
            player.play(step.note);
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    /**
     * Start endless play (until stop is called). Plays in separate thread.
     */
    public void start(){
        playing = true;
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
                    Thread.sleep(100);
                }catch(InterruptedException exc){}
            }
        }
    }
    
    public static void main(String[] args){
        for(int i=0; i<args.length; ++i);
    }
}