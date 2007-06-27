package com.pingdynasty.midi.bcontrol;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;

public class BCRHarmonicOscillatorConfiguration extends DevicePanel  {

    private final static String midiControlInputName = "Control input";
    private final static String midiControlOutputName = "Control output";
    private final static String midiInputName = "MIDI input";

    private boolean doSysex = true;
    private float outputFrequency = 22050.0f;
    private int sampleWidth = 256;
    private int bufferSize = sampleWidth * 4;

    public BCRHarmonicOscillatorConfiguration(){
        super(new String[]{midiInputName, midiControlInputName},  
              new String[]{midiControlOutputName});
    }

    public boolean doSysex(){
        return doSysex;
    }

    public float getOutputFrequency(){
        return outputFrequency;
    }

    public int getSampleWidth(){
        return sampleWidth;
    }

    public int getBufferSize(){
        return bufferSize;
    }

    public MidiDevice getMidiInput(){
        return getDevice(midiInputName);
    }

    public MidiDevice getMidiControlInput(){
        return getDevice(midiControlInputName);
    }

    public MidiDevice getMidiControlOutput(){
        return getDevice(midiControlOutputName);
    }

    public void init()
        throws MidiUnavailableException {
        // try to initialise BCR
        MidiDevice device = DeviceLocator.getDevice("Port 1 (MidiIN:3)");
        setDevice(midiControlInputName, device);
        device = DeviceLocator.getDevice("Port 1 (MidiOUT:3)");
        setDevice(midiControlOutputName, device);
    }

    public JComponent getMiscPanel(){
        Box content = Box.createHorizontalBox();
        content.add(Box.createHorizontalStrut(10));

        // output frequency
        Box box = Box.createVerticalBox();
        box.add(new JLabel("Output frequency"));
        //8000,11025,16000,22050,44100
        String[] options = new String[]{"8000", "11025", "16000", "22050", "44100"};
        Action action = new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    AbstractButton source = (AbstractButton)event.getSource();
                    outputFrequency = Float.parseFloat(source.getText());
                }
            };
        ButtonGroup group = new ButtonGroup();
        for(int i=0; i<options.length; ++i){
            JRadioButton button = new JRadioButton(options[i]);
            button.addActionListener(action);
            if(outputFrequency == Float.parseFloat(options[i]))
                button.setSelected(true);
            group.add(button);
            box.add(button);
        }
        content.add(box);

        // sample width
        content.add(Box.createHorizontalStrut(10));
        box = Box.createVerticalBox();
//         box.add(Box.createGlue());
        box.add(new JLabel("Sample width"));
        options = new String[]{"128", "256", "512", "1024", "2048"};
        action = new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    AbstractButton source = (AbstractButton)event.getSource();
                    sampleWidth = Integer.parseInt(source.getText());
                }
            };
        group = new ButtonGroup();
        for(int i=0; i<options.length; ++i){
            JRadioButton button = new JRadioButton(options[i]);
            button.addActionListener(action);
            if(sampleWidth == Integer.parseInt(options[i]))
                button.setSelected(true);
            group.add(button);
            box.add(button);
        }
        content.add(box);

        // audio buffer size
        content.add(Box.createHorizontalStrut(10));
        box = Box.createVerticalBox();
        box.add(new JLabel("Audio buffer size"));
        options = new String[]{"512", "1024", "2048", "4096", "8192"};
        action = new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    AbstractButton source = (AbstractButton)event.getSource();
                    bufferSize = Integer.parseInt(source.getText());
                }
            };
        group = new ButtonGroup();
        for(int i=0; i<options.length; ++i){
            JRadioButton button = new JRadioButton(options[i]);
            button.addActionListener(action);
            if(bufferSize == Integer.parseInt(options[i]))
                button.setSelected(true);
            group.add(button);
            box.add(button);
        }
        content.add(box);

        return content;
    }

    public JComponent getOutputPanel()
        throws MidiUnavailableException {
        Box content = Box.createVerticalBox();
        content.add(super.getOutputPanel());

        // doSysex configuration
        JPanel combo = new JPanel();
        AbstractAction action = new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    doSysex = !doSysex;
                }
            };
        AbstractButton button = new JCheckBox(action);
        button.setSelected(doSysex);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        combo.add(new JLabel("send Sysex MIDI messages:"));
        combo.add(button);
        content.add(combo);
        return content;
    }
}
