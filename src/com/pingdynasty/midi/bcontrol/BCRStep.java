package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;

public class BCRStep extends Step {
//     private int index;
    private MidiControl note;
    private MidiControl velocity;
    private MidiControl duration;
    private MidiControl modulation;
//     private MidiControl bend;
// private MidiControl offset;

//     public BCRStep(int index, int channel){
// //         this.index = index;
//         note = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 1+index, 60);
//         velocity = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 81+index, 80);
//         duration = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 89+index, 80);
//         modulation = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 97+index, 0);
// //         bend = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 1+index, 64);
//     }

    public BCRStep(RotaryEncoder note, RotaryEncoder velocity, 
                   RotaryEncoder duration, RotaryEncoder modulation){
        this.note = note;
        this.velocity = velocity;
        this.duration = duration;
        this.modulation = modulation;
//         this.bend = bend;
    }

    public int getNote(){
        return note.getValue();
    }

    public int getVelocity(){
        return velocity.getValue();
    }

    public int getDuration(){
        return duration.getValue();
    }

    public int getModulation(){
        return modulation.getValue();
    }

//     public int getBend(){
//         return bend.getValue();
//     }

    public void setNote(int note){
        try{
            this.note.setValue(note);
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void setVelocity(int velocity){
        try{
            this.velocity.setValue(velocity);
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void setDuration(int duration){
        try{
            this.duration.setValue(duration);
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void setModulation(int modulation){
        try{
            this.modulation.setValue(modulation);
        }catch(Exception exc){exc.printStackTrace();}
    }

//     public void setBend(int bend){
//         bend.setValue(bend);
//     }

}