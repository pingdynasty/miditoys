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
        setContentPane(pong);
        setJMenuBar(pong.getMenuBar(true));
        setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
//         setFocusable(true);
//         requestFocusInWindow();
        setVisible(true);
    }

    public void start() {
    }

    public void stop() {
        pong.stop();
    }

    public void destroy() {
        pong.destroy();
    }
}