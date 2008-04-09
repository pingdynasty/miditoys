package com.pingdynasty.pong;

import java.awt.Point;

public abstract class RacketController extends Configurable {

    Racket racket;

    public RacketController(Racket racket){
        super(racket.getConfiguration());
        this.racket = racket;
    }

    public void move(int delta){
//         int midpos = racket.pos.y + (racket.size.y / 2) + delta;
        int midpos = racket.pos.y + delta;
        if(midpos > -35 && midpos < Pong.SCREEN_HEIGHT - 50)
            racket.pos.y += delta;
        racket.speed = (racket.speed / 2) + delta;
    }

    public void moveTo(int pos){
        racket.speed = (racket.speed / 2) + (pos - racket.pos.y);
        racket.pos.y = pos;
    }

    abstract public void move();

    public void serve(Ball ball){
        racket.serve(ball);
    }

    public void missed(){}

    public void reset(){
        racket.reset();
    }

    // close this controller
    public void close(){}
}
