package com.pingdynasty.midi;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.sound.midi.*;

public class StepSequencerArpeggio implements Receiver {

    private Timer timer = new Timer(); // for midi sync messages

    private StepSequencer sequencer;
    private Transmitter transmitter;
    private StepSequencerPlayer[] players = new StepSequencerPlayer[128];

    private final int capacity = 4; // max polyphonic players
    private List resources;
    private Set taken;
    private int pos;

    public class Timer {
        boolean syncing = false;
//         boolean timing = false; // true after first sync message is received
        int timing = 0; // number of ticks we've received so far
        long tick;
        public void start(){
            System.out.println("starting midi sync");
            syncing = true;
            timing = 0;
        }
        public void reset(){
            timing = 0;
        }
        public void update(){
            if(!syncing)
                return;
            switch(timing++){
            case 0:
                tick = System.currentTimeMillis();
                break;
            case 24:
                long now = System.currentTimeMillis();
                int period = (int)(now - tick);
                sequencer.setPeriod(period);
                System.out.println("period: "+period+" bpm: "+60000/period);
                timing = 0;
                break;
            default:
            }
        }
        public void stop(){
            System.out.println("stopping midi sync");
            syncing = false;
        }
    }

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

    public void noteon(int channel, int note, int velocity){
        if(players[note] == null)
            players[note] = getPlayer();
        if(players[note] != null){
            players[note].setPeriod(sequencer.getPeriod());
            players[note].start(channel, note, velocity);
        }
    }

    public void noteoff(int note){
        if(players[note] != null){
            players[note].stop();
            release(players[note]);
            players[note] = null;
        }
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
        switch(msg.getStatus()){
        case ShortMessage.START: {
            // this should send a start callback etc to BCR
            System.out.println("arp start");
            timer.reset();
            sequencer.start();
            break;
        }
        case ShortMessage.STOP: {
            // this should send a stop callback etc to BCR
            System.out.println("arp stop");
            sequencer.stop();
            break;
        }
            // these should send note on/off messages to BCR
            // todo: move Receiver to BCRStepSequencer, call arp.noteon()/noteoff()
        case ShortMessage.NOTE_ON:{
            System.out.println("arp note on <"+msg.getData1()+"><"+msg.getData2());
            noteon(msg.getChannel(), msg.getData1(), msg.getData2());
            break;
        }
        case ShortMessage.NOTE_OFF:{
            System.out.println("arp note off <"+msg.getData1()+"><"+msg.getData2());
            noteoff(msg.getData1());
            break;
        }
        case ShortMessage.TIMING_CLOCK: {
            timer.update();
            break;
            }
        default:
            System.out.println("arp midi in <"+msg.getStatus()+"><"+msg.getCommand()+"><"+
                               msg.getData1()+"><"+msg.getData2()+">");
        }
    }

    public void close(){
        if(transmitter != null)
            transmitter.close();
    }

    public void setMidiSync(boolean doSync){
        if(doSync)
            timer.start();
        else
            timer.stop();
    }
}