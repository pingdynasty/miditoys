package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.Collections;

/**
 * Single thread implementation of polyphonic scheduling of notes of any duration,
 * ie new notes can be scheduled and started before the old note(s) have been stopped.
 */
public class SchedulingPlayer extends ReceiverPlayer implements Runnable, Comparator {

    private Thread scheduler;
    private SortedSet schedule;
    private boolean running = true;
    private long tick;

    public int compare(Object lhs, Object rhs){
        MidiEvent levent = (MidiEvent)lhs;
        MidiEvent revent = (MidiEvent)rhs;
        if(levent.getTick() < revent.getTick())
            return -1;
        else if(levent.getTick() > revent.getTick())
            return 1;
        return 0;
    }

    public SchedulingPlayer(Receiver receiver){
        super(receiver);
        schedule = Collections.synchronizedSortedSet(new TreeSet(this));
        tick = System.currentTimeMillis();
        scheduler = new Thread(this);
        scheduler.setDaemon(true);
        scheduler.start();
    }

    public void run(){
        while(running){
            if(schedule.isEmpty()){
                sleep(24);
            }else{
                MidiEvent event = (MidiEvent)schedule.first();
                MidiMessage msg = event.getMessage();
                tick = System.currentTimeMillis();
                if(event.getTick() > tick + 5){
                    // scheduling margin of 5ms
                    sleep(event.getTick() - tick);
                }else{
                    // We only remove the event from the schedule here.
                    // In case an earlier event has been scheduled while
                    // we were sleeping we would now be processing 
                    // a different event from the one we went to sleep for.
                    schedule.remove(event);
//                     try{
                        receiver.send(msg, -1);
//                     }catch(InvalidMidiDataException exc){
//                         exc.printStackTrace();
//                     }
                }
            }
        }
    }

    /**
     * Non-blocking call to play note.
     * Returns immediately and plays the note in a different thread.
     */
    public void play(int note){
        try{
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
            MidiEvent event = new MidiEvent(msg, -1);
            schedule.add(event);
            msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_OFF,  channel, note, velocity);
            tick = System.currentTimeMillis();
            event = new MidiEvent(msg, tick + duration);
            schedule.add(event);
            scheduler.interrupt();
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    public void sleep(long millis){
        try{
            Thread.sleep(millis);
        }catch(InterruptedException exc){}
        tick = System.currentTimeMillis();
    }
}
