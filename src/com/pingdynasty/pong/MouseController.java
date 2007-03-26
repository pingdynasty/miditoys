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

    public void mouseMoved(MouseEvent event){
        moveTo(event.getY() - 25);
    }
	
    public void mouseDragged(MouseEvent event){}

    public void move(){}

    public void close(){
        component.removeMouseMotionListener(this);
    }
}
