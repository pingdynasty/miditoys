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
        pong.initSound();

        // create frame
        JFrame frame = new JFrame("pong");
        frame.setJMenuBar(pong.getMenuBar());
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