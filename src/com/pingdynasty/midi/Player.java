package com.pingdynasty.midi;

import javax.sound.midi.*;

public abstract class Player {
    protected int velocity;
    protected int duration;

    public void setVelocity(int velocity){
        this.velocity = velocity;
    }

    public int getVelocity(){
        return velocity;
    }
    /**
     * Set note duration, in milliseconds.
     */
    public void setDuration(int duration){
        this.duration = duration;
    }

    public int getDuration(){
        return duration;
    }

    /**
     * Play note with predetermined duration and velocity.
     * This implements a blocking call to play note - waits for the duration of the note.
     */
    public void play(int note)
        throws InvalidMidiDataException{
        noteon(note);
        try{
            Thread.sleep(duration);
        }catch(InterruptedException exc){}
        noteoff(note);
    }

    public abstract void setChannel(int channel);
    public abstract int getChannel();
    public abstract void noteon(int note)
        throws InvalidMidiDataException;
    public abstract void noteoff(int note)
        throws InvalidMidiDataException;
    /** bend - the amount of pitch change, as a nonnegative 14-bit value (8192 = no bend) (0-16384) */
    public abstract void bend(int degree)
        throws InvalidMidiDataException;
    public abstract void modulate(int degree)
        throws InvalidMidiDataException;
    public abstract void programChange(int bank, int program)
        throws InvalidMidiDataException;
    public abstract void controlChange(int code, int value)
        throws InvalidMidiDataException;
    public abstract void allNotesOff()
        throws InvalidMidiDataException;
    public abstract void close();
}
