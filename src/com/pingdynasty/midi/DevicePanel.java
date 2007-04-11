package com.pingdynasty.midi;

import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.midi.*;


public class DevicePanel {
    private String[] names;
    private Class[] types;
    private MidiDevice[] devices;

    class DeviceActionListener implements ActionListener {
        MidiDevice device;
        int index;
        public DeviceActionListener(MidiDevice device, int index){
            this.device = device;
            this.index = index;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                device.open();
                devices[index] = device;
                System.out.println(names[index]+": "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public DevicePanel(String[] names, Class[] types){
        if(names.length != types.length)
            throw new IllegalArgumentException("name and class arrays must be of same length");
        this.names = names;
        this.types = types;
        devices = new MidiDevice[names.length];

    }

    public JPanel getPanel()
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

        List list = new ArrayList();
        for(int i=0; i<info.length; ++i){
            for(int j=0; j<types.length; ++j){
                if(types[j].isInstance(MidiSystem.getMidiDevice(info[i]))){
                    list.add(MidiSystem.getMidiDevice(info[i]));
                    break;
                }
            }
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
                if(types[i].isInstance(mididevices[j])){
                    JRadioButton button = new JRadioButton();
                    button.addActionListener(new DeviceActionListener(mididevices[j], i));
                    if(devices[i] == mididevices[j])
                        button.setSelected(true);
//                     if(!types[i].isInstance(mididevices[j]))
//                         button.disable();
                    groups[i].add(button);
                    content.add(button);
                }else{
                    content.add(new JPanel());
                }
            }
        }
        return content;
    }

    public JFrame getFrame()
        throws MidiUnavailableException {
        JPanel panel = getPanel();
        JFrame frame = new JFrame("MIDI configuration");
        frame.setContentPane(panel);
        return frame;
    }

    public MidiDevice getDevice(String name){
        for(int i=0; i<names.length; ++i)
            if(names[i].equals(name))
                return devices[i];
        return null;
    }
}
