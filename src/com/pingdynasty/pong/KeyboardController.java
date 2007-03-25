package com.pingdynasty.pong;

import java.awt.*;
import java.awt.event.*;
import javax.swing.KeyStroke;

public class KeyboardController extends RacketController implements KeyListener {

    private static final int steplength = 4;
    private int up;
    private int down;
    private boolean movingdown = false;
    private boolean movingup = false;

    public KeyboardController(Racket racket, Component component, int up, int down){
        super(racket);
        this.up = up;
        this.down = down;
        component.addKeyListener(this);
        component.setFocusable(true);
    }

    public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e){
        if(e.getKeyCode() == up)
            movingup = true;
        else if(e.getKeyCode() == down)
            movingdown = true;
    }

    public void keyReleased(KeyEvent e){
        if(e.getKeyCode() == up)
            movingup = false;
        else if(e.getKeyCode() == down)
            movingdown = false;
    }

    public void move(){
        if(movingup){
            move(-steplength);
        }else if(movingdown){
            move(steplength);
        }
    }
}
