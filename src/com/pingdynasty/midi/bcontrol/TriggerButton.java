package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;

public class TriggerButton extends ToggleButton implements ChangeListener {

    public TriggerButton(int code, int command, int channel, int data1, int data2,
                        String description){
        super(command, channel, data1, data2);
        this.code = code;
        button = new BControlButton();
        if(description != null)
            button.setToolTipText(description);
        button.addChangeListener(this);
    }

    public void stateChanged(ChangeEvent event) {
        // this is called when the graphical component is updated
        JButton source = (JButton)event.getSource();
        boolean changed = false;
        if(source.getModel().isPressed() && data2 == off){
            data2 = on;
            changed = true;
        }else if(!source.getModel().isPressed() && data2 == on){
            data2 = off;
            changed = true;
        }
//         System.out.println("trigger event - changed "+changed);
        if(changed){
            try{
                callback();
                updateMidiControl();
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public void send(ShortMessage msg, long time){
        super.send(msg, time);
    }

    public void updateGraphicalControl(){
        button.getModel().setPressed(data2 == on);
        button.repaint();
    }

    public void generateSysexMessages(List messages)
        throws InvalidMidiDataException {
//         System.out.println("button "+code+"  .easypar CC "+channel+" "+data1+" "+on+" "+off+" toggleoff");
        // encoder start message
        BCRSysexMessage.createMessage(messages, "$button "+code);
        // easypar message
        // assumes ShortMessage.CONTROL_CHANGE
        BCRSysexMessage.createMessage(messages, "  .easypar CC "+(channel+1)+" "+data1+" "+on+" "+off+" toggleoff");
        // showvalue message
        BCRSysexMessage.createMessage(messages, "  .showvalue off");
    }
}