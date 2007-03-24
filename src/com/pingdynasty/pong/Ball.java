package com.pingdynasty.pong;

import java.awt.Graphics;
import java.awt.Point;

public class Ball {
    Point pos = new Point(0, 0);
    Point speed = new Point(0, 0);
    int radius = 8;

    public Ball(){
        pos = new Point(Pong.SCREEN_WIDTH / 2, Pong.SCREEN_HEIGHT / 2);
        speed = new Point(0, 0);
    }

    public Ball(Point pos, Point speed){
        this.pos = pos;
        this.speed = speed;
    }

    public void move(){
        pos.x = pos.x + speed.x;
        pos.y = pos.y + speed.y;
    }		

    public void paint(Graphics g){
        g.fillOval(pos.x, pos.y, radius, radius);
    }
}

