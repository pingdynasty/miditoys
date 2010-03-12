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
    private Action updateAction; // action to perform when configuration is saved/updated
    private Action cancelAction; // action to perform when configuration is cancelled

    class DeviceActionListener implements ActionListener {
        MidiDevice device;
        String name;
        public DeviceActionListener(MidiDevice device, String name){
            this.device = device;
            this.name = name;
        }
        public void actionPerformed(ActionEvent event) {
            MidiDevice previous = getDevice(name);
            if(previous == device)
                return;
            if(previous != null)
                previous.close();
            if(device == null){
                devices.remove(name);
                System.out.println(name+": disabled");
            }else{
//             try{
//                 device.open();
                devices.put(name, device);
                System.out.println(name+": "+device.getDeviceInfo().getName());
//             }catch(Exception exc){exc.printStackTrace();}
            }
        }
    }

    public DevicePanel(String[] inputnames, String[] outputnames){
        this.inputnames = inputnames;
        this.outputnames = outputnames;
        devices = new HashMap();
    }

    public void setUpdateAction(Action updateAction){
        this.updateAction = updateAction;
    }

    public void setCancelAction(Action cancelAction){
        this.cancelAction = cancelAction;
    }

    public void init()
        throws MidiUnavailableException {
    }

    public JPanel getPanel()
        throws MidiUnavailableException {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JTabbedPane tabs = new JTabbedPane();
        JComponent panel = getInputPanel();
        if(panel != null)
            tabs.addTab("MIDI Input", panel);
        panel = getOutputPanel();
        if(panel != null)
            tabs.addTab("MIDI Output", panel);
        panel = getMiscPanel();
        if(panel != null)
            tabs.addTab("Miscellaneous", panel);
        //The following line enables to use scrolling tabs.
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        content.add(tabs);
        return content;
    }

    public JComponent getInputPanel()
        throws MidiUnavailableException {
        if(inputnames.length == 0)
            return null;
        return getPanel(inputnames, true);
    }

    public JComponent getOutputPanel()
        throws MidiUnavailableException {
        if(outputnames.length == 0)
            return null;
        return getPanel(outputnames, false);
    }

    public JComponent getMiscPanel()
        throws MidiUnavailableException {
        return null;
    }

    public JPanel getPanel(String[] names, boolean input)
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        List list = new ArrayList();
        for(int i=0; i<info.length; ++i){
            MidiDevice device = MidiSystem.getMidiDevice(info[i]);
            if((!input && device.getMaxReceivers() != 0) ||
               (input  && device.getMaxTransmitters() != 0))
                list.add(device);
        }
        MidiDevice[] mididevices = new MidiDevice[list.size()];
        list.toArray(mididevices);

        JPanel content = new JPanel(new SpringLayout());
        ButtonGroup[] groups = new ButtonGroup[names.length];
        for(int i=0; i<names.length; ++i)
            groups[i] = new ButtonGroup();

        // first row labels
        content.add(new JLabel());
        for(int i=0; i<names.length; ++i)
            content.add(new JLabel(names[i]));

        // second row 'none' buttons
        content.add(new JLabel("none"));
        for(int i=0; i<names.length; ++i){
            JRadioButton button = new JRadioButton();
            button.addActionListener(new DeviceActionListener(null, names[i]));
            if(getDevice(names[i]) == null)
                button.setSelected(true);
            groups[i].add(button);
            content.add(button);
        }

        // device rows
        for(int j=0; j<mididevices.length; ++j){
            String desc = mididevices[j].getDeviceInfo().getVendor() + " " + 
                mididevices[j].getDeviceInfo().getName();
//                 + " " + mididevices[j].getDeviceInfo().getDescription()
            content.add(new JLabel(desc));
            // input columns
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
        SpringUtilities.makeCompactGrid(content, mididevices.length+2, names.length+1, 
                                        15, 15, 5, 5);
        return content;
    }

    public JFrame getFrame()
        throws MidiUnavailableException {
        if(frame == null){
            JComponent panel = getPanel();
            Box box = Box.createHorizontalBox();
            JButton button = new JButton("update");
            if(updateAction != null)
                button.addActionListener(updateAction);
            button.addActionListener(new AbstractAction(){
                    public void actionPerformed(ActionEvent event){
                        frame.setVisible(false);
                    }
                });
            box.add(button);
            button = new JButton("cancel");
            if(cancelAction != null)
                button.addActionListener(cancelAction);
            button.addActionListener(new AbstractAction(){
                    public void actionPerformed(ActionEvent event){
                        frame.setVisible(false);
                    }
                });
            box.add(button);
            panel.add(box);
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