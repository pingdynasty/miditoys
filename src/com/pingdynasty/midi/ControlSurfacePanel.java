package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ControlSurfacePanel extends JPanel implements MouseMotionListener {

    private Player player;
    private int velocity, modulation, bend;

    public void mouseDragged(MouseEvent event){}
    public void mouseMoved(MouseEvent event){
        int x = event.getX() - 20;
        int y = event.getY() - 20;
        if(x < 0 || x > 255 || y < 0 || y > 255)
            return;
        velocity = x / 2;
        player.setVelocity(velocity);
        try{
            if((event.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK){
                bend = 127 - y/2;
                player.bend(bend);
            }else{
                modulation = 127 - y/2;
                player.modulate(modulation);
            }
        }catch(Exception exc){
            System.out.println("x/y "+x+"/"+y);
            exc.printStackTrace();
        }
        repaint();
    }

    public ControlSurfacePanel(Player player){
        super(new BorderLayout());
        this.player = player;
        bend = 64;
        modulation = 0;
        velocity = 60;
        try{
            player.bend(bend);
            player.modulate(modulation);
            player.setVelocity(velocity);
        }catch(Exception exc){
            exc.printStackTrace();
        }

        Dimension dim = new Dimension(300, 300);
        setSize(dim);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
        addMouseMotionListener(this);
        revalidate();
    }

    public void setPlayer(Player player){
        this.player = player;
    }

    public void reset(){
        bend = 64;
        modulation = 0;
        try{
            player.bend(bend);
            player.modulate(modulation);
        }catch(Exception exc){
            exc.printStackTrace();
        }
        repaint();
    }

    public void paintComponent(Graphics g){
        g.setColor(Color.blue);
        g.clearRect(0, 0, 300, 300);
        g.drawString("velocity "+velocity, 24, 16);
        g.drawString("modulation "+modulation, 114, 16);
        g.drawRect(19, 20, 262, 262);
        // put a blue dot on velocity / modulation mark
        g.fillOval(velocity * 2 + 20, 275 - modulation * 2, 6, 6);
        // put a purple dot on velocity / bend mark
        g.setColor(Color.magenta);
        g.drawString("bend "+bend, 224, 16);
        g.fillOval(velocity * 2 + 20, 275 - bend * 2, 6, 6);
    }
}