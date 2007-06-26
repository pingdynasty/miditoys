package com.pingdynasty.midi.bcontrol;

import javax.sound.midi.*;

public abstract class ShortMessageReceiver implements Receiver {
    protected Transmitter transmitter;

    public void setTransmitter(Transmitter transmitter){
        if(this.transmitter == transmitter)
            return;
        if(this.transmitter != null)
            this.transmitter.close();
        this.transmitter = transmitter;
        transmitter.setReceiver(this);
    }

    public void send(MidiMessage message, long time){
        if(message instanceof ShortMessage){
            try{
                send((ShortMessage)message, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
//             }else{
//                 System.out.println("midi message "+message);
//                 return;
        }
    }

    public void close(){
        if(this.transmitter != null)
            this.transmitter.close();
    }

    public abstract void send(ShortMessage msg, long time);

}
