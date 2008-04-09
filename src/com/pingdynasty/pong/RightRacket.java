package com.pingdynasty.pong;

import java.awt.Point;

class RightRacket extends Racket {
    public RightRacket(PongConfiguration cfg){
        super(cfg);
        // position at middle of right goal minus margin (5) and width of pad (6)
        pos = new Point(cfg.rightgoal - 11, cfg.y + cfg.width / 2);
    }

    public boolean isLeft(){
        return false;
    }

    public boolean check(Ball ball){
        if(ball.pos.x + ball.radius + ball.speed.x >= pos.x // racketPoint.x - 6
//            && ball.pos.x < pos.x + size.width
           && ball.pos.y + ball.radius > pos.y 
           && ball.pos.y < pos.y + size.height){
            int offset = hit(ball);
            //                 log("right racket speed "+speed);
            Pong.enqueue(new Event(Event.HIT, Event.RIGHT, Math.abs(offset)));
            return false;
        }
        return true;
    }

    public void serve(Ball ball){
        ++score;
        // start ball off right away
        ball.pos.x = pos.x - 26; // compensate for extra distance behind racket
        ball.pos.y = pos.y + 25;
        if(pos.y < cfg.height / 2)
            ball.speed.y = 4;
        else
            ball.speed.y = -4;
    }
}
