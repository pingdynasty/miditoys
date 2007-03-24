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
        int enemyPos = racket.pos.y + racket.size.y / 2;
        if(ball.speed.x < 0){
            int dist = java.lang.Math.abs(ball.pos.y - enemyPos);
            if(enemyPos < (ball.pos.y - 3))
                move(dist / adjustment);
            else if(enemyPos > (ball.pos.y + 3))
                move(- dist / adjustment);
        }else{
            if(enemyPos < (Pong.SCREEN_HEIGHT / 2 - 3))
                move(2);
            else if(enemyPos > (Pong.SCREEN_HEIGHT / 2 + 3))
                move(-2);
        }
    }
}
