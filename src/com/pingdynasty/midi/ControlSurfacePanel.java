package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ControlSurfacePanel extends JPanel implements MouseMotionListener {

    private Player player;
    private int x, y = 0;

    public void mouseDragged(MouseEvent event){}
    public void mouseMoved(MouseEvent event){
        x = event.getX();
        System.out.println("velocity "+x/2);
        player.setVelocity(x/2);
        y = event.getY();
//         duration = event.getY();
//         System.out.println("duration "+duration);
    }

    public ControlSurfacePanel(Player player){
        super(new BorderLayout());
        this.player = player;
        addMouseMotionListener(this);
        setSize(255, 255);
        setVisible(true);
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }
}