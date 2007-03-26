package com.pingdynasty.pong;

import java.awt.Point;

public class ComputerController extends RacketController {

    Ball ball;
    int adjustment = 8;

    public ComputerController(Racket racket, Ball ball){
        super(racket);
        this.ball = ball;
    }

    public void move(){
        int centerpos = racket.pos.y + racket.size.y / 2;
        if((ball.speed.x < 0 && racket.isLeft()) ||
           (ball.speed.x > 0 && !racket.isLeft())){
            int dist = java.lang.Math.abs(ball.pos.y - centerpos);
            if(centerpos < ball.pos.y - 3)
                move(dist / adjustment);
            else if(centerpos > ball.pos.y + 3)
                move(-dist / adjustment);
        }else{
            if(centerpos < Pong.SCREEN_HEIGHT / 2 - 3)
                move(3);
            else if(centerpos > Pong.SCREEN_HEIGHT / 2 + 3)
                move(-3);
        }
    }

    public void missed(){
        if(adjustment > 2)
            --adjustment;
    }
}
