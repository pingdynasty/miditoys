package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;

// a Control that sends and receives MIDI update requests
public abstract class MidiControl implements Control, Receiver {

    protected Transmitter transmitter;
    protected Receiver receiver;
    protected int command;
    protected int channel;
    protected int data1;
    protected int data2;

    protected ShortMessage msg;

    public MidiControl(int command, int channel, int data1, int data2){
        this.command = command;
        this.channel = channel;
        this.data1 = data1;
        this.data2 = data2;
        msg = new ShortMessage();
    }

    public int getValue(){
        return data2;
    }

    public void setValue(int value)
        throws InvalidMidiDataException {
        data2 = value;
        updateMidiControl();
        updateGraphicalControl();
    }

    // MIDI related - Receiver and Transmitter
    public void setReceiver(Receiver receiver){
        if(this.receiver == receiver)
            return;
        if(this.receiver != null)
            this.receiver.close();
        this.receiver = receiver;
//         synchronize();
    }

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
        }else{
            System.out.println("midi message "+message);
            return;
        }
    }

    public void send(ShortMessage msg, long time){
        System.out.println("midi sm <"+msg+"><"+time+"><"+
                           msg.getCommand()+"><"+msg.getChannel()+"><"+
                           msg.getData1()+"><"+msg.getData2()+">");
        if(command == msg.getStatus() && 
           channel == msg.getChannel() &&
           data1 == msg.getData1()){
            data2 = msg.getData2();
            System.out.println("received matching data");
            // notify graphical unit of update
            updateGraphicalControl();
        }else{
            System.out.println("no match"+
                               command+"><"+channel+"><"+
                               data1+"><"+data2+">");
        }
    }

    public void close(){
        if(this.transmitter != null)
            this.transmitter.close();
        if(this.receiver != null)
            this.receiver.close();
    }

    public MidiMessage getMidiMessage()
        throws InvalidMidiDataException {
        msg.setMessage(command, channel, data1, data2);
        System.out.println("msg: "+msg);
        return msg;
    }

    public void updateMidiControl()
        throws InvalidMidiDataException {
        if(receiver != null)
            receiver.send(getMidiMessage(), -1);
    }

    public abstract void updateGraphicalControl();

    public abstract void generateSysexMessages(List messages, int encoder)
        throws InvalidMidiDataException;
}