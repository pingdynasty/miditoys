package com.pingdynasty.midi;

import javax.sound.midi.*;

public class MidiSync implements Runnable, Transmitter {
    private int bpm;
    private Thread scheduler;
    private boolean running;
    private Receiver receiver; 
    private ShortMessage syncMessage;
    private long latency = 2; // assume 2ms latency by default

    public MidiSync(int bpm){
        setBPM(bpm);
        try{
            syncMessage = new ShortMessage();
            syncMessage.setMessage(ShortMessage.TIMING_CLOCK);
        }catch(InvalidMidiDataException exc){
            throw new RuntimeException(exc);
        }
    }

    public void setBPM(int bpm){
        if(bpm < 1)
            throw new IllegalArgumentException("BPM must be greater than 0");
        this.bpm = bpm;
        if(running)
            scheduler.interrupt();
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
        if(receiver != null)
            receiver.close();
        receiver = null;
    }

    public void start(){
        running = true;
        scheduler = new Thread(this);
        scheduler.setDaemon(true);
        scheduler.start();
    }

    public void stop(){
        running = false;
        scheduler.interrupt();
    }

    public void run(){
        while(running){
            int tick;
            long target;
            long period = (60000 / bpm) / 24;
            try{
                tick = 0;
                target = System.currentTimeMillis() + (60000 / bpm);
                while(++tick < 24){
                    Thread.sleep(period);
                    if(receiver != null)
                        receiver.send(syncMessage, -1);
                }
                target = target - System.currentTimeMillis();
                System.err.println("adjusting "+target+"/"+period);
                if(target > latency)
                    Thread.sleep(target - latency);
                if(receiver != null)
                    receiver.send(syncMessage, -1);
            }catch(InterruptedException exc){}
        }
    }


// count syncs sent out
// add up difference in scheduled time and actual time
// after 24 syncs, use difference average to adjust scheduling time (minus latency, eg 2ms)

//     diff += now - scheduledtime;
//     if(++counter == 24){
//         adjust = latency + (diff / 24);
//         counter = 0;
//     }
//     }
}
