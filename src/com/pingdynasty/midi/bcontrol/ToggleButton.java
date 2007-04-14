package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;

public class ToggleButton extends MidiControl 
    implements ActionListener {

    private int off = 0;
    private int on = 127;
    private int code;
    private JToggleButton button;

    public ToggleButton(int code, int command, int channel, int data1, int data2){
        super(command, channel, data1, data2);
        this.code = code;
        button = new JToggleButton("b"+code, data2 == on);
        button.addActionListener(this);
    }

    public int getCode(){
        return code;
    }

    public void actionPerformed(ActionEvent event) {
        // this is called when the graphical component is updated
        if(data2 == on)
            data2 = off;
        else
            data2 = on;
        try{
            System.out.println("value: "+data2);
            updateMidiControl();
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void generateSysexMessages(List messages)
        throws InvalidMidiDataException {
        System.out.println("button "+code+"  .easypar CC "+channel+" "+data1+" "+on+" "+off+" toggleon");
        // encoder start message
        BCRSysexMessage.createMessage(messages, "$button "+code);
        // easypar message
        // assumes ShortMessage.CONTROL_CHANGE
        BCRSysexMessage.createMessage(messages, "  .easypar CC "+channel+" "+data1+" "+on+" "+off+" toggleon");
        // showvalue message
        BCRSysexMessage.createMessage(messages, "  .showvalue on");
        // default message
        BCRSysexMessage.createMessage(messages, "  .default "+data2);
    }

    public JComponent getComponent(){
        return button;
    }

    public void updateGraphicalControl(){
        System.out.println("data2: "+data2);
        button.setSelected(data2 == on);
        button.repaint();
    }
}