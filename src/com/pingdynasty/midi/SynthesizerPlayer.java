package com.pingdynasty.midi;

import javax.sound.midi.*;

public class SynthesizerPlayer extends Player {
    private Synthesizer synth;
    private MidiChannel midi;

    public SynthesizerPlayer(Synthesizer synth){
        this.synth = synth;
        midi = synth.getChannels()[0];
    }

    public void noteon(int note)
        throws InvalidMidiDataException{
        midi.noteOn(note, velocity);
    }

    public void noteoff(int note)
        throws InvalidMidiDataException{
        midi.noteOff(note);
    }

    public void bend(int degree)
        throws InvalidMidiDataException{
        midi.setPitchBend(degree);
    }

    public void modulate(int degree)
        throws InvalidMidiDataException{
        midi.controlChange(1, degree);
    }

    public void programChange(int bank, int program)
        throws InvalidMidiDataException{
        midi.programChange(bank, program);
    }

    public void setChannel(int channel)
        throws InvalidMidiDataException{
        midi = synth.getChannels()[channel];
    }

    public int getChannel(){
        MidiChannel[] channels = synth.getChannels();
        for(int i=0; i<channels.length; ++i)
            if(channels[i] == midi)
                return i;
        return -1;
    }

    public void allNotesOff()
        throws InvalidMidiDataException{
        midi.controlChange(123, 0);
    }

    public void close(){
        synth.close();
    }
}
