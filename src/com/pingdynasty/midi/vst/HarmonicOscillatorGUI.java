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
        System.out.println("harms gui ctor");
        setTitle("harms vst");
        view = new OscillatorPanel(512); // todo fix sizes
        view.setMinimumSize(new Dimension(512, 100));
        view.setPreferredSize(new Dimension(512, 200));
        setSize(new Dimension(512, 100));
        //     this.setResizable(false);
    }

    // the order of events seems to be:
    // PongPlugin ctor
    // PongGUI ctor
    // PongGUI.init()
    // PongPlugin.open()
    // PongGUI.open()
    public void init(VSTPluginAdapter adapter) {
        System.out.println("harms gui init");
        plugin = (HarmonicOscillatorPlugin)adapter;
        plugin.setView(view);
//         setJMenuBar(pong.getMenuBar(false));
        getContentPane().add(view);
        setVisible(true);
    }

    public void open() {
        super.open();
        System.out.println("harms gui open");
        setVisible(true);
    }

    public void close() {
        super.close();
        System.out.println("harms gui close");
        setVisible(false);
    }
}