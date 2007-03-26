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
        setJMenuBar(pong.getMenuBar());
        setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
        setContentPane(pong);
//         getContentPane().add(pong);
        setVisible(true);

    }
    public void start() {}
    public void stop() {
        pong.stop();
    }
    public void destroy() {
        pong.destroy();
    }

}