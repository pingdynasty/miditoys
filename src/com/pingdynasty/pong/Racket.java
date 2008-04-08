package com.pingdynasty.pong;

import java.awt.Graphics;
import java.awt.Point;

public abstract class Racket {
    Point pos;
    Point size = new Point(6, 50);
    int score = 0;
    int speed = 0;
    int goal; // the x position of this side's goal line

    public Racket(Point pos){
        this.pos = pos;
    }

    public int hit(Ball ball){
        int offset = ball.pos.y - (pos.y + (size.y / 2)); // distance from center of racket
        if(Math.abs(ball.speed.y) < ball.MAX_VERTICAL_SPEED)
            ball.speed.y += offset / 7;
        ball.speed.x *= -1;
        return offset;
    }

    public void reset(){
        score = 0;
    }

    public abstract boolean isLeft();
    public abstract boolean check(Ball ball);
    public abstract void serve(Ball ball);

    public void paint(Graphics g){
        g.fillRect(pos.x, pos.y, size.x, size.y);
    }
}
