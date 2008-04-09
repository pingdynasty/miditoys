package com.pingdynasty.pong;

import java.awt.Point;

class LeftRacket extends Racket {
    public LeftRacket(PongConfiguration cfg){
        super(cfg);
        // position at middle of left goal plus margin (5)
        pos = new Point(cfg.leftgoal + 5, cfg.y + cfg.width / 2);
    }

    public boolean isLeft(){
        return true;
    }

    public boolean check(Ball ball){
        if(ball.pos.x + ball.speed.x <= pos.x + size.width
//            && ball.pos.x + ball.radius > pos.x
           && ball.pos.y + ball.radius > pos.y 
           && ball.pos.y < pos.y + size.height){
            int offset = hit(ball);
            // parameters:
            // ball vertical speed
            // racket vertical speed
            // offset point
//                 log("left racket speed "+speed);
            Pong.enqueue(new Event(Event.HIT, Event.LEFT, Math.abs(offset)));
            return false;
        }
        return true;
    }

    public void serve(Ball ball){
        ++score;
        // start ball off right away
        ball.pos.x = pos.x + 20; // compensate for extra distance behind racket
        //             ball.pos.y = pos.y + 25;
        ball.pos.y = cfg.height / 2;
        if(pos.y < cfg.height / 2)
            ball.speed.y = 4;
        else
            ball.speed.y = -4;
    }
}
