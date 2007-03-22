package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.util.List;
import java.util.ArrayList;

public class SchedulingPlayer extends ReceiverPlayer implements Runnable {

    private Thread scheduler;
    private List schedule;
    private boolean running = true;
    private long tick;

    public SchedulingPlayer(Receiver receiver){
        super(receiver);
        schedule = new ArrayList();
        scheduler = new Thread(this);
        scheduler.setDaemon(true);
        scheduler.start();
    }

    public void run(){
        while(running){
            if(schedule.isEmpty()){
                try{
                    Thread.currentThread().sleep(10);
                }catch(InterruptedException exc){}
            }else{
                try{
                    int note = ((Integer)schedule.remove(0)).intValue();
                    noteon(note);
                    try{
                        Thread.currentThread().sleep(duration);
                    }catch(InterruptedException exc){}
                    noteoff(note);
                }catch(InvalidMidiDataException exc){
                    exc.printStackTrace();
                }
            }
        }
    }

    /**
     * Non-blocking call to play note.
     * Returns immediately and plays the note in a different thread.
     */
    public void play(int note){
        schedule.add(new Integer(note));
    }
}
