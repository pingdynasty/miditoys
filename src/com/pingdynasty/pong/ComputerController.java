package com.pingdynasty.pong;

import java.awt.Point;

public class ComputerController extends RacketController {

    Ball ball;
    int adjustment;
    int fudge;

    public ComputerController(Racket racket, Ball ball){
        super(racket);
        this.ball = ball;
        update();
    }

    public void move(){
        int centerpos = racket.pos.y + racket.size.height / 2;
        if((ball.speed.x < 0 && racket.isLeft()) ||
           (ball.speed.x > 0 && !racket.isLeft())){
            int dist = ball.pos.y + (ball.diameter / 2) - centerpos;
//             if(Math.abs(dist) > fudge)
                move(dist / adjustment);
//             if(centerpos < ball.pos.y - fudge)
//                 move(dist / adjustment);
//             else if(centerpos > ball.pos.y + fudge)
//                 move(-dist / adjustment);
        }else{
            if(centerpos < cfg.height / 2 - fudge)
                move(3);
            else if(centerpos > cfg.height / 2 + fudge)
                move(-3);
        }
    }

    public void update(){
        adjustment = cfg.computerRacketSkill;
        // set fudge factor slightly different on left/right sides
        fudge = cfg.computerRacketFudge + (racket.isLeft() ? 3 : -3);
    }

    public void reset(){
        super.reset();
        update();
    }

    public void missed(){
        if(adjustment > 2)
            --adjustment;
    }
}
