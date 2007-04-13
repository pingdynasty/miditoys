package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;

public class RotaryEncoder extends MidiControl implements ChangeListener {

    private int encoder;
    private int min = 0; // 0-16383
    private int max = 127;
    private Knob knob;

    public RotaryEncoder(int encoder, int command, int channel, int data1, int data2){
        super(command, channel, data1, data2);
        this.encoder = encoder;
        knob = new Knob();
        knob.addChangeListener(this);
        float value = (float)(data2 - min) / (float)max;
        knob.setValue(value);
    }

    // ChangeListener i/f
    public void stateChanged(ChangeEvent event) {
        // this is called when the graphical component is updated
        Knob source = (Knob)event.getSource();
        //                     if(!source.getValueIsAdjusting()){
        int value = (int)(source.getValue() * (float)max) + min;
//         System.out.println("encoder value: "+value);
        try{
            data2 = value;
            updateMidiControl();
        }catch(Exception exc){exc.printStackTrace();}
        //                     }
    }

    public void generateSysexMessages(List messages)
        throws InvalidMidiDataException {
        // encoder start message
        BCRSysexMessage.createMessage(messages, "$encoder "+encoder);
        // easypar message
        // assumes ShortMessage.CONTROL_CHANGE
        BCRSysexMessage.createMessage(messages, "  .easypar CC "+channel+" "+data1+" "+min+" "+max+" absolute");
        // showvalue message
        BCRSysexMessage.createMessage(messages, "  .showvalue on");
        // mode message
        BCRSysexMessage.createMessage(messages, "  .mode 12dot");
        // default message
        BCRSysexMessage.createMessage(messages, "  .default "+data2);
    }

    public JComponent getComponent(){
        return knob;
    }

    public void updateGraphicalControl(){
        float value = (float)(data2 - min) / (float)max;
        System.out.println("knob value: "+value);
        knob.setValue(value);
        knob.repaint();
    }

    public static final void main(String[] args)
        throws Exception {
        List messages = new java.util.ArrayList();
        BCRSysexMessage.createMessage(messages, "$rev R1");
//         BCRSysexMessage.createMessage(messages, "$preset");
//         BCRSysexMessage.createMessage(messages, "  .name 'bcr keyboard control    '");
//         BCRSysexMessage.createMessage(messages, "  .snapshot off");
//         BCRSysexMessage.createMessage(messages, "  .request off");
//         BCRSysexMessage.createMessage(messages, "  .egroups 4");
//         BCRSysexMessage.createMessage(messages, "  .fkeys on");
//         BCRSysexMessage.createMessage(messages, "  .lock off");
//         BCRSysexMessage.createMessage(messages, "  .init");

        RotaryEncoder[] knobs = new RotaryEncoder[]{
            new RotaryEncoder(1, ShortMessage.CONTROL_CHANGE, 1, 1, 50),
            new RotaryEncoder(2, ShortMessage.CONTROL_CHANGE, 1, 2, 60),
            new RotaryEncoder(3, ShortMessage.CONTROL_CHANGE, 1, 3, 70),
            new RotaryEncoder(4, ShortMessage.CONTROL_CHANGE, 1, 4, 80),
            new RotaryEncoder(5, ShortMessage.CONTROL_CHANGE, 1, 5, 90)
        };
        for(int i=0; i<knobs.length; ++i)
            knobs[i].generateSysexMessages(messages);

        BCRSysexMessage.createMessage(messages, " ");
        BCRSysexMessage.createMessage(messages, "$end");

        String[] names = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<names.length; ++i)
            System.out.println(i+": "+names[i]);
        System.out.println("enter number of device");
        int pos = System.in.read() - 48;
        MidiDevice device = DeviceLocator.getDevice(names[pos]);
        device.open();
        Receiver receiver = device.getReceiver();
        for(int i=0; i<messages.size(); ++i)
            receiver.send((MidiMessage)messages.get(i), -1);
        System.out.println("Sysex messages sent to "+names[pos]);
    }
}