package com.pingdynasty.pong;

import java.awt.*;
import java.awt.event.*;

public class MouseController extends RacketController implements MouseMotionListener {

    private Component component;

    public MouseController(Racket racket, Component component){
        super(racket);
        this.component = component;
        component.addMouseMotionListener(this);
    }

    public void mouseMoved(MouseEvent e){
        moveTo(e.getY() - 25);
        component.repaint();
    }
	
    public void mouseDragged(MouseEvent e) {}
}
