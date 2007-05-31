package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;

public class RotaryEncoder extends MidiControl 
    implements ChangeListener, Comparable {

    private int code;
    private int min = 0; // 0-16383
    private int max = 127;
    private Knob knob;
    private boolean internalChange;

    public RotaryEncoder(int code, int command, int channel, int data1, int data2,
                         String description){
        super(command, channel, data1, data2);
        this.code = code;
        knob = new Knob();
        if(description != null)
            knob.setToolTipText(description);
        knob.addChangeListener(this);
        float value = (float)(data2 - min) / (float)max;
        knob.setValue(value);
    }

    public int compareTo(Object o){
        if(code < ((RotaryEncoder)o).code)
            return -1;
        if(code > ((RotaryEncoder)o).code)
            return 1;
        return 0;
    }

    public int getCode(){
        // the encoder number
        return code;
    }

    // ChangeListener i/f
    public void stateChanged(ChangeEvent event) {
        if(!internalChange){
            // this is called when the graphical component is updated
            Knob source = (Knob)event.getSource();
            int value = (int)(source.getValue() * (float)max) + min;
            try{
                data2 = value;
                callback();
                updateMidiControl();
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public void generateSysexMessages(List messages)
        throws InvalidMidiDataException {
//         System.out.println("encoder "+code+"  .easypar CC "+channel+" "+data1+" "+min+" "+max+" absolute");
        // encoder start message
        BCRSysexMessage.createMessage(messages, "$encoder "+code);
        // easypar message
        // assumes ShortMessage.CONTROL_CHANGE
        BCRSysexMessage.createMessage(messages, "  .easypar CC "+(channel+1)+" "+data1+" "+min+" "+max+" absolute");
        // showvalue message
        BCRSysexMessage.createMessage(messages, "  .showvalue on");
        // mode message
        if(code < 33) // push encoder
            BCRSysexMessage.createMessage(messages, "  .mode 12dot");
        else
            BCRSysexMessage.createMessage(messages, "  .mode 1dot/off");
        // default message
        BCRSysexMessage.createMessage(messages, "  .default "+data2);
    }

    public JComponent getComponent(){
        return knob;
//         JPanel panel = new JPanel();
//         panel.add(knob);
//         panel.add(new JLabel(""+code));
//         return panel;
    }

    public void updateGraphicalControl(){
        internalChange = true;
        float value = (float)(data2 - min) / (float)max;
        knob.setValue(value);
        knob.repaint();
        internalChange = false;
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
            new RotaryEncoder(1, ShortMessage.CONTROL_CHANGE, 1, 1, 50, null),
            new RotaryEncoder(2, ShortMessage.CONTROL_CHANGE, 1, 2, 60, null),
            new RotaryEncoder(3, ShortMessage.CONTROL_CHANGE, 1, 3, 70, null),
            new RotaryEncoder(4, ShortMessage.CONTROL_CHANGE, 1, 4, 80, null),
            new RotaryEncoder(5, ShortMessage.CONTROL_CHANGE, 1, 5, 90, null)
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