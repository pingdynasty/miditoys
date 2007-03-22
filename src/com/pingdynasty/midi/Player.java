package com.pingdynasty.midi;

import javax.sound.midi.*;

public abstract class Player {
    protected int velocity;
    public void setVelocity(int velocity){
        this.velocity = velocity;
    }
    public abstract void noteon(int note)
        throws InvalidMidiDataException;
    public abstract void noteoff(int note)
        throws InvalidMidiDataException;
    public abstract void bend(int degree)
        throws InvalidMidiDataException;
    public abstract void modulate(int degree)
        throws InvalidMidiDataException;
    public abstract void programChange(int bank, int program)
        throws InvalidMidiDataException;
    public abstract void setChannel(int channel)
        throws InvalidMidiDataException;
    public abstract void allNotesOff()
        throws InvalidMidiDataException;
    public abstract void close();
}
