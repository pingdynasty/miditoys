package com.pingdynasty.pong;

import java.awt.Point;
import java.awt.Graphics;
import java.awt.Dimension;

public abstract class Racket extends Configurable {
    Point pos;
    Dimension size = new Dimension(6, 50);
    int score = 0;
    int speed = 0;
//     int goal; // the x position of this side's goal line

    public Racket(PongConfiguration cfg){
        super(cfg);
    }

//     public Racket(Point pos){
//         this.pos = pos;
//     }

    public int hit(Ball ball){
        int offset = ball.pos.y - (pos.y + (size.height / 2)); // distance from center of racket
        if(Math.abs(ball.speed.y) < cfg.maxVerticalSpeed)
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
        g.fillRect(pos.x, pos.y, size.width, size.height);
    }
}
