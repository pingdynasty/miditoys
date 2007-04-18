package com.pingdynasty.midi.bcontrol;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;

public class BCRStepSequencerApplet extends JApplet  {

    private BCRStepSequencer steps;

    public void init() {
        steps = new BCRStepSequencer();
        try{
            steps.initialiseMidiDevices();
        }catch(Exception exc){exc.printStackTrace();}
        setContentPane(steps);
        setJMenuBar(steps.getMenuBar());
        setSize(625, 500);
        setVisible(true);
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
        steps.destroy();
    }
}