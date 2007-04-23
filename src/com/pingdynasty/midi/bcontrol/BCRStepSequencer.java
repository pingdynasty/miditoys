package com.pingdynasty.midi.bcontrol;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.net.URL;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.event.*;

// todo:
// - have a steps-per-beat control (1,2,3,4)
// - additional step control: configurable CC controls
// - global controls for one of the modes:
// - - tempo
// - - global bend, modulation, cc
// - - key controls: scale, octave, velocity
// - new class for push encoder
// - labels to show functionality ?
// - tooltip messages to show functionality in statusbar
// - message window for top right corner - show value of currently focused component
// - random value generator
// - change first row of buttons to control step on/off (which mode?)
// - blink first row of buttons when steps are playing? (only in above mode?)
//// maybe have a separate screen indicator instead. what about arp plays?
// - output encoder values - notes, periods (as beat fractions), plain values
// - change switch-pressed image to look depressed (to give real action feel)
// - sort out midi channel issue: Java midi channels are 1 less
public class BCRStepSequencer extends JPanel {
    private StepSequencer sequencer;
    private MidiControl[] controls;
    private MidiControl[] cc_controls; // quick index for CC controls
    private int width = 8;
    private JLabel statusbar;
    private JSlider slider;
    private EventHandler eventHandler = new EventHandler();

    // MIDI handlers
    private ReceiverPlayer midiOutput;
    private StepSequencerArpeggio midiInput;
    private ControlSurfaceHandler midiControl;
    private BCRStepSequencerConfiguration devicepanel;

    public class AboutFrame extends JFrame {
        public AboutFrame(){
            super("about");
            Icon icon = ResourceLocator.getIcon("bcr-steps/about.png");
            getContentPane().add(new JLabel(icon));
            addMouseListener(new MouseAdapter(){
                    public void mouseClicked(MouseEvent e){
                        setVisible(false);
                        dispose();
                    }
                });
            setResizable(false);
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            pack();
            Dimension dim = getToolkit().getScreenSize();
            setLocation(dim.width/2 - getWidth()/2, dim.height/2 - getHeight()/2);
        }
    }

