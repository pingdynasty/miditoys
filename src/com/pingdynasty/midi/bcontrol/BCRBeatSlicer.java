package com.pingdynasty.midi.bcontrol;

import java.io.File;
import java.net.URL;
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
import javax.swing.event.*;
// import javax.swing.filechooser.FileFilter;

public class BCRBeatSlicer extends JPanel {
    private BeatSlicer slicer;
    private MidiControl[] controls;
    private MidiControl[] cc_controls; // quick index for CC controls
    private int width = 8;
    private JLabel statusbar;
    private JSlider slider;
    private EventHandler eventHandler;
    private WaveformPanel waveform;

    private MidiSync midiSync; // internal MIDI sync generator
    private GridSequencer sequencer;
    private ControlSurfaceHandler midiControl;
    private BCRBeatSlicerConfiguration configuration;

    public class WaveformFocusListener extends FocusAdapter {
        private int slice;
        WaveformFocusListener(int slice){
            this.slice = slice;
        }
        public void focusGained(FocusEvent e){
            if(!e.isTemporary())
                waveform.setMark(slicer.getSlice(slice));
        }
    }

    public class ToolTipFocusAdapter extends FocusAdapter {
        private int i;
        public ToolTipFocusAdapter(int i){
            this.i = i;
        }
        public void focusGained(FocusEvent e){
            status(controls[i].getToolTip());
        }
    }

