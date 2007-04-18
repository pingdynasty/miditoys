package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;

public class ToggleButton extends MidiControl 
    implements ActionListener {

    protected int off = 0;
    protected int on = 127;
    protected int code;
    protected AbstractButton button;

    protected ToggleButton(int command, int channel, int data1, int data2){
        super(command, channel, data1, data2);
    }

    public ToggleButton(int code, int command, int channel, int data1, int data2,
                        String description){
        super(command, channel, data1, data2);
        this.code = code;
        button = new BControlButton();
//         button = new JToggleButton();
        if(description != null)
            button.setToolTipText(description);
        button.setSelected(data2 == on);
        button.addActionListener(this);
    }

    public int getCode(){
        return code;
    }

    public void actionPerformed(ActionEvent event) {
//         System.out.println("action performed "+event);
        // this is called when the graphical component is updated
        if(data2 == on)
            data2 = off;
        else
            data2 = on;
        button.setSelected(data2 == on);
        try{
            callback();
            updateMidiControl();
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void generateSysexMessages(List messages)
        throws InvalidMidiDataException {
//         System.out.println("button "+code+"  .easypar CC "+channel+" "+data1+" "+on+" "+off+" toggleon");
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
        button.setSelected(data2 == on);
        button.repaint();
    }
}