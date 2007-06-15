package com.pingdynasty.midi;

public class Step {
    private int note = 60;
    private int velocity = 64;
    private int duration = 64;
    private int modulation = 0;
    private int bend = 64;
    private int delay = 0;
    private int lastNote;
    private int noteOffTick;

    public Step(){}

    public Step(int note, int velocity, int duration, 
                int modulation, int bend, int delay){
        this.note = note;
        this.velocity = velocity;
        this.duration = duration;
        this.modulation = modulation;
        this.bend = bend;
        this.delay = delay;
    }

    public int getNote(){
        return note;
    }

    public int getVelocity(){
        return velocity;
    }

    public int getDuration(){
        return duration;
    }

    public int getDurationTicks(){
        return duration * 24 / 127;
    }

    public int getModulation(){
        return modulation;
    }

    public int getBend(){
        return bend;
    }

    public int getDelay(){
        return delay;
    }

    public int getDelayTicks(){
        return delay * 24 / 127;
    }

    public void setNote(int note){
        this.note = note;
    }

    public void setVelocity(int velocity){
        this.velocity = velocity;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }

    public void setModulation(int modulation){
        this.modulation = modulation;
    }

    public void setBend(int bend){
        this.bend = bend;
    }

    public void setDelay(int delay){
        this.delay = delay;
    }

    public int getLastNote(){
        return lastNote;
    }

    public void setNoteOffTick(int tick){
        this.noteOffTick = tick;
        lastNote = note;
    }

    public int getNoteOffTick(){
        return noteOffTick;
    }
}
