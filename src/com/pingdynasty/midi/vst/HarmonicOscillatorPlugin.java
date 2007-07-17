package com.pingdynasty.midi.vst;

import com.pingdynasty.midi.*;

import jvst.wrapper.VSTPluginAdapter;
import jvst.wrapper.valueobjects.*;

public abstract class HarmonicOscillatorPlugin extends VSTPluginAdapter {

    protected OscillatorPanel view;

    public HarmonicOscillatorPlugin(long wrapper){
        super(wrapper);
    }

    /* called from HarmonicOscillatorGUI.init() */
    public void setView(OscillatorPanel view){
        this.view = view;
    }

    public String getVendorString() { return "http://mars.pingdynasty.com/software.oml"; }

    /* Deprecated as of VST 2.4 */
    public void process(float[][] inputs, float[][] outputs, int frames) {
        processReplacing(inputs, outputs, frames);
    }

    public boolean setBypass(boolean value) {
        log("harms effect bypass "+value);
        return false; 
    }

    public int getNumPrograms() {
        return 1;
    }

    public String getParameterLabel(int index) {
        return "";
    }

    public String getParameterDisplay(int index) {
        return "";
    }

    public int getProgram() {
        return 0;
    }

    public String getProgramName() {
        return "";
    }

    public String getProgramNameIndexed(int category, int index) {
        return "";
    }

    public void setProgram(int index) {}

    public void setProgramName(String name) {}

    public boolean string2Parameter(int index, String value) {   
        return false;
    }
}