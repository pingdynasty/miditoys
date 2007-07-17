package com.pingdynasty.midi.vst;

import com.pingdynasty.midi.*;

import org.jvaptools.VstPluginImpl;
import jvst.wrapper.valueobjects.*;

public class HarmonicOscillatorSynthPlugin extends HarmonicOscillatorPlugin {

    public static final int UNIQUE_ID = '9' << 24 | '0' << 16 | '2' << 8 | 'p';
    private HarmonicOscillator osc;
    private double scale = 1.0d;
//     private double scaleconstant = 2.0d * 32768.0d / 127.0d;
//     private double scaleconstant = 2.0d / 127.0d;
    private double scaleconstant = 2.0d;
//     private boolean running = false;
    private int framepos;
    private int[] controlvalues = new int[CONTROLS];
    private static final int CONTROLS = 24;
    private static final int SAMPLE_WIDTH = 512;
    private static final int NOTE_OFFSET = 60;
    private static final String[] PARAMETER_NAMES = new String[]{
        "wavelength",
        "distance",
        "time step",
        "scale"
    };

//     public native void setOutputSamplerate(float);

    public HarmonicOscillatorSynthPlugin(long wrapper){
        super(wrapper);
        log("harms synth plugin ctor");
        this.setProgram(0);
        this.setNumInputs(0);
        this.setNumOutputs(1);
        //this.hasVu(false); //deprecated as of vst2.4
        //this.hasClip(false); //deprecated as of vst2.4
        this.canProcessReplacing(true);
        this.canDoubleReplacing(true);
        this.canMono(true);
        this.isSynth(true);
        this.setUniqueID(UNIQUE_ID);
        this.suspend();

        osc = new HarmonicOscillator(SAMPLE_WIDTH, CONTROLS);

//         // initialise sound
//         MidiDevice device = getMidiDevice();
//         try{
//             osc.initSound(device);
//         }catch(Exception exc){
//             exc.printStackTrace();
//         }
    }

    public String getEffectName() { return "harms synth"; }
    public String getProductString() { return "harms synth"; }

    public int getPlugCategory(){
//         return PLUG_CATEG_UNKNOWN;
//         return PLUG_CATEG_EFFECT;
//         return PLUG_CATEG_GENERATOR;
        return PLUG_CATEG_SYNTH;
    }

    public int canDo(String feature){
        log("harms synth cando: "+feature+".");
        if(CANDO_PLUG_RECEIVE_VST_EVENTS.equals(feature))
            return CANDO_YES;
        if(CANDO_PLUG_RECEIVE_VST_MIDI_EVENT.equals(feature))
            return CANDO_YES;
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

    public int getNumParams() { 
        return PARAMETER_NAMES.length + CONTROLS; 
    }

    public String getParameterName(int index) {
        if(index < PARAMETER_NAMES.length)
            return PARAMETER_NAMES[index];
        return "amplitude "+(index - PARAMETER_NAMES.length);
    }

    public void setParameter(int index, float value){
        int val = (int)(value * 127.0f);
//         log("set parameter "+index+": "+value+"("+val+")");
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
            scale =  value * scaleconstant;
            if(view != null)
                view.setScaleFactor(val);
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
        default:
            val = osc.getControl(index - PARAMETER_NAMES.length);
        }
//         log("get parameter "+index+": "+(val / 127.0f));
        return val / 127.0f;
    }

    public VSTPinProperties getOutputProperties(int index) {
        VSTPinProperties vpp = null;
        if(index == 0) {
            vpp = new VSTPinProperties();
            vpp.setLabel("harms synth output");
            vpp.setFlags(VSTPinProperties.VST_PIN_IS_ACTIVE);
        }
        return vpp;
    }

    // Audio data processed by VST Plug-Ins is 32 bit (single precision) and optionally 64 bit (double precision) floating-point data. The default used range is from -1.0 to +1.0 inclusive [-1.0, +1.0] (where 1.0 corresponds to 0dB, 0.5 to -6dB and 0.0 to -oodB). Note that an effect could generate values above this range.
    public void processReplacing(float[][] inputs, float[][] outputs, int frames) {
//         if(running){
            if(framepos == 0)
                osc.calculate();
            double[] values = osc.getData();
            for(int i=0; i<frames && framepos<values.length; ++i)
                outputs[0][i] = (float)(values[framepos++] * scale);
            if(framepos == values.length){
                framepos = 0;
                osc.increment();
                if(view != null)
                    view.setAndScaleData(osc.getData());
            }
//         }
    }

    public void processDoubleReplacing(double[][] inputs, double[][] outputs, int frames) {
//         if(running){
            if(framepos == 0)
                osc.calculate();
            double[] values = osc.getData();
            for(int i=0; i<frames && framepos<values.length; ++i)
                outputs[0][i] = values[framepos++] * scale;
            if(framepos == values.length){
                framepos = 0;
                osc.increment();
                if(view != null)
                    view.setAndScaleData(osc.getData());
            }
//         }
    }

    public int processEvents( VSTEvents vst_events ) {
        VSTEvent[] events = vst_events.getEvents();
        int num_events = vst_events.getNumEvents();
        for(int i=0; i<num_events; ++i) {
            if(events[i].getType() == VSTEvent.VST_EVENT_MIDI_TYPE){
                byte[] msg_data = ((VSTMidiEvent)events[i]).getData();
                switch(msg_data[0] & 0xF0) {
                case 0x80: { /* Note off.*/
                    int control = (msg_data[1] & 0x7F) - NOTE_OFFSET;
//                     log("note off "+control+"/"+(msg_data[1] & 0x7F));
                    if(control < controlvalues.length && control >= 0)
                        osc.setControl(control, controlvalues[control]);
                    controlvalues[control] = 0;
                    break;
                }
                case 0x90: { /* Note on.*/
                    int control = (msg_data[1] & 0x7F) - NOTE_OFFSET;
                    int velocity = msg_data[2] & 0x7F;
//                     log("note on "+control+"/"+(msg_data[1] & 0x7F)+"/"+velocity);
                    if(control < controlvalues.length && control >= 0){
                        /* It seems note on with velocity = 0 is also note off.*/
                        if(velocity == 0){
                            osc.setControl(control, controlvalues[control]);
                            controlvalues[control] = 0;
                        }else{ // if(controlvalues[control] == 0)
                            controlvalues[control] = osc.getControl(control);
                            osc.setControl(control, velocity);
                        }
                    }
                    break;
                }
//             case 0xB0: /* Control change.*/
//                 /* Controller 120 = all sound off */
//                 /* Controller 121 = reset all controllers */
//                 /* Controller 123 = all notes off */
//                 int ctrl = msg_data[ 1 ] & 0x7F;
//                 int value = msg_data[ 2 ] & 0x7F;
//                 if( ctrl >= 20 && ctrl < num_controllers + 20 )
//                     liquinth_vst_gui.set_controller( ctrl - 20, value );
//                 if( ctrl == 0x7E || ctrl == 0x7B )
//                     synthesizer.all_notes_off( false );
//                 break;
//                 case 0xE0: /* Pitch wheel.*/
//                     int pitch = ( msg_data[ 1 ] & 0x7F ) | ( ( msg_data[ 2 ] & 0x7F ) << 7 );
//                     synthesizer.set_pitch_wheel( pitch - 8192 );
//                     break;
//                 }
                }
            }
        }
        return 1;
    }
}