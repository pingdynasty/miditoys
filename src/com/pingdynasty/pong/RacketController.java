package com.pingdynasty.pong;

import java.awt.Point;

public abstract class RacketController {

    Racket racket;

    public RacketController(Racket racket){
        this.racket = racket;
    }

    public void move(int delta){
//         int midpos = racket.pos.y + (racket.size.y / 2) + delta;
        int midpos = racket.pos.y + delta;
        if(midpos > -35 && midpos < Pong.SCREEN_HEIGHT - 50)
            racket.pos.y += delta;
    }

    public void moveTo(int pos){
        racket.pos.y = pos;
    }

    abstract public void move();

    public void serve(Ball ball){
        racket.serve(ball);
    }

    public void missed(){}

    // close this controller
    public void close(){}
}