    public class HelpFrame extends JFrame {
        public HelpFrame(){
            super("help");
            try{
                URL url = ResourceLocator.getResourceURL("bcr-steps/help.html");
                JEditorPane text = new JEditorPane(url);
                text.setEditable(false);
                text.setMargin(new Insets(8, 8, 16, 16));
                text.addHyperlinkListener(new HyperlinkListener() {
                        public void hyperlinkUpdate(HyperlinkEvent e){
                            if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
                                JEditorPane pane = (JEditorPane)e.getSource();
                                try{
                                    pane.setPage(e.getURL());
                                }catch(Exception exc){
                                    status(exc.toString());
                                    exc.printStackTrace();
                                }
                            }
                        }
                    });
                getContentPane().add(new JScrollPane(text));
            }catch(Exception exc){exc.printStackTrace();}
            setSize(500, 500);
            Dimension dim = getToolkit().getScreenSize();
            setLocation(dim.width/2 - getWidth()/2, dim.height/2 - getHeight()/2);
        }
    }

    public class EventHandler implements Control.Callback {
        private int mode = MODE_A;
        private static final int MODE_A = 0;
        private static final int MODE_B = 1;
        private static final int MODE_C = 2;
        private static final int MODE_D = 3;

        public void setMode(int mode){
            if(mode > MODE_B)
                return;
            if(this.mode == mode)
                return;
            this.mode = mode;
            try{
                // update reassignable encoders
                switch(mode){
                case MODE_A :
                    for(int i=0; i<width; ++i)
                        cc_controls[i+81].setValue(sequencer.getStep(i).getVelocity());
                    for(int i=0; i<width; ++i)
                        cc_controls[i+89].setValue(sequencer.getStep(i).getDuration());
                    for(int i=0; i<width; ++i)
                        cc_controls[i+97].setValue(sequencer.getStep(i).getDelay());
                    status("mode A");
                    break;
                case MODE_B :
                    for(int i=0; i<width; ++i)
                        cc_controls[i+81].setValue(sequencer.getStep(i).getModulation());
                    for(int i=0; i<width; ++i)
                        cc_controls[i+89].setValue(sequencer.getStep(i).getBend());
                    for(int i=0; i<width; ++i)
                        cc_controls[i+97].setValue(0);
                    status("mode B");
                    break;
                }
                }catch(Exception exc){exc.printStackTrace();}
        }

        public int getMode(){
            return mode;
        }

        public void action(int command, int channel, int data1, int data2){
//             status("action: "+command+" channel "+channel+
//                    " data1 "+data1+" data2 "+data2);
            if(data1 >= 1 && data1 <= 8){
                // push encoder turned
                sequencer.getStep(data1 - 1).setNote(data2);
                status("note "+NoteParser.getStringNote(data2));
            }else if(data1 >= 33 && data1 <= 40){
                // push encoder pressed
                if(data2 > 63)
                    sequencer.play(sequencer.getStep(data1 - 33));
            }else if(data1 >= 65 && data1 <= 80){
                // button row 1 or 2 pressed
                // arbitrary range: 52 - 68
                // arbitrary velocity: 80
                int note = data1 - 12;
                int velocity = 80;
                if(data2 < 64)
                    midiInput.noteoff(note);
                else
                    midiInput.noteon(note, velocity);
                // note: in order to pass on channel information, we need to do
                // midiOutput.setChannel(channel);
                status(NoteParser.getStringNote(note)+" arpeggio");
            }else if(data1 >= 105 && data1 <= 108){
                // mode buttons - four buttons in bottom right corner
                setMode(data1 - 105);
                // turn off three other buttons and make sure this one is on
                for(int i=105; i<=108; ++i)
                    try{
                        cc_controls[i].setValue(mode == i - 105 ? 127 : 0);
                    }catch(Exception exc){exc.printStackTrace();}
            }else if(data1 == 115){
                // store button
                if(data2 < 64){
                    sequencer.stop();
                    status("stop");
                }else{
                    sequencer.start();
                    status("start");
                }
            }else if(data1 == 116){
                // learn button
                if(data2 < 64){
                    status("MIDI sync off");
                    midiInput.setMidiSync(false);
                    slider.setEnabled(true);
                    sequencer.setPeriod(60000 / slider.getValue());
                }else{
                    status("MIDI sync on");
                    midiInput.setMidiSync(true);
                    slider.setEnabled(false);
                }
            }else{
                if(data1 >= 81 && data1 <= 104)
                    status("value "+data2);
                switch(mode){
                case MODE_A :
                    if(data1 >= 81 && data1 <= 88){
                        // top row simple encoder (below buttons)
                        sequencer.getStep(data1 - 81).setVelocity(data2);
                    }else if(data1 >= 89 && data1 <= 96){
                        // second row simple encoder
                        sequencer.getStep(data1 - 89).setDuration(data2);
                    }else if(data1 >= 97 && data1 <= 104){
                        // third row simple encoder
                        sequencer.getStep(data1 - 97).setDelay(data2);
                    }
                    break;
                case MODE_B :
                    if(data1 >= 81 && data1 <= 88){
                        // top row simple encoder (below buttons)
                        sequencer.getStep(data1 - 81).setModulation(data2);
                    }else if(data1 >= 89 && data1 <= 96){
                        // second row simple encoder
                        sequencer.getStep(data1 - 89).setBend(data2);
//                     }else if(data1 >= 97 && data1 <= 104){
                        // third row simple encoder
                        // disabled for now
                    }
                    break;
                }
            }
        }
    }

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
            case ShortMessage.START: {
                status("start");
                sequencer.start();
                break;
            }
            case ShortMessage.STOP: {
                status("stop");
                sequencer.stop();
                break;
            }
            case ShortMessage.CONTROL_CHANGE: {
                cc_controls[msg.getData1()].send(msg, -1);
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
        midiOutput = new SchedulingPlayer(null);
        // the channel that all controls are tuned to listen and talk on
        int channel = 0;

        // statusbar
        statusbar = new JLabel();
        statusbar.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(statusbar, BorderLayout.SOUTH);

        JPanel mainarea = new JPanel();
        mainarea.setLayout(new BoxLayout(mainarea, BoxLayout.Y_AXIS));
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.X_AXIS));
//         JPanel mainarea = new JPanel(new GridLayout(0, width));
        // create rotary encoders and buttons
        List list = new ArrayList();
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i, 60,
                                  "step "+(1+i)+" note");
            list.add(control);
            rows.add(control.getComponent());
            // add 'invisible' button to back up the push encoder
            // todo: replace with PushEncoder class that can handle double-click as press
            TriggerButton trigger = new TriggerButton(1+i, ShortMessage.CONTROL_CHANGE, channel, 33+i, 0, "not in use");
            list.add(trigger);
        }
        mainarea.add(rows);
        // two rows of simple buttons
        rows = new JPanel(new GridLayout(2, width));
        for(int i=0; i<width; ++i){
            TriggerButton trigger = 
                new TriggerButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
                                  "play "+NoteParser.getStringNote(i+53)+" arpeggio");
            list.add(trigger);
            rows.add(trigger.getComponent());
        }
        for(int i=0; i<width; ++i){
            TriggerButton trigger = 
                new TriggerButton(41+i, ShortMessage.CONTROL_CHANGE, channel, 73+i, 0,
                                  "play "+NoteParser.getStringNote(i+61)+" arpeggio");
            list.add(trigger);
            rows.add(trigger.getComponent());
        }
        mainarea.add(rows);
        rows = new JPanel(new GridLayout(3, width));

        // first row of simple encoders (below buttons)
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(33+i, ShortMessage.CONTROL_CHANGE, channel, 81+i, 80,
                                  "step "+(1+i)+" velocity/modulation");
            list.add(control);
            rows.add(control.getComponent());
        }

        // second row of simple encoders
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(41+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, 80,
                                  "step "+(1+i)+" duration/bend");
            list.add(control);
            rows.add(control.getComponent());
        }

        // third row of simple encoders
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(49+i, ShortMessage.CONTROL_CHANGE, channel, 97+i, 0,
                                  "step "+(1+i)+" delay");
            list.add(control);
            rows.add(control.getComponent());
        }
        mainarea.add(rows);

        this.add(mainarea, BorderLayout.CENTER);

        Box buttonarea = new Box(BoxLayout.Y_AXIS);
        JLabel label = new JLabel(ResourceLocator.getIcon("bcr-steps/icon.png"));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        buttonarea.add(label);
        buttonarea.add(buttonarea.createVerticalStrut(50));

        // encoder buttons
        ToggleButton[] toggles = new ToggleButton[4];
        for(int i=0; i<4; ++i){
            toggles[i] = new ToggleButton(57+i, ShortMessage.CONTROL_CHANGE, channel, 111+i, 0, "not in use");
            list.add(toggles[i]);
        }
        addFourButtons(buttonarea, toggles);
        buttonarea.add(buttonarea.createVerticalStrut(20));

        // store/learn/edit/exit
        toggles = new ToggleButton[4];
        String[] tooltips = new String[]{"start/stop", "MIDI sync", "not in use", "not in use"};
        for(int i=0; i<4; ++i){
            toggles[i] = new ToggleButton(53+i, ShortMessage.CONTROL_CHANGE, channel, 115+i, 0, tooltips[i]);
            list.add(toggles[i]);
        }
        addFourButtons(buttonarea, toggles);
        buttonarea.add(buttonarea.createVerticalStrut(20));

        // preset left/right
        toggles = new ToggleButton[2];
        for(int i=0; i<2; ++i){
            toggles[i] = new ToggleButton(63+i, ShortMessage.CONTROL_CHANGE, channel, 120+i, 0, "not in use");
            list.add(toggles[i]);
        }
        addFourButtons(buttonarea, toggles);
        buttonarea.add(buttonarea.createVerticalStrut(20));

        // four simple buttons bottom right
        toggles = new ToggleButton[4];
        tooltips = new String[]{"mode A", "mode B", "not in use", "not in use"};
        for(int i=0; i<4; ++i){
            toggles[i] = new ToggleButton(49+i, ShortMessage.CONTROL_CHANGE, channel, 105+i, 
                                          eventHandler.getMode() == i ? 127 : 0, tooltips[i]);
            list.add(toggles[i]);
        }
        addFourButtons(buttonarea, toggles);

        this.add(buttonarea, BorderLayout.EAST);

        controls = new MidiControl[list.size()];
        list.toArray(controls);

        cc_controls = new MidiControl[128];
        for(int i=0; i<controls.length; ++i)
            if(controls[i].getCommand() == ShortMessage.CONTROL_CHANGE)
                cc_controls[controls[i].getData1()] = controls[i];

        sequencer = new StepSequencer(midiOutput, width);
        midiInput = new StepSequencerArpeggio(sequencer);
        midiControl = new ControlSurfaceHandler();

        for(int i=0; i<controls.length; ++i)
            controls[i].setCallback(eventHandler);

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

    private void addFourButtons(JComponent component, MidiControl[] controls){
//         JPanel panel = new JPanel(new GridLayout(0, 2));
//         for(int i=0; i<controls.length; ++i)
//             panel.add(controls[i].getComponent());
//         component.add(panel);
        Box box = new Box(BoxLayout.Y_AXIS);
        Box row = new Box(BoxLayout.X_AXIS);
        row.add(controls[0].getComponent());
        row.add(controls[1].getComponent());
        box.add(row);
        if(controls.length > 2){
            row = new Box(BoxLayout.X_AXIS);
            row.add(controls[2].getComponent());
            row.add(controls[3].getComponent());
        }
        box.add(row);
        component.add(box);
    }

    private void addTwoButtons(JComponent component, MidiControl high, MidiControl low){
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(high.getComponent());
        box.add(low.getComponent());
        component.add(box);
    }

    public void initialiseMidiDevices()
        throws MidiUnavailableException {

        devicepanel = new BCRStepSequencerConfiguration();

        // lock into MIDI config frame
        devicepanel.getFrame().addWindowListener(new WindowAdapter() {
                public void windowDeactivated(WindowEvent e){
                    try{
                        System.out.println("deactivated");
                        updateMidiDevices();
                    }catch(Exception exc){exc.printStackTrace();}
                }
                public void windowClosing(WindowEvent e){
                    try{
                        System.out.println("closed");
                        updateMidiDevices();
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });

        updateMidiDevices();
    }

    public void updateMidiDevices()
        throws MidiUnavailableException {
        // update devices from devicepanel settings
        MidiDevice device = devicepanel.getMidiInput();
        if(device != null){
            device.open();
            midiInput.setTransmitter(device.getTransmitter());
        }

        device = devicepanel.getMidiControlInput();
        if(device != null){
            device.open();
            midiControl.setTransmitter(device.getTransmitter());
        }

        device = devicepanel.getMidiOutput();
        if(device != null){
            device.open();
            midiOutput.setReceiver(device.getReceiver());
            midiOutput.setChannel(devicepanel.getChannel());
            status("MIDI output: "+device.getDeviceInfo().getName());
        }

        device = devicepanel.getMidiControlOutput();
        if(device != null){
            device.open();
            Receiver receiver = device.getReceiver();
            status("control output: "+receiver);
            try{
                if(devicepanel.doSysex())
                    sendSysexMessages(receiver);
                for(int i=0; i<controls.length; ++i){
                    controls[i].setReceiver(receiver);
                    if(!devicepanel.doSysex()){
                        // otherwise done by the .default sysex message
                        controls[i].updateMidiControl();
                    }
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
        BCRSysexMessage.createMessage(list, "  .egroups 1");
        BCRSysexMessage.createMessage(list, "  .fkeys off");
        BCRSysexMessage.createMessage(list, "  .lock off");
        BCRSysexMessage.createMessage(list, "  .init");
        for(int i=0; i<controls.length; ++i)
            controls[i].generateSysexMessages(list);
        BCRSysexMessage.createMessage(list, " ");
        BCRSysexMessage.createMessage(list, "$end");
        for(int i=0; i<list.size(); ++i)
            receiver.send((MidiMessage)list.get(i), -1);
    }

    public JMenuBar getMenuBar(){
        // add menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("bcr steps");
        AbstractAction action = new AbstractAction("about"){
                public void actionPerformed(ActionEvent event) {
                    JFrame frame = new AboutFrame();
                    frame.setVisible(true);
                }
            };
        menu.add(action);
        action = new AbstractAction("setup"){
                public void actionPerformed(ActionEvent event) {
                    try{
                        devicepanel.open();
                    }catch(Exception exc){exc.printStackTrace();}
                }
            };
        menu.add(new JMenuItem(action));
        action = new AbstractAction("help"){
                public void actionPerformed(ActionEvent event) {
                    JFrame frame = new HelpFrame();
                    frame.setVisible(true);
                }
            };
        menu.add(action);
        menubar.add(menu);
        return menubar;
    }

    public void destroy(){
        midiInput.close();
        midiOutput.close();
        midiControl.close();
//         devicepanel.getFrame().dispose();
    }

    public static void main(String[] args)
        throws Exception {
        BCRStepSequencer seq = new BCRStepSequencer();
        seq.initialiseMidiDevices();
        // create frame
        JFrame frame = new JFrame("bcr steps");
        frame.setJMenuBar(seq.getMenuBar());
        // configure frame
//         frame.pack();
        frame.setSize(625, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(seq);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}