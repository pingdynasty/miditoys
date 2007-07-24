package com.pingdynasty.midi.bcontrol;

import java.io.File;
import java.net.URL;
import javax.swing.*;

public class BCRBeatSlicerApplet extends JApplet  {

    private BCRBeatSlicer beats;

    public void init() {
        try{
            beats = new BCRBeatSlicer();
            beats.configure();
            String sample = getParameter("sample");
            if(sample != null && !sample.equals(""))
                beats.loadSample(new URL(getCodeBase(), sample));
        }catch(Exception exc){exc.printStackTrace();}
        setContentPane(beats);
        setJMenuBar(beats.getMenuBar());
        setSize(625, 550);
        setVisible(true);
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
        beats.destroy();
    }
}