    public class AboutFrame extends JFrame {
        public AboutFrame(){
            super("about");
            Icon icon = ResourceLocator.getIcon("bcr-beats/about.png");
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
                URL url = ResourceLocator.getResourceURL("bcr-beats/help.html");
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
        private int mode = -1; // uninitialised mode
        private static final int MODE_A = 0;
        private static final int MODE_B = 1;
        private static final int MODE_C = 2;
        private static final int MODE_D = 3;

        public void setMode(int mode){
            if(mode > MODE_B) // todo: enable mode b
                return;
            if(this.mode == mode)
                return;
            this.mode = mode;
            try{
                // update reassignable encoders
                int maxcontrols = slicer.getSlice(0).getNumberOfControls();
                switch(mode){
                case MODE_A :
                    for(int i=0; i<width; ++i){
                        cc_controls[i+81].setValue(slicer.getSlice(i).getLength());
                        cc_controls[i+81].setDescription("length");
                        if(maxcontrols > 0){
                            cc_controls[i+89].setValue(slicer.getSlice(i).getControlValue(0));
                            cc_controls[i+89].setDescription(slicer.getSlice(i).getControlName(0));
                        }else{
                            cc_controls[i+89].setValue(0);
                            cc_controls[i+89].setDescription("not in use");
                        }
                        if(maxcontrols > 1){
                            cc_controls[i+97].setValue(slicer.getSlice(i).getControlValue(1));
                            cc_controls[i+97].setDescription(slicer.getSlice(i).getControlName(1));
                        }else{
                            cc_controls[i+97].setValue(0);
                            cc_controls[i+97].setDescription("not in use");
                        }
                    }
                    status("mode A");
                    break;
                case MODE_B :
                    for(int i=0; i<width; ++i){
                        if(maxcontrols > 2){
                            cc_controls[i+81].setValue(slicer.getSlice(i).getControlValue(2));
                            cc_controls[i+81].setDescription(slicer.getSlice(i).getControlName(2));
                        }else{
                            cc_controls[i+81].setValue(0);
                            cc_controls[i+81].setDescription("not in use");
                        }
                        if(maxcontrols > 3){
                            cc_controls[i+89].setValue(slicer.getSlice(i).getControlValue(3));
                            cc_controls[i+89].setDescription(slicer.getSlice(i).getControlName(3));
                        }else{
                            cc_controls[i+89].setValue(0);
                            cc_controls[i+89].setDescription("not in use");
                        }
                        if(maxcontrols > 4){
                            cc_controls[i+97].setValue(slicer.getSlice(i).getControlValue(4));
                            cc_controls[i+97].setDescription(slicer.getSlice(i).getControlName(4));
                        }else{
                            cc_controls[i+97].setValue(0);
                            cc_controls[i+97].setDescription("not in use");
                        }
                    }
                    status("mode B");
                    break;
                }
                }catch(Exception exc){exc.printStackTrace();}
        }

        public int getMode(){
            return mode;
        }

        private int gridnote;
        private boolean playmode = true;

        public void action(int command, int channel, int data1, int data2){
//             status("action: "+command+" channel "+channel+
//                    " data1 "+data1+" data2 "+data2);
            if(data1 >= 1 && data1 <= 8){
                // push encoder turned
                BeatSlicer.Slice slice = slicer.getSlice(data1 - 1);
                slice.setStart(data2);
                waveform.setMark(slice);
                status("start offset "+slice.getByteOffset()+" bytes");
            }else if(data1 >= 33 && data1 <= 40){
                // push encoder pressed
                if(data2 > 63)
                    slicer.getSlice(data1 - 33).start();
                else
                    slicer.getSlice(data1 - 33).stop();
            }else if(data1 >= 65 && data1 <= 72){
                // button row 1 pressed
                if(data2 > 63 && playmode){
                    slicer.getSlice(data1 - 65).play();
                    status("play slice "+(data1 - 64));
                }else{
                    status("select slice "+(data1 - 64));
                }
                gridnote = data1 - 5; // notes start with 60
                for(int i=0; i<width; ++i)
                    try{
                        cc_controls[i+73].setValue(sequencer.isNoteOn(i, gridnote) ? 127 : 0);
                    }catch(Exception exc){exc.printStackTrace();}
            }else if(data1 >= 73 && data1 <= 80){
                // button row 2 pressed
                sequencer.setNote(data1 - 73, gridnote, data2 > 63);
                status("set slice "+(gridnote - 59)+" row "+(data1 - 72)+
                       (data2 > 63 ? " on":" off"));
            }else if(data1 >= 105 && data1 <= 108){
                // mode buttons - top two of four buttons in bottom right corner
                setMode(data1 - 105);
                // turn off the other mode buttons and make sure this one is on
                for(int i=105; i<=108; ++i)
                    try{
                        cc_controls[i].setValue(mode == i - 105 ? 127 : 0);
                    }catch(Exception exc){exc.printStackTrace();}
            }else if(data1 == 115){
                // store button
                if(data2 < 64)
                    stop();
                else
                    start();
            }else if(data1 == 116){
                // learn button
                if(data2 < 64){
                    status("external MIDI sync off");
//                     midiInput.setMidiSync(false);
                    slider.setEnabled(true);
//                     sequencer.setPeriod(60000 / slider.getValue());
                }else{
                    status("external MIDI sync on");
//                     midiInput.setMidiSync(true);
                    slider.setEnabled(false);
                }
            }else if(data1 == 117){
                // learn button
                playmode = data2 < 64;
            }else if(data1 >= 81 && data1 <= 104){
                int row = (data1 - 81) / 8;
                BeatSlicer.Slice slice = slicer.getSlice((data1 - 81) % 8);
                switch(mode){
                case MODE_A :
                    switch(row){
                    case 0 :
                        // top row simple encoder (below buttons)
                        slice.setLength(data2);
                        waveform.setMark(slice);
                        status("length "+slice.getByteLength()+" bytes");
                        break;
                    case 1 :
                        // second row simple encoder
                        if(slice.getNumberOfControls() > 0){
                            slice.setControlValue(0, data2);
                            status(slice.getControlName(0)+" "+slice.getControlValueString(0));
                        }
                        break;
                    case 2 :
                        // third row simple encoder
                        if(slice.getNumberOfControls() > 1){
                            slice.setControlValue(1, data2);
                            status(slice.getControlName(1)+" "+slice.getControlValueString(1));
                        }
                        break;
                    }
                    break;
                case MODE_B :
                    switch(row){
                    case 0 :
                        // top row simple encoder (below buttons)
                        if(slice.getNumberOfControls() > 2){
                            slice.setControlValue(2, data2);
                            status(slice.getControlName(2)+" "+slice.getControlValueString(2));
                        }
                        break;
                    case 1 :
                        // second row simple encoder
                        if(slice.getNumberOfControls() > 3){
                            slice.setControlValue(3, data2);
                            status(slice.getControlName(3)+" "+slice.getControlValueString(3));
                        }
                        break;
                    case 2 :
                        // third row simple encoder
                        if(slice.getNumberOfControls() > 4){
                            slice.setControlValue(4, data2);
                            status(slice.getControlName(4)+" "+slice.getControlValueString(4));
                        }
                        break;
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
//             }else{
//                 System.out.println("midi message "+message);
//                 return;
            }
        }

        public void send(ShortMessage msg, long time){
//             System.out.println("short message "+msg.getStatus()+" "+msg.getData1()+" "+msg.getData2());
            switch(msg.getStatus()){
            case ShortMessage.START: {
                start();
                break;
            }
            case ShortMessage.STOP: {
                stop();
                break;
            }
            case ShortMessage.CONTROL_CHANGE: {
                assert msg.getData1() > cc_controls.length;
                assert cc_controls[msg.getData1()] != null;
                cc_controls[msg.getData1()].send(msg, -1);
                break;
            }
//             case ShortMessage.NOTE_ON:
//             case ShortMessage.NOTE_OFF:
//             case ShortMessage.PITCH_BEND:
//                 break;
//             default:
//                 status("midi control <"+msg+"><"+msg.getStatus()+">");
            }
        }

        public void close(){
            if(this.transmitter != null)
                this.transmitter.close();
        }
    }

    public BCRBeatSlicer()
        throws Exception{
        super(new BorderLayout());
        // the channel that all controls are tuned to listen and talk on
        int channel = 0; // 0 is midi channel 1
        int bpm = 120; // default beats per minute

        slicer = new BeatSlicer(width);
        eventHandler = new EventHandler();
        midiControl = new ControlSurfaceHandler();
        sequencer = new GridSequencer(width);
        // initialise grid sequencer pattern
        for(int i=0; i<width; ++i)
            sequencer.setNoteOn(i, 60+i);
        sequencer.setReceiver(slicer);
        midiSync = new MidiSync(bpm);
        midiSync.setReceiver(sequencer);

        // statusbar
        statusbar = new JLabel();
        statusbar.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(statusbar, BorderLayout.SOUTH);

        JPanel mainarea = new JPanel();
        mainarea.setLayout(new BoxLayout(mainarea, BoxLayout.Y_AXIS));

        // add waveform graph
        waveform = new WaveformPanel(500, 100);
        mainarea.add(waveform);

//         // add grid sequencer display
//         mainarea.add(new GridSequencerPanel(sequencer));

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.X_AXIS));
//         JPanel mainarea = new JPanel(new GridLayout(0, width));
        // create rotary encoders and buttons
        List list = new ArrayList();
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i,
                                  slicer.getSlice(i).getStart(),
                                  "slice "+(1+i)+" start position");
            list.add(control);
            rows.add(control.getComponent());
            control.getComponent().addFocusListener(new WaveformFocusListener(i));

