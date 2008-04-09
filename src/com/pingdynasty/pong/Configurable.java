package com.pingdynasty.pong;

public abstract class Configurable {

    protected PongConfiguration cfg;

    protected Configurable(){}

    protected Configurable(PongConfiguration cfg){
        this.cfg = cfg;
    }

    public void setConfiguration(PongConfiguration cfg){
        this.cfg = cfg;
    }

    public PongConfiguration getConfiguration(){
        return cfg;
    }

    public void update(){}

}
