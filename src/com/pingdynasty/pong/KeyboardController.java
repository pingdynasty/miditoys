package com.pingdynasty.pong;

import java.awt.*;
import java.awt.event.*;
import javax.swing.KeyStroke;

public class KeyboardController extends RacketController implements Runnable, KeyListener {

    private static final int steplength = 4;
    private Component component;
    private int up;
    private int down;
    private boolean movingdown = false;
    private boolean movingup = false;
    private Thread poller;
    private boolean running = true;

    public KeyboardController(Racket racket, Component component, int up, int down){
                              
        super(racket);
        this.component = component;
        this.up = up;
        this.down = down;
        component.addKeyListener(this);
        component.setFocusable(true);
        poller = new Thread(this);
        poller.setDaemon(true);
        poller.start();
    }

    public KeyboardController(Racket racket, Component component, 
                              String upkey, String downkey){
        super(racket);
        this.component = component;
        component.addKeyListener(this);
        KeyStroke stroke = KeyStroke.getKeyStroke(upkey);
        if(stroke == null)
            throw new IllegalArgumentException("no such key: "+upkey);
        up = stroke.getKeyCode();
        stroke = KeyStroke.getKeyStroke(downkey);
        if(stroke == null)
            throw new IllegalArgumentException("no such key: "+downkey);
        down = stroke.getKeyCode();
        poller = new Thread(this);
        poller.setDaemon(true);
        poller.start();
    }

    public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e){
        System.out.println("pressed "+KeyEvent.getKeyText(e.getKeyCode())+" ("+e.getKeyCode()+")");
        if(e.getKeyCode() == up)
            movingup = true;
        else if(e.getKeyCode() == down)
            movingdown = true;
    }

    public void keyReleased(KeyEvent e){
        System.out.println("released "+KeyEvent.getKeyText(e.getKeyCode())+" ("+e.getKeyCode()+")");
        if(e.getKeyCode() == up)
            movingup = false;
        else if(e.getKeyCode() == down)
            movingdown = false;
    }

    public void run(){
        while(running){
            if(movingup){
                move(-steplength);
            }else if(movingdown){
                move(steplength);
            }
            try{
                Thread.sleep(10);
            }catch(InterruptedException exc){}
        }
    }

    public void destroy(){
        running = false;
    }
}
