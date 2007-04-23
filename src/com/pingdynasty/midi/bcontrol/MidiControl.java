package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import javax.swing.JComponent;

// a Control that sends and receives MIDI update requests
public abstract class MidiControl implements Control, Receiver {

    protected Callback callback;
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

    public void setCallback(Callback callback){
        this.callback = callback;
    }

    public int getCommand(){
        return command;
    }

    public int getChannel(){
        return channel;
    }

    public int getData1(){
        return data1;
    }

    public int getData2(){
        return data2;
    }

    public int getValue(){
        return data2;
    }

    // nb: calls updateMidiControl() and updateGraphicalControl() 
    // nb: but does not call callback()
    public void setValue(int value)
        throws InvalidMidiDataException {
        data2 = value;
//         callback();
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
//             System.out.println("midi message "+message);
            return;
        }
    }

    public void send(ShortMessage msg, long time){
//         System.out.println("midi sm <"+msg.getCommand()+"><"+msg.getChannel()+"><"+
//                            msg.getData1()+"><"+msg.getData2()+">");
        // todo: remove channel == 0 check, it's really MIDI channel 1
        if(command == msg.getStatus() && 
           (channel == msg.getChannel() || msg.getChannel() == 0) &&
           data1 == msg.getData1()){
            data2 = msg.getData2();
            // notify callback
            callback();
            // notify graphical unit of update
            updateGraphicalControl();
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
//         msg.setMessage(command, channel, data1, data2);
        // note: BCR only responds when channel is set to 0
        msg.setMessage(command, 0, data1, data2);
        return msg;
    }

    public void updateMidiControl()
        throws InvalidMidiDataException {
        if(receiver != null)
            receiver.send(getMidiMessage(), -1);
    }

    protected void callback(){
        if(callback != null)
            callback.action(command, channel, data1, data2);
    }

    public abstract void updateGraphicalControl();

    public abstract JComponent getComponent();

    public abstract void generateSysexMessages(List messages)
        throws InvalidMidiDataException;
}