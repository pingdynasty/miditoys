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
    private JFrame frame;

    public void init(jvst.wrapper.VSTPluginAdapter adapter) {
        plugin = (PongPlugin)adapter;
    }

    public void open() {
        super.open();
        System.out.println("pong gui open");
        pong = plugin.pong;
        if(frame == null){
            //         // create menu bar
            //         JMenuBar menubar = new JMenuBar();
            //         JMenu menu = new JMenu("Scales");
            //         String[] scalenames = pong.scales.getScaleNames();
            //         for(int i=0; i<scalenames.length; ++i){
            //             JMenuItem item = new JMenuItem(scalenames[i]);
            //             item.addActionListener(pong.new ScaleActionListener(i));
            //             menu.add(item);
            //         }
            //         menubar.add(menu);

            // create frame
            frame = new JFrame("pong");
            //         frame.setJMenuBar(menubar);
            frame.setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
            frame.getContentPane().add(pong);
        }
        frame.setVisible(true);
    }

    public void close() {
        super.close();
        System.out.println("pong gui close");
        if(frame != null)
            frame.setVisible(false);
    }
}