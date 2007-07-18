package com.pingdynasty.pong.vst;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;
import com.pingdynasty.pong.*;

import jvst.wrapper.valueobjects.*;

public class PongPlugin extends org.jvaptools.VstPluginImpl  {

    Pong pong;
    public static final int UNIQUE_ID = '4' << 24 | 'i' << 16 | '5' << 8 | '8';

    public PongPlugin(long wrapper){
        super(wrapper);
        System.out.println("pong plugin ctor");
        this.setProgram(0);
        this.setNumInputs(0);
        this.setNumOutputs(2);
        //this.hasVu(false); //deprecated as of vst2.4
        //this.hasClip(false); //deprecated as of vst2.4
        this.canProcessReplacing(true);
        this.canMono(true);
        this.isSynth(true);
        this.setUniqueID(UNIQUE_ID);
        this.suspend();

        pong = new Pong();

        // initialise sound
        MidiDevice device = getMidiDevice();
        try{
            pong.initSound(device);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void open() {
        super.open();
        System.out.println("pong plugin open");
    }

    public String getEffectName() { return "pong"; }
    public String getVendorString() { return "http://mars.pingdynasty.com/pong/"; }
    public String getProductString() { return "pong"; }
//     public int getNumPrograms() { return 1; }
//     public int getNumParams() { return 0; }
    public boolean setBypass(boolean value) {
//         pong.startOrStop(value);
        // todo: pause pong
        return false; 
    }

    public int getPlugCategory(){
//         return PLUG_CATEG_UNKNOWN;
        return PLUG_CATEG_EFFECT;
//         return PLUG_CATEG_GENERATOR;
//         return PLUG_CATEG_SYNTH;
    }

    public int canDo(String feature){
        System.out.println("cando: "+feature+".");
//         if(CANDO_PLUG_RECEIVE_VST_MIDI_EVENT.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_RECEIVE_VST_TIME_INFO.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_MIDI_PROGRAM_NAMES.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_SEND_VST_EVENTS.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_RECEIVE_VST_EVENTS.equals(feature))
//             return CANDO_YES;
        if(CANDO_PLUG_SEND_VST_MIDI_EVENT.equals(feature))
            return CANDO_YES;
        return CANDO_NO;
    }

    public void close() {
        super.close();
        System.out.println("pong plugin close");
        pong.stop();
    }
}