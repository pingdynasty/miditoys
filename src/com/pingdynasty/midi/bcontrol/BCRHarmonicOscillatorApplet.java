package com.pingdynasty.midi.bcontrol;

import java.io.File;
import java.net.URL;
import javax.swing.*;

public class BCRHarmonicOscillatorApplet extends JApplet  {

    private BCRHarmonicOscillator osc;

    public void init() {
        try{
            osc = new BCRHarmonicOscillator();
//             osc.initialiseMidiDevices();
        }catch(Exception exc){exc.printStackTrace();}
        setContentPane(osc);
        setJMenuBar(osc.getMenuBar());
        setSize(625, 650);
        setVisible(true);
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
        osc.destroy();
    }
}