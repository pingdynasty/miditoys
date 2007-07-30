package com.pingdynasty.midi;

import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.InvalidMidiDataException;

public class HarmonicOscillatorSynth implements Receiver {

    private HarmonicOscillator osc;
    private int[] controlvalues;
    private int wavelength;
    private int bend;
    private int timestep;
    private int modulation;
    private static final int NOTE_OFFSET = 60;

    public HarmonicOscillatorSynth(HarmonicOscillator osc){
        setHarmonicOscillator(osc);
    }

    public void setHarmonicOscillator(HarmonicOscillator osc){
        this.osc = osc;
        controlvalues = new int[osc.getControls()];
    }

    public void close(){}

    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    public void send(ShortMessage msg, long time)
        throws InvalidMidiDataException {
        switch(msg.getStatus()){
        case ShortMessage.NOTE_ON:{
            int velocity = msg.getData2();
            if(velocity == 0)
                noteoff(msg.getData1());
            else
                noteon(msg.getData1(), velocity);
            break;
        }
        case ShortMessage.NOTE_OFF:
            noteoff(msg.getData1());
            break;
        case ShortMessage.PITCH_BEND:
            pitchbend((msg.getData1() & 0x7F) | ((msg.getData2() & 0x7F) << 7));
            break;
        case ShortMessage.CONTROL_CHANGE:
            if(msg.getData1() == 1)
                modulation(msg.getData2());
            break;
        }
    }

    public void pitchbend(int bend){
        System.out.println("synth pitch bend "+bend);
        if(this.bend == 0){
            if(bend == 8192)
                return;
            // store wavelength
            this.bend = bend;
            wavelength = osc.getWavelength();
            pitchbend(bend);
        }else if(bend == 8192){
            // reset wavelength
            osc.setWavelength(wavelength);
            this.bend = 0;
        }else{
            int value = (8192 - bend) / 64 + wavelength;
            if(value < 0)
                value = 0;
            else if(value > 127)
                value = 127;
            osc.setWavelength(value);
        }
    }

    public void modulation(int modulation){
        System.out.println("synth modulation "+modulation);
        if(this.modulation == 0){
            if(modulation == 0)
                return;
            // store timestep
            this.modulation = modulation;
            timestep = osc.getTimeStep();
            modulation(modulation);
        }else if(modulation == 0){
            // reset timestep
            osc.setTimeStep(timestep);
            this.modulation = 0;
        }else{
            int value = modulation + timestep;
            if(value > 127)
                value = 127;
            osc.setTimeStep(value);
        }
    }
    
    public void noteon(int note, int velocity){
        System.out.println("synth note on "+note+"/"+velocity);
        int control = note - NOTE_OFFSET;
        if(control < controlvalues.length && control >= 0){
            controlvalues[control] = osc.getControl(control);
            osc.setControl(control, velocity);
        }
    }

    public void noteoff(int note){
        System.out.println("synth note off "+note);
        int control = note - NOTE_OFFSET;
        if(control < controlvalues.length && control >= 0){
            osc.setControl(control, controlvalues[control]);
            controlvalues[control] = 0;
        }
    }
}