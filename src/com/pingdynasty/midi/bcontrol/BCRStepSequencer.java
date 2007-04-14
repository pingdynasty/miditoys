package com.pingdynasty.midi.bcontrol;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
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
    private MidiControl[] controls;
    private int channel = 1;
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
//                 System.out.println("midi message "+message);
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

                // todo: change MidiControl.setValue() to not do updates
                // find correct MidiControl, call setValue() and updateMidiControl()
                if(cmd >= 1 && cmd <= 8){
                    sequencer.getStep(cmd-1).setNote(msg.getData2());
                }else if(cmd >= 81 && cmd <= 88){
                    sequencer.getStep(cmd-81).setVelocity(msg.getData2());
                }else if(cmd >= 89 && cmd <= 96){
                    sequencer.getStep(cmd-89).setDuration(msg.getData2());
                }else if(cmd >= 97 && cmd <= 104){
                    sequencer.getStep(cmd-97).setModulation(msg.getData2());
                }else if(cmd >= 65 && cmd <= 81){
                    // todo: maintain a map or index of CC to controls
                    // remove these hardcoded mappings
                    try{
                        controls[cmd - 57].setValue(msg.getData2());
                    }catch(Exception exc){exc.printStackTrace();}
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

        // create rotary encoders and buttons
        List list = new ArrayList();
        BCRStep[] steps = new BCRStep[width];
        for(int i=0; i<width; ++i)
            list.add(new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i, 60));
        for(int i=0; i<width*2; ++i)
            list.add(new ToggleButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0));
        for(int i=0; i<width; ++i)
            list.add(new RotaryEncoder(33+i, ShortMessage.CONTROL_CHANGE, channel, 81+i, 80));
        for(int i=0; i<width; ++i)
            list.add(new RotaryEncoder(41+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, 80));
        for(int i=0; i<width; ++i)
            list.add(new RotaryEncoder(49+i, ShortMessage.CONTROL_CHANGE, channel, 97+i, 0));
        controls = new MidiControl[list.size()];
        list.toArray(controls);

        for(int i=0; i<width; ++i)
            steps[i] = new BCRStep(controls[i], controls[i+(width*3)], controls[i+(width*4)], controls[i+(width*5)]);

        sequencer = new StepSequencer(null, steps);

        for(int i=0; i<controls.length; ++i)
            content.add(controls[i].getComponent());
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
                sendSysexMessages(receiver);
                for(int i=0; i<controls.length; ++i)
                    controls[i].setReceiver(receiver);
                    // done by the .default sysex message
//                     controls[i].updateMidiControl();
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
//         BCRSysexMessage.createMessage(list, "$preset");
//         BCRSysexMessage.createMessage(list, "  .name 'bcr keyboard control    '");
//         BCRSysexMessage.createMessage(list, "  .snapshot off");
//         BCRSysexMessage.createMessage(list, "  .request off");
//         BCRSysexMessage.createMessage(list, "  .egroups 4");
//         BCRSysexMessage.createMessage(list, "  .fkeys on");
//         BCRSysexMessage.createMessage(list, "  .lock off");
//         BCRSysexMessage.createMessage(list, "  .init");
        for(int i=0; i<controls.length; ++i)
            controls[i].generateSysexMessages(list);
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