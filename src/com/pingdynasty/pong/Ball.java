package com.pingdynasty.pong;

import java.awt.Graphics;
import java.awt.Point;

public class Ball {
    public static final int MAX_VERTICAL_SPEED = 18;

    Point pos = new Point(0, 0);
    Point speed = new Point(0, 0);
    int radius = 8;
    int tick = 0;
    int distance; // total pixels across screen
    int resolution; // total ticks across screen
 
    public Ball(){
        pos = new Point(Pong.SCREEN_WIDTH / 2, Pong.SCREEN_HEIGHT / 2);
        speed = new Point(0, 0);
    }

    public Ball(Point pos, Point speed){
        this.pos = pos;
        this.speed = speed;
    }

    public void move(int tick){
        assert tick < resolution && tick >= 0;
        if(speed.x > 0)
            pos.x = 20 + tick * distance / resolution;
        else
            pos.x = Pong.SCREEN_WIDTH - tick * distance / resolution - 20;
        pos.y = pos.y + speed.y;
    }

//     public void move(long start, long now, long target){
//         // ratio of time elapsed in this beat
//         double ratio = (double)(now - start) / (double)(target - start);
//         if(speed.x > 0)
//             pos.x = (int)(distance * ratio);
//         else
//             pos.x = Pong.SCREEN_WIDTH - (int)(distance * ratio);

//         pos.y = pos.y + speed.y;
//     }
//     public void move(){
//         pos.x = pos.x + speed.x;
//         pos.y = pos.y + speed.y;
//     }		

    public void tick(){
        ++tick;
    }

    public void paint(Graphics g){
        g.fillOval(pos.x, pos.y, radius, radius);
    }

    public void reset(){
        pos = new Point(Pong.SCREEN_WIDTH / 2, Pong.SCREEN_HEIGHT / 2);
    }

    // get the relative distance between racket and ball, 0-30
    public int distance(Racket racket){
        int edge = racket.pos.y + racket.size.y;
        if(edge > pos.y)
            return (edge - pos.y) * 30 / (Pong.SCREEN_HEIGHT - 40);
        else
            return (pos.y - racket.pos.y) * 30 / (Pong.SCREEN_HEIGHT - 40);
    }
}

