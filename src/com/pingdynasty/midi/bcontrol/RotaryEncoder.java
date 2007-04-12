package com.pingdynasty.midi.bcontrol;

import java.util.List;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;

public class RotaryEncoder {

    int value;
//     int min; // 0-16383
//     int max;
    private ShortMessage msg;
    private int command;
    private int channel;
    private int data1;
    private int data2;

    public RotaryEncoder(int command, int channel, int data1, int data2){
        this.command = command;
        this.channel = channel;
        this.data1 = data1;
        this.data2 = data2;
        msg = new ShortMessage();
    }

    public MidiMessage getMidiMessage()
        throws InvalidMidiDataException {
        msg.setMessage(command, channel, data1, data2);
        return msg;
    }

    public void generateSysexMessages(List messages, int encoder)
        throws InvalidMidiDataException {
        int index = messages.size();
        // encoder start message
        BCRSysexMessage sysex = new BCRSysexMessage(index++);
        sysex.setMessage("$encoder "+encoder);
        messages.add(sysex);
        // easypar message
        sysex = new BCRSysexMessage(index++);
        // assumes ShortMessage.CONTROL_CHANGE
        sysex.setMessage("  .easypar CC "+channel+" "+data1+" 0 127 absolute");
        messages.add(sysex);
        // showvalue message
        sysex = new BCRSysexMessage(index++);
        sysex.setMessage("  .showvalue on");
        messages.add(sysex);
        // mode message
        sysex = new BCRSysexMessage(index++);
        sysex.setMessage("  .mode 12dot");
        messages.add(sysex);
    }

    public static void createMessage(List messages, String data)
        throws Exception {
        BCRSysexMessage sysex = new BCRSysexMessage(messages.size());
        sysex.setMessage(data);
        messages.add(sysex);
    }

    public static final void main(String[] args)
        throws Exception {
        List messages = new java.util.ArrayList();
        createMessage(messages, "$rev R1");
//         createMessage(messages, "$preset");
//         createMessage(messages, "  .name 'bcr keyboard control    '");
//         createMessage(messages, "  .snapshot off");
//         createMessage(messages, "  .request off");
//         createMessage(messages, "  .egroups 4");
//         createMessage(messages, "  .fkeys on");
//         createMessage(messages, "  .lock off");
//         createMessage(messages, "  .init");

        RotaryEncoder[] knobs = new RotaryEncoder[]{
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 1, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 2, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 3, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 4, 127),
            new RotaryEncoder(ShortMessage.CONTROL_CHANGE, 1, 5, 127)
        };
        for(int i=0; i<knobs.length; ++i)
            knobs[i].generateSysexMessages(messages, i+1);

        createMessage(messages, " ");
        createMessage(messages, "$end");

        String[] names = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<names.length; ++i)
            System.out.println(i+": "+names[i]);
        System.out.println("enter number of device");
        int pos = System.in.read() - 48;
        MidiDevice device = DeviceLocator.getDevice(names[pos]);
        device.open();
        Receiver receiver = device.getReceiver();
        for(int i=0; i<messages.size(); ++i)
            receiver.send((MidiMessage)messages.get(i), -1);
        System.out.println("Sysex messages sent to "+names[pos]);
    }
}