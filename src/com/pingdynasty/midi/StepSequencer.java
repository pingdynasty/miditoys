package com.pingdynasty.midi;

import javax.sound.midi.*;

public class StepSequencer implements Runnable {

    protected Player player;
    protected Step[] steps;
    private Thread thread;
    protected int period = 500; // time between notes. 500ms == 120bpm.
//     private int acceleration;
    private boolean playing = false;

    public static class Step {
        int note = 60;
        int velocity = 80;
        int duration = 80;
        int modulation = 0;
        int bend = 64;
//         public Step(int note, int velocity, int duration, 
//                     int modulation, int step){
//             this.note = note;
//             this.velocity = velocity;
//             this.duration = duration;
//             this.modulation = modulation;
//             this.step = step;
//         }
    }

    public StepSequencer(Player player, int length){
        this.player = player;
        steps = new Step[0];
        setLength(length);
        thread = new Thread(this);
        thread.start();
    }

    protected StepSequencer(Player player, Step[] steps){
        this.player = player;
        this.steps = steps;
        thread = new Thread(this);
        thread.start();
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

    /**
     * Play one iteration (blocking call)
     */
    public void play(){
//         int delay = period;
        for(int i=0; i<steps.length; ++i){
            play(steps[i]);
            try{
                for(int j=0; j<24; ++j)
                    Thread.sleep(period / 24);
                Thread.sleep(period % 24);
            }catch(InterruptedException exc){
                return;
            }
//             if(acceleration != 0)
//                 delay += acceleration;
        }
    }

    /**
     * Play one step (blocking call). Todo: shouldn't block.
     */
    public void play(Step step){
        try{
            // duration : a value of 64 should be 1/2 note:
            // (duration * period) / (64 * 4)
//             player.setDuration((step.duration * period) / 256);
            player.setDuration((step.duration * period) / 64);
//             player.setDuration((step.duration * period) / 16);
            System.out.println(NoteParser.getStringNote(step.note)+" duration: "+(step.duration * period) / 16);
            player.setVelocity(step.velocity);
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
                    Thread.sleep(period / 48); // sleep half a frame
                }catch(InterruptedException exc){}
            }
        }
    }
    
    public static void main(String[] args){
        for(int i=0; i<args.length; ++i);
    }
}