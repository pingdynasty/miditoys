package com.pingdynasty.pong;

import java.awt.Graphics;
import java.awt.Point;

public abstract class Racket {
    Point pos;
    Point size = new Point(6, 50);
    int score = 0;

    public Racket(Point pos){
        this.pos = pos;
    }

    public int hit(Ball ball){
        int offset = ball.pos.y - (pos.y + (size.y / 2)); // distance from center of racket
        ball.speed.y += offset / 7;
        ball.speed.x *= -1;
        return offset;
    }

    public abstract void check(Ball ball);
    public abstract void serve(Ball ball);

    public void paint(Graphics g){
        g.fillRect(pos.x, pos.y, size.x, size.y);
    }
}
