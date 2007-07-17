package com.pingdynasty.midi.vst;

import java.awt.*;
import javax.swing.*;
import com.pingdynasty.midi.*;
import jvst.wrapper.*;
import jvst.wrapper.valueobjects.*;

public class HarmonicOscillatorGUI extends VSTPluginGUIAdapter {

    private OscillatorPanel view;
    private HarmonicOscillatorPlugin plugin;

    public HarmonicOscillatorGUI(){
        setTitle("harms vst");
        view = new OscillatorPanel(512); // todo fix sizes
        view.setMinimumSize(new Dimension(512, 100));
        view.setPreferredSize(new Dimension(512, 200));
        setSize(new Dimension(512, 100));
        //     this.setResizable(false);
        log("harms gui ctor");
    }

    // the order of events seems to be:
    // PongPlugin ctor
    // PongGUI ctor
    // PongGUI.init()
    // PongPlugin.open()
    // PongGUI.open()
    public void init(VSTPluginAdapter adapter) {
        if(adapter instanceof HarmonicOscillatorPlugin){
            plugin = (HarmonicOscillatorPlugin)adapter;
            plugin.setView(view);
        }
//         plugin.setView(view);
//         setJMenuBar(osc.getMenuBar(false));
        getContentPane().add(view);
        undecorate();
        log("harms gui init");
    }

    public void open() {
        super.open();
        log("harms gui open");
        setVisible(true);
    }

    public void close() {
        super.close();
        log("harms gui close");
        setVisible(false);
    }
}