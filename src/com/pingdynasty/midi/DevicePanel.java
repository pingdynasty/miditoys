package com.pingdynasty.midi;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.midi.*;

public class DevicePanel {
    private String[] inputnames;
    private String[] outputnames;
    private Map devices;
    private JFrame frame;

    class DeviceActionListener implements ActionListener {
        MidiDevice device;
        String name;
        public DeviceActionListener(MidiDevice device, String name){
            this.device = device;
            this.name = name;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                device.open();
                devices.put(name, device);
                System.out.println(name+": "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public DevicePanel(String[] inputnames, String[] outputnames){
        this.inputnames = inputnames;
        this.outputnames = outputnames;
        devices = new HashMap();
    }

    public JPanel getPanel()
        throws MidiUnavailableException {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        if(inputnames.length > 0){
            content.add(new JLabel("Input"));
            content.add(getPanel(inputnames, true));
        }
        if(outputnames.length > 0){
            content.add(new JLabel("Output"));
            content.add(getPanel(outputnames, false));
        }
        return content;
    }

    public JPanel getPanel(String[] names, boolean input)
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        List list = new ArrayList();
        for(int i=0; i<info.length; ++i){
            MidiDevice device = MidiSystem.getMidiDevice(info[i]);
//             if((!input && device.getMaxReceivers() != 0) ||
//                (input  && device.getMaxTransmitters() != 0))
            if((!input && (device instanceof Receiver || device instanceof Synthesizer)) ||
               (input  && device instanceof Transmitter))
                list.add(device);
        }
        MidiDevice[] mididevices = new MidiDevice[list.size()];
        list.toArray(mididevices);

        JPanel content = new JPanel();
        content.setLayout(new GridLayout(mididevices.length+1, names.length+1));

        ButtonGroup[] groups = new ButtonGroup[names.length];
        for(int i=0; i<names.length; ++i)
            groups[i] = new ButtonGroup();

        // first row
        content.add(new JLabel());
        for(int i=0; i<names.length; ++i)
            content.add(new JLabel(names[i]));

        // device rows
        for(int j=0; j<mididevices.length; ++j){
            String desc = // mididevices[j].getDeviceInfo().getVendor() + " " + 
                mididevices[j].getDeviceInfo().getName();
//                 + " " + mididevices[j].getDeviceInfo().getDescription()
            content.add(new JLabel(desc));

            for(int i=0; i<names.length; ++i){
                JRadioButton button = new JRadioButton();
                button.addActionListener(new DeviceActionListener(mididevices[j], names[i]));
                MidiDevice device = getDevice(names[i]);
                if(device != null && device.getDeviceInfo().getName()
                   .equals(mididevices[j].getDeviceInfo().getName()))
                    button.setSelected(true);
                groups[i].add(button);
                content.add(button);
            }
        }
        return content;
    }

    public JFrame getFrame()
        throws MidiUnavailableException {
        if(frame == null){
            JPanel panel = getPanel();
            JButton close = new JButton("close");
            close.addActionListener(new AbstractAction(){
                    public void actionPerformed(ActionEvent event){
                        frame.setVisible(false);
                    }
                });
            panel.add(close);
            frame = new JFrame("MIDI configuration");
            frame.setContentPane(panel);
            frame.pack();
        }
        return frame;
    }

    public void setDevice(String name, MidiDevice device){
        devices.put(name, device);
    }

    public MidiDevice getDevice(String name){
        return (MidiDevice)devices.get(name);
    }

    public void open()
        throws MidiUnavailableException {
        if(frame == null)
            getFrame();
        frame.setVisible(true);
    }

    public void close(){
        frame.setVisible(false);
    }
}