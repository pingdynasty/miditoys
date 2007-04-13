package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;

public interface Control {

    public int getValue();

    public void setValue(int value)
        throws InvalidMidiDataException;

    public MidiMessage getMidiMessage()
        throws InvalidMidiDataException;

    public void generateSysexMessages(List messages)
        throws InvalidMidiDataException;
}