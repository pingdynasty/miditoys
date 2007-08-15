package com.pingdynasty.midi;

import javax.sound.midi.*;

public class MidiSync implements Runnable, Transmitter {
    private int bpm;
    private Thread scheduler;
    private boolean running;
    private Receiver receiver; 
    private ShortMessage syncMessage;
    private long latency = 4; // assume 4ms latency by default
    private boolean enabled = true;
    private static final int TICKS_PER_BEAT = 24;

    public MidiSync(int bpm){
        setBPM(bpm);
        try{
            syncMessage = new ShortMessage();
            syncMessage.setMessage(ShortMessage.TIMING_CLOCK);
        }catch(InvalidMidiDataException exc){
            throw new RuntimeException(exc);
        }
    }

    public void disable(){
        stop();
        enabled = false;
    }

    public void enable(){
        enabled = true;
    }

    public int getBPM(){
        return bpm;
    }

    public void setBPM(int bpm){
        if(bpm < 1)
            throw new IllegalArgumentException("BPM must be greater than 0");
        this.bpm = bpm;
//         if(scheduler != null)
//             scheduler.interrupt();
// throws exception when called as applet.
    }

    public void setReceiver(Receiver receiver){
        if(this.receiver == receiver)
            return;
        if(this.receiver != null)
            this.receiver.close();
        this.receiver = receiver;
    }

    public Receiver getReceiver(){
        return receiver;
    }

    public void close(){
        stop();
        receiver = null;
    }

    public boolean isOpen(){
        return receiver != null;
    }

    public void start(){
        if(enabled && !running){
            running = true;
            scheduler = new Thread(this);
            scheduler.setDaemon(true);
            scheduler.start();
        }
    }

    public void stop(){
        running = false;
//         if(scheduler != null)
//             scheduler.interrupt();
// throws exception when called as applet.
    }

    public void run(){
        while(running){
            int tick;
            long target;
            long period = (60000 / bpm - latency) / TICKS_PER_BEAT;
            try{
                tick = 0;
                target = System.currentTimeMillis() + (60000 / bpm);
                while(++tick < TICKS_PER_BEAT){
                    Thread.sleep(period);
                    if(receiver != null)
                        receiver.send(syncMessage, -1);
                }
                target = target - System.currentTimeMillis();
//                 System.err.println("adjusting "+target+"/"+period);
                if(target > latency)
                    Thread.sleep(target - latency);
                if(receiver != null)
                    receiver.send(syncMessage, -1);
            }catch(InterruptedException exc){}
        }
    }


// count syncs sent out
// add up difference in scheduled time and actual time
// after TICKS_PER_BEAT syncs, use difference average to adjust scheduling time (minus latency, eg 2ms)

//     diff += now - scheduledtime;
//     if(++counter == TICKS_PER_BEAT){
//         adjust = latency + (diff / TICKS_PER_BEAT);
//         counter = 0;
//     }
//     }
}
