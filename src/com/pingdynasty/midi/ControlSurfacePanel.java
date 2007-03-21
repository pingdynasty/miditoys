package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ControlSurfacePanel extends JPanel implements MouseMotionListener {

    private Player player;
    private int x, y = 0;

    public void mouseMoved(MouseEvent event){}
    public void mouseDragged(MouseEvent event){
        x = event.getX();
        y = event.getY();
        if(x < 0 || x > 255 || y < 0 || y > 255)
            return;
        player.setVelocity(x/2);
        try{
            if((event.getModifiersEx() & 
                MouseEvent.SHIFT_DOWN_MASK) != 0)
                player.bend(127 - y/2);
            else
                player.modulate(127 - y/2);
        }catch(Exception exc){
            System.out.println(exc.getMessage());
            System.out.println("x/y "+x+"/"+y);
//             exc.printStackTrace();
        }
        //         System.out.println("x/y "+x+"/"+y);
    }

    public ControlSurfacePanel(Player player){
//         super(new BorderLayout());
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