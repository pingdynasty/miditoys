package com.pingdynasty.midi.bcontrol;

import java.util.List;
import java.util.ArrayList;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

// todo:
// have a steps-per-beat control (1,2,3,4)
// additional step control: delay period (before), configurable CC controls
// separate graphical/midi controls from functions, ie one knob can have several functions
public class BCRStepSequencer extends JPanel {
    private StepSequencer sequencer;
    private RotaryEncoder[] encoders;
    private int channel = 0;
    private JLabel statusbar;
    private JSlider slider;

    // MIDI handlers
    private ReceiverPlayer midiOutput;
    private ControlSurfaceHandler midiControl;
    private final static String midiControlInputName = "Control input";
    private final static String midiControlOutputName = "Control output";
    private final static String midiInputName = "MIDI input";
    private final static String midiOutputName = "MIDI output";
    private DevicePanel devicepanel = 
        new DevicePanel(new String[]{midiInputName, midiControlInputName},  
                        new String[]{midiOutputName, midiControlOutputName});

    public class ControlSurfaceHandler implements Receiver {
        private Transmitter transmitter;

        public void setTransmitter(Transmitter transmitter){
            if(this.transmitter == transmitter)
                return;
            if(this.transmitter != null)
                this.transmitter.close();
            this.transmitter = transmitter;
            transmitter.setReceiver(this);
        }

        public void send(MidiMessage message, long time){
            if(message instanceof ShortMessage){
                try{
                    send((ShortMessage)message, time);
                }catch(Exception exc){
                    exc.printStackTrace();
                }
            }else{
                System.out.println("midi message "+message);
                return;
            }
        }

        public void send(ShortMessage msg, long time){
            switch(msg.getStatus()){
            case ShortMessage.CONTROL_CHANGE: {
//             status("midi cc <"+msg+"><"+time+"><"+
//                    msg.getCommand()+"><"+msg.getChannel()+"><"+
//                    msg.getData1()+"><"+msg.getData2()+">");
                int cmd = msg.getData1();
                // note: BCRStep.setXXX() will send out a CC message
                if(cmd >= 1 && cmd <= 8){
                    sequencer.getStep(cmd-1).setNote(msg.getData2());
                }else if(cmd >= 81 && cmd <= 88){
                    sequencer.getStep(cmd-81).setVelocity(msg.getData2());
                }else if(cmd >= 89 && cmd <= 96){
                    sequencer.getStep(cmd-89).setDuration(msg.getData2());
                }else if(cmd >= 97 && cmd <= 104){
                    sequencer.getStep(cmd-97).setModulation(msg.getData2());
                }else if(cmd >= 33 && cmd <= 40){
                    sequencer.play(sequencer.getStep(cmd-33));
                }else if(cmd == 105){
                    if(sequencer.isStarted())
                        sequencer.stop();
                    else
                        sequencer.start();
                }
                repaint();
                break;
            }
            case ShortMessage.NOTE_ON:
            case ShortMessage.NOTE_OFF:
            case ShortMessage.PITCH_BEND:
                break;
            default:
                status("midi control <"+msg+"><"+msg.getStatus()+">");
            }
        }

        public void close(){
            if(this.transmitter != null)
                this.transmitter.close();
        }
    }

    public BCRStepSequencer(){
        super(new BorderLayout());
        int width = 8;
        midiControl = new ControlSurfaceHandler();

        // statusbar
        statusbar = new JLabel();
        this.add(statusbar, BorderLayout.SOUTH);

        JPanel content = new JPanel();
        content.setLayout(new GridLayout(0, width));

        // rotary encoders and steps
        List list = new ArrayList();
        BCRStep[] steps = new BCRStep[width];
        for(int i=0; i<width; ++i){
            RotaryEncoder note = new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i, 60);
            list.add(note);
            RotaryEncoder velocity = new RotaryEncoder(81+i, ShortMessage.CONTROL_CHANGE, channel, 81+i, 80);
            list.add(velocity);
            RotaryEncoder duration = new RotaryEncoder(89+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, 80);
            list.add(duration);
            RotaryEncoder modulation = new RotaryEncoder(97+i, ShortMessage.CONTROL_CHANGE, channel, 97+i, 0);
            list.add(modulation);
            steps[i] = new BCRStep(note, velocity, duration, modulation);
        }
        encoders = new RotaryEncoder[list.size()];
        list.toArray(encoders);
        for(int j=0; j<4; ++j)
            for(int i=0; i<encoders.length / 4; ++i)
                content.add(encoders[i*4+j].getComponent());
        sequencer = new StepSequencer(null, steps);

        this.add(content, BorderLayout.CENTER);

        // buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        // play button
        JButton button = new JButton("play");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("play");
                    sequencer.play();
                }
            });
        buttons.add(button);
        // start button
        button = new JButton("start");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("start");
                    sequencer.start();
                }
            });
        // stop button
        buttons.add(button);
        button = new JButton("stop");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("stop");
                    sequencer.stop();
                }
            });
        buttons.add(button);
        // midi config button
        button = new JButton("config");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    try{
                        devicepanel.getFrame().addWindowListener(new WindowAdapter() {
                                public void windowClosing(WindowEvent e){
                                    try{
                                        updateMidiDevices();
                                    }catch(Exception exc){exc.printStackTrace();}
                                }
                                public void windowDeactivated(WindowEvent e){
                                    try{
                                        updateMidiDevices();
                                    }catch(Exception exc){exc.printStackTrace();}
                                }
                            });
                        devicepanel.open();
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
            });
        buttons.add(button);
        this.add(buttons, BorderLayout.EAST);

        // BPM control
        slider = new JSlider(JSlider.HORIZONTAL, 20, 380, 
                             60000/sequencer.getPeriod());
        //Turn on labels at major tick marks.
        slider.setMajorTickSpacing(60);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
                    if(!source.getValueIsAdjusting()){
                        int bpm = (int)source.getValue();
                        sequencer.setPeriod(60000 / bpm);
                    }
                }
            });
        this.add(slider, BorderLayout.NORTH);
    }

    public void initialiseMidiDevices()
        throws MidiUnavailableException {
        // initialise MIDI out
        MidiDevice device = DeviceLocator.getDevice(Synthesizer.class);
        devicepanel.setDevice(midiOutputName, device);
        device.open();
        status("MIDI OUT device: "+device.getDeviceInfo().getName());
        midiOutput = new SchedulingPlayer(device.getReceiver());
        sequencer.setPlayer(midiOutput);

        // try to initialise BCR
        device = DeviceLocator.getDevice("Port 1 (MidiIN:3)");
        devicepanel.setDevice(midiControlInputName, device);
        device = DeviceLocator.getDevice("Port 1 (MidiOUT:3)");
        devicepanel.setDevice(midiControlOutputName, device);
        updateMidiDevices();
    }

    public void updateMidiDevices()
        throws MidiUnavailableException {
        status("updating midi devices");
        // update devices from devicepanel settings
            MidiDevice device;
//         MidiDevice device = devicepanel.getDevice(midiInputName);
//         if(device != null){
//             device.open();
//             midiInput.setTransmitter(device.getTransmitter());
//         }

        device = devicepanel.getDevice(midiControlInputName);
        if(device != null){
            device.open();
//             Transmitter transmitter = device.getTransmitter();
            status("control input: "+device);
            midiControl.setTransmitter(device.getTransmitter());
        }

        device = devicepanel.getDevice(midiOutputName);
        if(device != null){
            device.open();
            midiOutput.setReceiver(device.getReceiver());
        }

        device = devicepanel.getDevice(midiControlOutputName);
        if(device != null){
            device.open();
            Receiver receiver = device.getReceiver();
            status("control output: "+receiver);
            try{
//                 sendSysexMessages(receiver);
                for(int i=0; i<encoders.length; ++i){
                    encoders[i].setReceiver(receiver);
                    // done by the .default sysex message
                    encoders[i].updateMidiControl();
                }
            }catch(InvalidMidiDataException exc){exc.printStackTrace();}
        }
    }

    public void status(String msg){
        System.out.println(msg);
        statusbar.setText(msg);
    }

    public void sendSysexMessages(Receiver receiver)
        throws InvalidMidiDataException {
        List list = new ArrayList();
        BCRSysexMessage.createMessage(list, "$rev R1");
        BCRSysexMessage.createMessage(list, "$preset");
        BCRSysexMessage.createMessage(list, "  .name 'bcr keyboard control    '");
        BCRSysexMessage.createMessage(list, "  .snapshot off");
        BCRSysexMessage.createMessage(list, "  .request off");
        BCRSysexMessage.createMessage(list, "  .egroups 4");
        BCRSysexMessage.createMessage(list, "  .fkeys on");
        BCRSysexMessage.createMessage(list, "  .lock off");
        BCRSysexMessage.createMessage(list, "  .init");
        for(int i=8; i<encoders.length; ++i)
            encoders[i].generateSysexMessages(list);
//             for(int i=0; i<8; ++i)
//                 new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i, 60).generateSysexMessages(list);
//             new RotaryEncoder(1, ShortMessage.CONTROL_CHANGE, 1, 1, 50).generateSysexMessages(list);
//             new RotaryEncoder(2, ShortMessage.CONTROL_CHANGE, 1, 2, 60).generateSysexMessages(list);
//             new RotaryEncoder(3, ShortMessage.CONTROL_CHANGE, 1, 3, 70).generateSysexMessages(list);
//             new RotaryEncoder(4, ShortMessage.CONTROL_CHANGE, 1, 4, 80).generateSysexMessages(list);
//             new RotaryEncoder(5, ShortMessage.CONTROL_CHANGE, 1, 5, 90).generateSysexMessages(list);
        BCRSysexMessage.createMessage(list, " ");
        BCRSysexMessage.createMessage(list, "$end");
        for(int i=0; i<list.size(); ++i)
            receiver.send((MidiMessage)list.get(i), -1);
    }

    public static void main(String[] args)
        throws Exception {
        BCRStepSequencer seq = new BCRStepSequencer();
        seq.initialiseMidiDevices();
        // create frame
        JFrame frame = new JFrame("step sequencer");
        // configure frame
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(seq);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}