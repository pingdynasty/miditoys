package com.pingdynasty.pong;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;

public class PongApplet extends JApplet  {

    private Pong pong;

    public void init() {
        pong = new Pong();
        // create menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("Devices");
        try{
            MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
            MidiDevice[] devices = new MidiDevice[info.length];
            for(int i=0; i<info.length; ++i){
                devices[i] = MidiSystem.getMidiDevice(info[i]); 
                if(devices[i] instanceof Receiver ||
                   devices[i] instanceof Synthesizer){
                    JMenuItem item = new JMenuItem(info[i].getName());
                    item.addActionListener(pong.new DeviceActionListener(devices[i]));
                    menu.add(item); 
                }
            }
        }catch(Exception exc){}
        menubar.add(menu);
        menu = new JMenu("Scales");
        String[] scalenames = pong.scales.getScaleNames();
        for(int i=0; i<scalenames.length; ++i){
            JMenuItem item = new JMenuItem(scalenames[i]);
            item.addActionListener(pong.new ScaleActionListener(i));
            menu.add(item);
        }
        menubar.add(menu);

        // create frame
        JFrame frame = new JFrame("pong");
        frame.setJMenuBar(menubar);
        frame.setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
        frame.setContentPane(pong);
        frame.setVisible(true);

    }
    public void start() {}
    public void stop() {
        pong.stop();
    }
    public void destroy() {
        pong.destroy();
    }

}