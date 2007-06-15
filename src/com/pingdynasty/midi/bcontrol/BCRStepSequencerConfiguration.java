package com.pingdynasty.midi.bcontrol;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;

public class BCRStepSequencerConfiguration extends DevicePanel  {

    private final static String midiControlInputName = "Control input";
    private final static String midiControlOutputName = "Control output";
    private final static String midiInputName = "MIDI input";
    private final static String midiOutputName = "MIDI output";
    private final static String midiSyncName = "MIDI sync";

    private int channel = 0; // Java channel 0 is MIDI channel 1
    private boolean doSysex = true;

    public BCRStepSequencerConfiguration(){
        super(new String[]{midiInputName, midiSyncName, midiControlInputName},  
              new String[]{midiOutputName, midiControlOutputName});
    }

    public int getChannel(){
        return channel;
    }

    public boolean doSysex(){
        return doSysex;
    }

    public MidiDevice getMidiSync(){
        return getDevice(midiSyncName);
    }

    public MidiDevice getMidiInput(){
        return getDevice(midiInputName);
    }

    public MidiDevice getMidiOutput(){
        return getDevice(midiOutputName);
    }

    public MidiDevice getMidiControlInput(){
        return getDevice(midiControlInputName);
    }

    public MidiDevice getMidiControlOutput(){
        return getDevice(midiControlOutputName);
    }

    public void init()
        throws MidiUnavailableException {
        // initialise MIDI out
        MidiDevice device = DeviceLocator.getDevice(Synthesizer.class);
        setDevice(midiOutputName, device);

        // try to initialise BCR
        device = DeviceLocator.getDevice("Port 1 (MidiIN:3)");
        setDevice(midiControlInputName, device);
        device = DeviceLocator.getDevice("Port 1 (MidiOUT:3)");
        setDevice(midiControlOutputName, device);
    }

//     public JComponent getMiscPanel()
//         throws MidiUnavailableException {
//         Box content = Box.createHorizontalBox();
// ...
//         return content;
//     }

    public JPanel getPanel()
        throws MidiUnavailableException{
        JPanel panel = super.getPanel();

        // doSysex configuration
        JPanel combo = new JPanel();
        AbstractAction action = new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    doSysex = !doSysex();
                }
            };
        AbstractButton button = new JCheckBox(action);
        button.setSelected(doSysex);
        combo.add(new JLabel("send Sysex MIDI messages:"));
        combo.add(button);
        panel.add(combo);

        // channel configuration
        combo = new JPanel();
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(channel+1, 1, 16, 1));
        spinner.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent e){
                    JSpinner source = (JSpinner)e.getSource();
                    channel = ((Integer)source.getValue()).intValue()-1;
                }
            });
        combo.add(new JLabel("MIDI channel:"));
        combo.add(spinner);
        panel.add(combo);

        return panel;
    }
}