            // add 'invisible' button to back up the push encoder
            // todo: replace with PushEncoder class that can handle double-click as press
            TriggerButton trigger = new TriggerButton(1+i, ShortMessage.CONTROL_CHANGE, channel, 33+i, 0, "not in use");
            list.add(trigger);
        }
        mainarea.add(rows);
        // two rows of simple buttons
        rows = new JPanel(new GridLayout(2, width));
        for(int i=0; i<width; ++i){
//             ToggleButton button = 
//                 new ToggleButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
//                                  "play slice "+(i+1));
            TriggerButton button = 
                new TriggerButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
                                 "play slice "+(i+1));
            list.add(button);
            rows.add(button.getComponent());
            button.getComponent().addFocusListener(new WaveformFocusListener(i));
        }
        for(int i=0; i<width; ++i){
            ToggleButton button = 
                new ToggleButton(41+i, ShortMessage.CONTROL_CHANGE, channel, 73+i, 0,
                                 "loop slice "+(i+1));
//             TriggerButton trigger = 
//                 new TriggerButton(41+i, ShortMessage.CONTROL_CHANGE, channel, 73+i, 0,
//                                   "play "+NoteParser.getStringNote(i+61)+" arpeggio");
            list.add(button);
            rows.add(button.getComponent());
        }
        mainarea.add(rows);
        rows = new JPanel(new GridLayout(3, width));

        // first row of simple encoders (below buttons)
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(33+i, ShortMessage.CONTROL_CHANGE, channel, 81+i, 
                                  slicer.getSlice(i).getLength(),
                                  "slice "+(1+i)+" length/sound control");
            list.add(control);
            rows.add(control.getComponent());
            control.getComponent().addFocusListener(new WaveformFocusListener(i));
        }

        // second row of simple encoders
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(41+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, 
                                  0, "sound control");
            list.add(control);
            rows.add(control.getComponent());
            control.getComponent().addFocusListener(new WaveformFocusListener(i));
        }

        // third row of simple encoders
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(49+i, ShortMessage.CONTROL_CHANGE, channel, 97+i, 
                                  0, "sound control");
            list.add(control);
            rows.add(control.getComponent());
            control.getComponent().addFocusListener(new WaveformFocusListener(i));
        }
        mainarea.add(rows);

        this.add(mainarea, BorderLayout.CENTER);

        // buttonarea comprises the content in the right hand column of the screen
        Box buttonarea = new Box(BoxLayout.Y_AXIS);
        JLabel label = new JLabel(ResourceLocator.getIcon("bcr-beats/icon.png"));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        buttonarea.add(label);
        buttonarea.add(buttonarea.createVerticalStrut(100));

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
        String[] tooltips = new String[]{"start/stop", "external MIDI sync", "play/edit mode", "not in use"};
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
                                          (i == 0 ? 127 : 0), tooltips[i]);
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

        // BPM slider
        slider = new JSlider(JSlider.HORIZONTAL, 20, 240, bpm);
        slider.setMajorTickSpacing(40);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
