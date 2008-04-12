package com.pingdynasty.pong;

import java.awt.Graphics;
import java.awt.Point;

public class Ball extends Configurable {

    Point pos;
    Point speed;
    int diameter;
    int distance; // total pixels across screen
    private int resolution; // total ticks across screen
 
    public Ball(PongConfiguration cfg){
        super(cfg);
        pos = new Point(); // cfg.x + cfg.width / 2, cfg.y + cfg.height / 2);
//         diameter = (cfg.width + cfg.height) / 120;
        speed = new Point(0, 0);
        update();
    }

    public void update(){
        pos.x = cfg.x + cfg.width / 2;
        pos.y = cfg.y + cfg.height / 2;
        diameter = (cfg.width + cfg.height) / 120;
        distance = cfg.rightgoal - cfg.leftgoal;
        resolution = cfg.clocksperbeat * cfg.ticksperclock;
    }

    public void move(int tick){
//         assert tick <= resolution && tick >= 0 : "tick: "+tick;
        if(speed.x > 0)
            pos.x = cfg.leftgoal + tick * distance / resolution;
        else
            pos.x = cfg.rightgoal - tick * distance / resolution;
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

    public void paint(Graphics g){
        g.fillOval(pos.x, pos.y, diameter, diameter);
    }

    public void reset(){
        move(cfg.clockoffset*cfg.ticksperclock);
        pos.y = cfg.height / 2;
        speed.y = 2;
    }

    // get the relative distance between racket and ball, 0-30
    public int distance(Racket racket){
        int edge = racket.pos.y + racket.size.height;
        if(edge > pos.y)
            return (edge - pos.y) * 30 / (Pong.SCREEN_HEIGHT - 40);
        else
            return (pos.y - racket.pos.y) * 30 / (Pong.SCREEN_HEIGHT - 40);
    }
}

