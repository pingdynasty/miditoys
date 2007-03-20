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
        player.setVelocity(x/2);
        y = event.getY();
        System.out.println("x/y "+x+"/"+y);
//         duration = event.getY();
//         System.out.println("duration "+duration);
    }

    public ControlSurfacePanel(Player player){
        super(new BorderLayout());
        this.player = player;

        Dimension dim = new Dimension(255, 255);
        setSize(dim);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
        addMouseMotionListener(this);
        setBorder(BorderFactory.createLineBorder(Color.blue));
        revalidate();
    }

    public void setPlayer(Player player){
        this.player = player;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }
}