//                     if(!source.getValueIsAdjusting()){
                        int bpm = (int)source.getValue();
                        midiSync.setBPM(bpm);
//                     }
                }
            });
        slider.setToolTipText("beats per minute");
        this.add(slider, BorderLayout.NORTH);

        // we attach a midi keyboard listener to all components
        MidiKeyboardListener listener = new MidiKeyboardListener();
        listener.setReceiver(slicer);
        this.addKeyListener(listener);
        slider.addKeyListener(listener);
        for(int i=0; i<controls.length; ++i){
            controls[i].setCallback(eventHandler);
            controls[i].getComponent().addKeyListener(listener);
            controls[i].getComponent().addFocusListener(new ToolTipFocusAdapter(i));
        }
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

    public void updateMidiControl(){
        try{
            for(int i=0; i<controls.length; ++i)
                controls[i].updateMidiControl();
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    public void configure()
        throws MidiUnavailableException {
        configuration = new BCRBeatSlicerConfiguration();
        configuration.setUpdateAction(new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    try{
                        configure(configuration);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });
        configuration.init(); // runs configuration initialisation
        configure(configuration);
    }

    public void configure(BCRBeatSlicerConfiguration configuration)
        throws MidiUnavailableException {
        // update devices from configuration settings
        MidiDevice device = configuration.getMidiInput();
        if(device != null){
            device.open();
            device.getTransmitter().setReceiver(slicer);
        }

        device = configuration.getMidiControlInput();
        if(device != null){
            device.open();
            midiControl.setTransmitter(device.getTransmitter());
        }

//         device = configuration.getMidiOutput();
//         if(device != null){
//             device.open();
//             midiOutput.setReceiver(device.getReceiver());
//             midiOutput.setChannel(configuration.getChannel());
//             status("MIDI output: "+device.getDeviceInfo().getName());
//         }

        device = configuration.getMidiControlOutput();
        if(device != null){
            device.open();
            Receiver receiver = device.getReceiver();
            status("control output: "+receiver);
            try{
                if(configuration.doSysex())
                    sendSysexMessages(receiver);
                for(int i=0; i<controls.length; ++i){
                    controls[i].setReceiver(receiver);
                    if(!configuration.doSysex()){
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
        BCRSysexMessage.createMessage(list, "  .name 'bcr beats               '");
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
        JMenu menu;
        AbstractAction action;

        menu = new JMenu("bcr beats");
        action  = new AbstractAction("about"){
                public void actionPerformed(ActionEvent event) {
                    JFrame frame = new AboutFrame();
                    frame.setVisible(true);
                }
            };
        menu.add(action);
        action = new AbstractAction("setup"){
                public void actionPerformed(ActionEvent event) {
                    try{
                        configuration.open();
                    }catch(Exception exc){exc.printStackTrace();}
                }
            };
        menu.add(new JMenuItem(action));
        action = new AbstractAction("load sample"){
                public void actionPerformed(ActionEvent event) {
                    chooseSampleFile();
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
        action = new AbstractAction("quit"){
                public void actionPerformed(ActionEvent event) {
                    System.exit(0);
                }
            };
        menu.add(new JMenuItem(action));
        menubar.add(menu);
        return menubar;
    }

    public void chooseSampleFile(){
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Sample");
        // Choose only files, not directories
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Start in current directory
//         fc.setCurrentDirectory(new File("."));

        // Set filter for Java source files.
//         FileFilter filter = new FileFilter(){
//                 public boolean accept(File f) {
//                     return f.getName ().toLowerCase().endsWith(".wav")
//                         || f.isDirectory();
//                 }
//             };
//         fc.setFileFilter(filter);

        // Now open chooser
        int result = fc.showOpenDialog(this);
        if(result == JFileChooser.CANCEL_OPTION) {
            return;
        }else if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            loadSample(file);
        }
    }

    public void loadSample(File file){
        try{
            slicer.loadSample(file);
        }catch(Exception exc){
            status("failed to load sample "+file.getName()+": "+exc.getMessage());
            return;
        }
        waveform.setData(slicer.getSlice(0).getData(), slicer.getSlice(0).getAudioFormat());
        eventHandler.setMode(eventHandler.MODE_A);
        updateMidiControl();
        status("loaded sample from file "+file.getName());
    }

    public void loadSample(URL url){
        try{
            slicer.loadSample(url);
        }catch(Exception exc){
            status("failed to load sample "+url+": "+exc.getMessage());
            return;
        }
        waveform.setData(slicer.getSlice(0).getData(), slicer.getSlice(0).getAudioFormat());
        eventHandler.setMode(eventHandler.MODE_A);
        updateMidiControl();
        status("loaded sample from URL "+url);
    }

    public void stop(){
        midiSync.stop();
        sequencer.stop();
        status("stop");
    }

    public void start(){
        sequencer.start();
        midiSync.start();
        status("start");
    }

    public void destroy(){
//         midiInput.close();
//         midiOutput.close();
        slicer.close();
        sequencer.close();
        midiControl.close();
        midiSync.close();
    }

    public static void main(String[] args)
        throws Exception {
        BCRBeatSlicer beats = new BCRBeatSlicer();
        if(args.length > 0)
            beats.loadSample(new File(args[0]));

        beats.configure();
        // create frame
        JFrame frame = new JFrame("bcr beats");
        frame.setJMenuBar(beats.getMenuBar());
        // configure frame
//         frame.pack();

        frame.setSize(625, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(beats);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}