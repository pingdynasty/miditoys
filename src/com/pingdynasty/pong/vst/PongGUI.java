package com.pingdynasty.pong.vst;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;
import com.pingdynasty.pong.*;

public class PongGUI extends jvst.wrapper.VSTPluginGUIAdapter {

    private PongPlugin plugin;
    private Pong pong;

    public PongGUI(){
        System.out.println("pong gui ctor");
        setTitle("pong vst");
        setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
        //     this.setResizable(false);
    }

    // the order of events seems to be:
    // PongPlugin ctor
    // PongGUI ctor
    // PongGUI.init()
    // PongPlugin.open()
    // PongGUI.open()
    public void init(jvst.wrapper.VSTPluginAdapter adapter) {
        System.out.println("pong gui init");
        plugin = (PongPlugin)adapter;
        pong = plugin.pong;
        setJMenuBar(pong.getMenuBar(false));
        getContentPane().add(pong);
        setVisible(true);
    }

    public void open() {
        super.open();
        System.out.println("pong gui open");
        setVisible(true);
    }

    public void close() {
        super.close();
        System.out.println("pong gui close");
        setVisible(false);
    }
}