package com.pingdynasty.midi;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.sound.midi.*;

public class StepSequencerArpeggio implements Receiver {

    private StepSequencer sequencer;
    private Transmitter transmitter;
    private StepSequencerPlayer[] players = new StepSequencerPlayer[128];

    private final int capacity = 4; // max polyphonic players
    private List resources;
    private Set taken;
    private int pos;

    public StepSequencerArpeggio(StepSequencer sequencer){
        this.sequencer = sequencer;
        resources = Collections.synchronizedList(new ArrayList());
        taken = Collections.synchronizedSet(new HashSet());
        pos = 0;
        // create Arpeggio players
        for(int i=0; i<capacity; ++i)
            resources.add(new StepSequencerPlayer(sequencer));
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


    public void setTransmitter(Transmitter transmitter){
        if(this.transmitter == transmitter)
            return;
        if(this.transmitter != null)
            this.transmitter.close();
        this.transmitter = transmitter;
        transmitter.setReceiver(this);
    }

    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }else{
            System.out.println("midi message "+msg);
            return;
        }
    }

    public void send(ShortMessage msg, long time)
        throws InvalidMidiDataException {
        System.out.println("midi in <"+msg.getStatus()+"><"+msg.getCommand()+"><"+
                           msg.getData1()+"><"+msg.getData2()+">");
        switch(msg.getStatus()){
        case ShortMessage.NOTE_ON:{
            int note = msg.getData1();
            if(players[note] == null)
                players[note] = getPlayer();
            if(players[note] != null){
                players[note].setPeriod(sequencer.getPeriod());
                players[note].start(msg.getChannel(), note, msg.getData2());
            }
            break;
        }
        case ShortMessage.NOTE_OFF:{
            int note = msg.getData1();
            if(players[note] != null){
                players[note].stop();
                release(players[note]);
                players[note] = null;
            }
            break;
        }
        default:
        }
    }

    public void close(){
        if(transmitter != null)
            transmitter.close();
    }
}