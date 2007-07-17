package com.pingdynasty.midi.vst;

import com.pingdynasty.midi.*;

import org.jvaptools.VstPluginImpl;
import jvst.wrapper.valueobjects.*;

public class HarmonicOscillatorPlugin extends VstPluginImpl {

    public static final int UNIQUE_ID = '2' << 24 | '5' << 16 | '8' << 8 | '3';
    private HarmonicOscillator osc;
    private OscillatorPanel view;
    private double scale = 1.0d;
//     private double scaleconstant = 2.0d * 32768.0d / 127.0d;
//     private double scaleconstant = 2.0d / 127.0d;
    private double scaleconstant = 2.0d;
    private int framepos;
    private static final int CONTROLS = 16;
    private static final int SAMPLE_WIDTH = 512;
    private static final String[] PARAMETER_NAMES = new String[]{
        "wavelength",
        "distance",
        "time step",
        "scale",
        "glauber state"
    };

//     public native void setOutputSamplerate(float);

    public HarmonicOscillatorPlugin(long wrapper){
        super(wrapper);
        System.out.println("harms effect plugin ctor");
        this.setProgram(0);
        this.setNumInputs(1);
        this.setNumOutputs(1);
        //this.hasVu(false); //deprecated as of vst2.4
        //this.hasClip(false); //deprecated as of vst2.4
        this.canProcessReplacing(true);
//         this.canDoubleReplacing(true);
        this.canMono(true);
        this.isSynth(false);
        this.setUniqueID(UNIQUE_ID);
        this.suspend();
        osc = new HarmonicOscillator(SAMPLE_WIDTH, CONTROLS);
    }

    /* called from HarmonicOscillatorGUI.init() */
    public void setView(OscillatorPanel view){
        this.view = view;
//         setParameter(4, 0.5f); // set scale factor
    }

//     public void open() {
//         super.open();
//         System.out.println("harms effect plugin opened");
//     }

//     public void close() {
//         super.close();
//         //         osc.close();
//         System.out.println("harms effect plugin closed");
//     }

//     public int startProcess() { System.out.println("start process"); return 0;}
//     public int stopProcess() { System.out.println("stop process"); return 0;}
    public boolean setBypass(boolean value) {
        log("harms effect bypass "+value);
        return false; 
    }

    public String getEffectName() { return "harms effect"; }
    public String getVendorString() { return "http://mars.pingdynasty.com/software.oml"; }
    public String getProductString() { return "harms effect"; }
//     public int getNumPrograms() { return 1; }

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
//         if(CANDO_PLUG_MIDI_PROGRAM_NAMES.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_SEND_VST_EVENTS.equals(feature))
//             return CANDO_YES;
//         if(CANDO_PLUG_SEND_VST_MIDI_EVENT.equals(feature))
//             return CANDO_YES;
        return CANDO_NO;
    }

//     public void setBlockSize(int size) {
//         System.out.println("set block size "+size);
//         osc = new HarmonicOscillator(size * 4, CONTROLS);
//     }

//     public void setSampleRate(float rate) {
//         System.out.println("set sample rate "+rate);
//     }

//     public boolean setProcessPrecision(int precision){
//         System.out.println("set precision "+precision);
// //         if(precision == 16)
// //             scaleconstant = 2.0d * 32768.0d / 127.0d;
// //         else if(precision == 32)
// //             scaleconstant = 2.0d * 2147483648.0d / 127.0d;
// //         else
// //             return false;
//         return true;
//     }

    public int getNumParams() { 
        return PARAMETER_NAMES.length + CONTROLS; 
    }

//     public String getParameterLabel(int index) {
//         return "label "+index;
//     }

//     public String getParameterDisplay(int index) {
//         return "display "+index;
//     }

    public String getParameterName(int index) {
        if(index < PARAMETER_NAMES.length)
            return PARAMETER_NAMES[index];
        return "amplitude "+(index - PARAMETER_NAMES.length);
    }

    public void setParameter(int index, float value){
        int val = (int)(value * 127.0f);
//         System.out.println("set parameter "+index+": "+value+"("+val+")");
        switch(index){
        case 0:
            osc.setWavelength(val);
            break;
        case 1:
            osc.setDistance(val);
            break;
        case 2:
            osc.setTimeStep(val);
            break;
        case 3:
//             scale =  val * scaleconstant;
            scale =  value * scaleconstant;
            if(view != null)
                view.setScaleFactor(val);
            break;
        case 4:
            osc.setEnergy(val);
            for(int i=0; i<CONTROLS; ++i)
                setParameterAutomated(i+PARAMETER_NAMES.length, osc.getControl(i));
//             sendParameterToHost();
            updateDisplay(); // update params
            break;
        default:
            osc.setControl(index - PARAMETER_NAMES.length, val);
        }
    }

    public float getParameter(int index) {
        int val = 0;
        switch(index){
        case 0:
            val = osc.getWavelength();
            break;
        case 1:
            val = osc.getDistance();
            break;
        case 2:
            val = osc.getTimeStep();
            break;
        case 3:
//             return (float)(scale / scaleconstant);
            val = (int)(scale / scaleconstant * 127.0d);
            break;
        case 4:
            val = osc.getEnergy();
            break;
        default:
            val = osc.getControl(index - PARAMETER_NAMES.length);
        }
//         System.out.println("get parameter "+index+": "+(val / 127.0f));
        return val / 127.0f;
    }

    public VSTPinProperties getInputProperties(int index) {
        VSTPinProperties vpp = null;
        if(index == 0) {
            vpp = new VSTPinProperties();
            vpp.setLabel("harms effect input");
            vpp.setFlags(VSTPinProperties.VST_PIN_IS_ACTIVE);
        }
        return vpp;
    }

    public VSTPinProperties getOutputProperties(int index) {
        VSTPinProperties vpp = null;
        if(index == 0) {
            vpp = new VSTPinProperties();
            vpp.setLabel("harms effect output");
            vpp.setFlags(VSTPinProperties.VST_PIN_IS_ACTIVE);
        }
        return vpp;
    }

    /* Deprecated as of VST 2.4 */
    public void process(float[][] inputs, float[][] outputs, int frames) {
        processReplacing(inputs, outputs, frames);
    }

    // Audio data processed by VST Plug-Ins is 32 bit (single precision) and optionally 64 bit (double precision) floating-point data. The default used range is from -1.0 to +1.0 inclusive [-1.0, +1.0] (where 1.0 corresponds to 0dB, 0.5 to -6dB and 0.0 to -oodB). Note that an effect could generate values above this range.
    public void processReplacing(float[][] inputs, float[][] outputs, int frames) {
        if(framepos == 0)
            osc.calculate();
        double[] values = osc.getData();
        for(int i=0; i<frames && framepos<values.length; ++i)
            outputs[0][i] = inputs[0][i] * (float)(values[framepos++] * scale);
        if(framepos == values.length){
            framepos = 0;
            osc.increment();
            if(view != null)
                view.setAndScaleData(osc.getData());
        }
    }

//     public void processDoubleReplacing(double[][] inputs, double[][] outputs, int frames) {
//         System.out.println("process double");
//         double[] values = osc.calculate();
//         int pos = 0;
//         while(frames > 0) {
//             int length = frames > values.length ? values.length : frames;
//             for(int i=0; i<length; ++i)
//                 outputs[0][pos++] = values[i] * scale;
//             frames -= length;
//         }
//         osc.increment();
//         if(view != null)
//             view.setAndScaleData(osc.getData());
//     }

}