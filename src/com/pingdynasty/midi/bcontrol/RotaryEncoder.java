package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;

public class RotaryEncoder extends MidiControl implements ChangeListener {

    private int min = 0; // 0-16383
    private int max = 127;
    private Knob knob;

    public RotaryEncoder(int command, int channel, int data1, int data2){
        super(command, channel, data1, data2);
        knob = new Knob();
        knob.addChangeListener(this);
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

    public void generateSysexMessages(List messages, int encoder)
        throws InvalidMidiDataException {
        int index = messages.size();
        // encoder start message
        BCRSysexMessage sysex = new BCRSysexMessage(index++);
        sysex.setMessage("$encoder "+encoder);
        messages.add(sysex);
        // easypar message
        sysex = new BCRSysexMessage(index++);
        // assumes ShortMessage.CONTROL_CHANGE
        sysex.setMessage("  .easypar CC "+channel+" "+data1+" "+min+" "+max+" absolute");
        messages.add(sysex);
        // showvalue message
        sysex = new BCRSysexMessage(index++);
        sysex.setMessage("  .showvalue on");
        messages.add(sysex);
        // mode message
        sysex = new BCRSysexMessage(index++);
        sysex.setMessage("  .mode 12dot");
        messages.add(sysex);
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

    public static void createMessage(List messages, String data)
        throws Exception {
        BCRSysexMessage sysex = new BCRSysexMessage(messages.size());
        sysex.setMessage(data);
        messages.add(sysex);
    }

    public static final void main(String[] args)
        throws Exception {
        List messages = new java.util.ArrayList();
        createMessage(messages, "$rev R1");
//         createMessage(messages, "$preset");
//         createMessage(messages, "  .name 'bcr keyboard control    '");
//         createMessage(messages, "  .snapshot off");
//         createMessage(messages, "  .request off");
//         createMessage(messages, "  .egroups 4");
//         createMessage(messages, "  .fkeys on");
//         createMessage(messages, "  .lock off");
//         createMessage(messages, "  .init");

        RotaryEncoder[] knobs = new RotaryEncoder[]{
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 1, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 2, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 3, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 4, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 5, 127)
        };
        for(int i=0; i<knobs.length; ++i)
            knobs[i].generateSysexMessages(messages, i+1);

        createMessage(messages, " ");
        createMessage(messages, "$end");

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