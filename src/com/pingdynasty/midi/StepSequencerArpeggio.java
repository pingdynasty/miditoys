package com.pingdynasty.midi;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import javax.sound.midi.*;

public class StepSequencerArpeggio implements Receiver {

    private StepSequencer sequencer;
    private Transmitter transmitter;
    private StepSequencerPlayer[] players = new StepSequencerPlayer[128];
    private final int capacity = 6; // max polyphonic players / voices
    private List resources;
    private Set taken;
    private int pos;

    public StepSequencerArpeggio(StepSequencer sequencer){
        this.sequencer = sequencer;
        resources = Collections.synchronizedList(new ArrayList());
        taken = Collections.synchronizedSet(new HashSet());
        pos = 0;
        // create Arpeggio players
        for(int i=0; i<capacity; ++i){
            StepSequencerPlayer player = new StepSequencerPlayer(sequencer);
            resources.add(player);
        }
    }

    protected synchronized int nextIndex(){
        if(pos >= resources.size())
            pos = 0;
        return pos++;
    }

    protected boolean resourceAvailable(){
        if(taken.size() < resources.size())
            return true;
        if(resources.size() == capacity)
            return false;
        try{
            grow();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected void grow(){
        if(resources.size() == capacity)
            throw new RuntimeException("maximum capacity reached");
        resources.add(new StepSequencerPlayer(sequencer));
    }

    public StepSequencerPlayer getPlayer(){
        int retries = 0;
        if(resourceAvailable()){
            Object o = resources.get(nextIndex());
            while(taken.contains(o))
                o = resources.get(nextIndex());
            taken.add(o);
            return (StepSequencerPlayer)o;
        }
        return null;
    }
    
    public void release(StepSequencerPlayer resource){
        taken.remove(resource);
    }

    public void noteon(int note, int velocity){
        if(players[note] == null)
            players[note] = getPlayer();
        if(players[note] != null){
            players[note].setBPM(sequencer.getBPM());
            players[note].start(note, velocity);
        }
    }

    public void noteoff(int note){
        if(players[note] != null){
            players[note].stop();
            release(players[note]);
            players[note] = null;
        }
    }

    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    public void send(ShortMessage msg, long time)
        throws InvalidMidiDataException {
        switch(msg.getStatus()){
            // todo: move Receiver to BCRStepSequencer, call arp.noteon()/noteoff()
        case ShortMessage.NOTE_ON:{
            int velocity = msg.getData2();
            if(velocity == 0)
                noteoff(msg.getData1());
            else
                noteon(msg.getData1(), velocity);
            break;
        }
        case ShortMessage.NOTE_OFF:{
            noteoff(msg.getData1());
            break;
        }
        }
    }

    public void tick(){
        Iterator it = taken.iterator();
        while(it.hasNext()){
            StepSequencerPlayer player = (StepSequencerPlayer)it.next();
            player.tick();
        }
    }

    public void close(){
        if(transmitter != null)
            transmitter.close();
    }

    public void setTransmitter(Transmitter transmitter){
        if(this.transmitter == transmitter)
            return;
        if(this.transmitter != null)
            this.transmitter.close();
        this.transmitter = transmitter;
        transmitter.setReceiver(this);
    }

    public void setSteps(Step[] steps){
        Iterator it = resources.iterator();
        for(StepSequencerPlayer player = (StepSequencerPlayer)it.next(); 
            it.hasNext(); player = (StepSequencerPlayer)it.next())
            player.setSteps(steps);
    }
}