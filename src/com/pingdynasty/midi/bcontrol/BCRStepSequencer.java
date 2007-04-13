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
            RotaryEncoder note = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 1+i, 60);
            content.add(note.getComponent());
            list.add(note);
            RotaryEncoder velocity = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 81+i, 80);
            content.add(velocity.getComponent());
            list.add(velocity);
            RotaryEncoder duration = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 89+i, 80);
            content.add(duration.getComponent());
            list.add(duration);
            RotaryEncoder modulation = new RotaryEncoder(ShortMessage.CONTROL_CHANGE, channel, 97+i, 0);
            content.add(modulation.getComponent());
            list.add(modulation);
            steps[i] = new BCRStep(note, velocity, duration, modulation);
        }
        encoders = new RotaryEncoder[list.size()];
        list.toArray(encoders);
        sequencer = new StepSequencer(null, steps);

        this.add(content, BorderLayout.CENTER);

        // buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        // midi config button
        JButton button = new JButton("config");
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
        content.add(buttons, BorderLayout.EAST);
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
//             for(int i=0; i<encoders.length; ++i)
//                 encoders[i].setTransmitter(device.getTransmitter());
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
            for(int i=0; i<encoders.length; ++i)
                encoders[i].setReceiver(receiver);
        }
    }

    public void status(String msg){
        System.out.println(msg);
        statusbar.setText(msg);
    }

    public static void main(String[] args)
        throws Exception {
        BCRStepSequencer seq = new BCRStepSequencer();
        seq.initialiseMidiDevices();
        // create frame
        JFrame frame = new JFrame("step sequencer");
        // configure frame
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(seq);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}