package com.pingdynasty.midi;

import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.midi.*;

public class DeviceLocator {

//     private MidiDevice device;
//     private Player player;

//     class DeviceActionListener implements ActionListener {
//         protected MidiDevice dev;
//         public DeviceActionListener(MidiDevice dev){
//             this.dev = dev;
//         }
//         public void actionPerformed(ActionEvent event) {
//             device = dev;
//             try{
//                 int velocity = player.getVelocity();
//                 int duration = player.getDuration();
//                 player.close();
//                 device.open();
//                 player = new ReceiverPlayer(device.getReceiver());
//                 player.setVelocity(velocity);
//                 player.setDuration(duration);
//             }catch(Exception exc){exc.printStackTrace();}
//         }
//     }

//     public DeviceLocator(){

//     }

//     public MidiDevice getCurrentDevice(){
//         return device;
//     }

//     public JMenu getMenu(){
//         JMenu menu = new JMenu("Devices");
//         MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
//         MidiDevice[] devices = new MidiDevice[info.length];
//         for(int i=0; i<info.length; ++i){
//             devices[i] = MidiSystem.getMidiDevice(info[i]); 
//             if(devices[i] instanceof Receiver){
//                 JMenuItem item = new JMenuItem(info[i].getName());
//                 item.addActionListener(new DeviceActionListener(devices[i]));
//                 menu.add(item);
//             }
//         }
//         return menu;
//     }

    /**
     * return first matching device
     */
    public static MidiDevice getDevice(Class type)
        throws MidiUnavailableException {
        MidiDevice device = null;
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length && device == null; ++i){
            devices[i] = MidiSystem.getMidiDevice(info[i]);
            if(type.isInstance(devices[i]))
                device = devices[i];
        }
        return device;
    }

    public static Player getPlayer(Class type)
        throws MidiUnavailableException {
        MidiDevice device = getDevice(type);
        return getPlayer(device);
    }


    public static String[] getDeviceNames(Class type)
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        List list = new ArrayList();
        for(int i=0; i<info.length; ++i){
            if(type.isInstance(MidiSystem.getMidiDevice(info[i])))
                list.add(info[i].getName());
        }
        String[] names = new String[list.size()];
        list.toArray(names);
        return names;
    }

    public static String[] getTransmitterDeviceNames()
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        List list = new ArrayList();
        for(int i=0; i<info.length; ++i){
            if(MidiSystem.getMidiDevice(info[i]).getMaxTransmitters() != 0)
                list.add(info[i].getName());
        }
        String[] names = new String[list.size()];
        list.toArray(names);
        return names;
    }

    public static String[] getReceiverDeviceNames()
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        List list = new ArrayList();
        for(int i=0; i<info.length; ++i){
            if(MidiSystem.getMidiDevice(info[i]).getMaxReceivers() != 0)
                list.add(info[i].getName());
        }
        String[] names = new String[list.size()];
        list.toArray(names);
        return names;
    }

    public static MidiDevice getDevice(String name)
        throws MidiUnavailableException {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        for(int i=0; i<info.length; ++i){
            if(info[i].getName().equals(name))
                return MidiSystem.getMidiDevice(info[i]);
        }
        return null;
    }

    public static Player getPlayer(String name)
        throws MidiUnavailableException {
        return getPlayer(getDevice(name));
    }

    public static Player getPlayer(MidiDevice device)
        throws MidiUnavailableException {
        if(device == null)
            return null;
        device.open();
//         if(device instanceof Synthesizer)
//             return new SynthesizerPlayer((Synthesizer)device);
        return new ReceiverPlayer(device.getReceiver());
    }

//     public static MidiDevice getDevice(byte[] identifier)
//         throws MidiUnavailableException {
}
