package com.pingdynasty.pong;

public class Event {
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final int SCORE = 1;
    public static final int MISS = 2;
    public static final int HIT = 3;
    public static final int WALL = 4;

    private static final String[] names = {"Score", "Miss", "Hit", "Wall"};

    public final int type;
    public final String side;
    public final int value;
        
    public Event(int type, String side, int value){
        this.type = type;
        this.side = side;
        this.value = value;
    }

    public String getEventName(){
        return names[type-1];
    }
}
