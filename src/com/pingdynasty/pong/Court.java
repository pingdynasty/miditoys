package com.pingdynasty.pong;

import java.awt.Graphics;

class Court extends Configurable {

    public Court(PongConfiguration cfg) {
        super(cfg);
    }

    public boolean check(Ball ball){
        if(ball.pos.x <= cfg.leftgoal){
            // goal on left side
//             Pong.enqueue(new Event(Event.SCORE, Event.RIGHT, ball.distance(leftController.racket)));
            //                 enqueue(new Event(Event.MISS, Event.LEFT, ball.speed.y));
//             leftController.missed();
//             rightController.serve(ball);
            return false;
        }else if(ball.pos.x + ball.diameter >= cfg.rightgoal){
            // goal on right side
//             Pong.enqueue(new Event(Event.SCORE, Event.LEFT, ball.distance(rightController.racket)));
            //                 enqueue(new Event(Event.MISS, Event.RIGHT, ball.speed.y));
            return false;
        }else if(ball.pos.y <= cfg.y){
            // hit top wall
            ball.speed.y *= -1;
            if(cfg.doWalls)
                Pong.enqueue(new Event(Event.WALL, Event.LEFT, ball.speed.y));
        }else if(ball.pos.y + ball.diameter >= cfg.y + cfg.height){
            // hit bottom wall
            if(cfg.doWalls)
                Pong.enqueue(new Event(Event.WALL, Event.LEFT, ball.speed.y));
            ball.speed.y *= -1;
        }
        return true;
    }

    public void paint(Graphics g){
        g.drawRect(cfg.x, cfg.y, cfg.width, cfg.height);
    }
}

