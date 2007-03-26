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

    public PongPlugin(long wrapper){
        super(wrapper);

        this.setProgram(0);
        this.setNumInputs(2);
        this.setNumOutputs(2);
        //this.hasVu(false); //deprecated as of vst2.4
        //this.hasClip(false); //deprecated as of vst2.4
        this.canProcessReplacing(true);
//         this.canMono(true);
        this.isSynth(true);

        this.setUniqueID(7842315);
        this.suspend();
    }

//     public void init(org.jvaptools.VstPluginImpl owner){
//         System.out.println("pong plugin init");
//         pong = new Pong();
//         MidiDevice device = getMidiDevice();
//         try{
//             pong.initSound(device);
//         }catch(Exception exc){
//             exc.printStackTrace();
//         }
//         System.out.println("pong plugin finished init");
//     }

    public void open() {
        super.open();
        System.out.println("pong plugin open");
        pong = new Pong();
        MidiDevice device = getMidiDevice();
        System.out.println("pong midi device "+device);
        try{
//             pong.initSound();
            pong.initSound(device);
        }catch(Exception exc){
            exc.printStackTrace();
        }
        System.out.println("pong plugin open done");
    }

    public String getEffectName() { return "pong"; }
    public String getVendorString() { return "http://mars.pingdynasty.com/pong/"; }
    public String getProductString() { return "pong"; }
    public int getNumPrograms() { return 1; }
    public int getNumParams() { return 0; }
    public boolean setBypass(boolean value) { return false; }

    public int getPlugCategory(){
//         return PLUG_CATEG_UNKNOWN;
        return PLUG_CATEG_EFFECT;
//         return PLUG_CATEG_GENERATOR;
//         return PLUG_CATEG_SYNTH;
    }

    public int canDo(String feature){
        System.out.println("cando: "+feature+".");
//         if(CANDO_PLUG_RECEIVE_VST_EVENTS.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_RECEIVE_VST_MIDI_EVENT.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_RECEIVE_VST_TIME_INFO.equals(feature))
//             return CANDO_YES;
        if(CANDO_PLUG_SEND_VST_MIDI_EVENT.equals(feature))
            return CANDO_YES;
//         if(CANDO_PLUG_MIDI_PROGRAM_NAMES.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_SEND_VST_EVENTS.equals(feature))
//             return CANDO_YES;
        return CANDO_NO;
    }

    public void close() {
        super.close();
        System.out.println("pong plugin close");
        pong.stop();
    }

//     public int getMidiProgramName(int channel, MidiProgramName mpn) {
//         System.out.println("pong getMidiProgramName: "+mpn.getName()+" "+channel);
//         int prg = mpn.getThisProgramIndex();
//         mpn.setMidiProgram((byte)prg);
//         mpn.setName("Pong"+prg);
//         mpn.setParentCategoryIndex(-1);
//         return 6;
//     }

//   public int getCurrentMidiProgram(int channel, MidiProgramName mpn) {    
//       System.out.println("pong getCurrentMidiProgram: "+mpn.getName()+" "+channel);
//       if(channel < 0 || channel >= 16 || mpn==null) return -1;
//       mpn.setThisProgramIndex(1);
//       mpn.setName("Standard");
//       mpn.setMidiProgram((byte)0);
//       mpn.setParentCategoryIndex(-1);
//       return 1;
//   }

//   public int getMidiProgramCategory(int channel, MidiProgramCategory cat) {
//       System.out.println("pong getMidiProgramCategory: "+cat.getName()+" "+channel);
//       System.out.println("pong getMidiProgramCategory: "+cat.getThisCategoryIndex()+" "+channel);
//       return 1;
//   }

//   public boolean hasMidiProgramsChanged(int channel) {
//       System.out.println("pong hasMidiProgramsChanged: "+channel);
//       return false;
//   }

//   public boolean getMidiKeyName(long channel, MidiKeyName key) {
//       System.out.println("pong getMidiKeyName: "+key);
//       return false;
//   }
}

// enum VstPlugCategory
// {
//     kPlugCategUnknown = 0,              ///< Unknown, category not implemented
//     kPlugCategEffect,                   ///< Simple Effect
//     kPlugCategSynth,                    ///< VST Instrument (Synths, samplers,...)
//     kPlugCategAnalysis,                 ///< Scope, Tuner, ...
//     kPlugCategMastering,                ///< Dynamics, ...
//         kPlugCategSpacializer,          ///< Panners, ...
//         kPlugCategRoomFx,                       ///< Delays and Reverbs
//         kPlugSurroundFx,                        ///< Dedicated surround processor
//         kPlugCategRestoration,          ///< Denoiser, ...
//         kPlugCategOfflineProcess,       ///< Offline Process
//         kPlugCategShell,                        ///< Plug-in is container of other plug-ins  @see effShellGetNextPlugin
//         kPlugCategGenerator,            ///< ToneGenerator, ...

//         kPlugCategMaxCount                      ///< Marker to count the categor
// ies
