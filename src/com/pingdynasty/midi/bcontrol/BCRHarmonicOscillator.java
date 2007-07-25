package com.pingdynasty.midi.bcontrol;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.*;


// todo:
// - write control update method that sets control values from osc and output settings.
// - add volume control.
// - check for sample rate, volume, master gain controls before adding them in.
// - make display update frequency configurable
// - make output buffer size configurable
// - give visual clipping indication - flash a controller button
// - change buttons and knobs to use spring layout 
// - move scale factor to HarmonicOscillator so that the value carries over with presets
// - write documentation.
// - write applet version
// - write vst plugin version
public class BCRHarmonicOscillator extends JPanel {

    private HarmonicOscillator[] presets;
    private HarmonicOscillator osc;
    private OscillatorPanel view;
    private AudioLineOutput output;
//     private AudioFileOutput fileoutput;
    private Receiver midiInput;
    private MidiControl[] controls;
    private MidiControl[] cc_controls; // quick index for CC controls
    private JLabel statusbar;
    private EventHandler eventHandler;
    private ControlSurfaceHandler midiControl;
    private BCRHarmonicOscillatorConfiguration configuration;
    private Runner runner;
//     private AudioControl[] audiocontrols;

//     class AudioControl {
        
//     }

//     class Animator implements Runnable {
//         private static final long FRAME_DELAY = 40; // 25fps
//         private boolean running;

//         public void start(){
//             running = true;
//             Thread thread = new Thread(this);
//             thread.setDaemon(true);
//             thread.start();
//         }

//         public void stop(){
//             running = false;
//         }

//         public void run(){
//             while(running){
//                 view.setAndScaleData(osc.getData());
//                 try{
//                     Thread.sleep(FRAME_DELAY);
//                 }catch(InterruptedException e){}
//             }
//         }
//     }

    class Runner implements Runnable {
        private boolean running = false;
//         private Animator animator = new Animator();

        public void start(){
            running = true;
            Thread thread = new Thread(this);
            thread.start();
//             animator.start();
        }

        public void stop(){
            running = false;
//             animator.stop();
        }

        public void run(){
            double[] values;
            while(running){
                osc.increment();
                values = osc.calculate();
//                 for(int i=0; i<4; ++i){
                output.write(values);
                if(output.clipping())
                    status("clipping!");
                    //                 if(fileoutput != null)
                    //                     fileoutput.write(values);
//                     values = osc.calculate();
//                 }
                view.setAndScaleData(values);
            }
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
            Icon icon = ResourceLocator.getIcon("bcr-harms/about.png");
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
                URL url = ResourceLocator.getResourceURL("bcr-harms/help.html");
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
        boolean synchronising;

        // update controls from oscillator values
        public void update(){
            this.updateAmplitudes();
            this.updateEnergy();
        }

        public void updateAmplitudes(){
            synchronising = true;
            for(int i=0; i<8; ++i){
                try{
                    cc_controls[i+1].setValue(osc.getControl(i));
                }catch(Exception exc){exc.printStackTrace();}
//                 System.out.println(i+":\t"+osc.getControl(i));
            }
            synchronising = false;
        }

        public void updateEnergy(){
            synchronising = true;
            try{
                cc_controls[97].setValue(osc.getEnergy());
            }catch(Exception exc){exc.printStackTrace();}
//             System.out.println("e:\t"+osc.getEnergy());
            synchronising = false;
        }

        public void action(int command, int channel, int data1, int data2){
            if(synchronising)
                return;
//             status("action: "+command+" channel "+channel+
//                    " data1 "+data1+" data2 "+data2);
            if(data1 >= 1 && data1 <= 8){
                // push encoder turned
                osc.setControl(data1 - 1, data2);
                updateEnergy();
                status("amplitude "+data1+" "+data2);
//             }else if(data1 >= 33 && data1 <= 40){
                // push encoder pressed
            }else if(data1 >= 65 && data1 <= 72){
                // button row 1 pressed
                if(data2 > 63){
                    osc.setSingleState(data1 - 65);
                    update();
                    status("single state "+(data1 - 64));
                }
//             }else if(data1 >= 73 && data1 <= 80){
                // button row 2 pressed
            }else if(data1 >= 111 && data1 <= 114){
                // encoder group buttons
                int index = data1 - 111;
                if(osc != presets[index]){
                    assert index < presets.length;
                    assert index >= 0;
                    osc = presets[index];
                    update();
                    setControlValues();
                    status("preset "+(index+1));
                }
                // turn other buttons off
                for(int i=111; i<=114; ++i)
                    try{
                        cc_controls[i].setValue(data1 == i ? 127 : 0);
                    }catch(Exception exc){exc.printStackTrace();}
//             }else if(data1 >= 105 && data1 <= 108){
                // mode buttons - four buttons in bottom right corner
            }else if(data1 == 115){
                // store button
                if(data2 < 64){
                    runner.stop();
                    status("stop");
                }else{
                    runner.start();
                    status("start");
                }
            }else if(data1 == 117){
                // edit button
                if(data2 > 63){
                    try{
                        stop();
                        configuration.open();
                    }catch(Exception exc){exc.printStackTrace();}
                }
//             }else if(data1 == 116){
//                 if(data2 > 63){
//                     try{
//                         if(fileoutput == null)
//                             fileoutput = new AudioFileOutput(512, AudioOutput.PCM32SL);
//                         else fileoutput.reset();
//                     }catch(Exception exc){exc.printStackTrace();}
//                 }
//             }else if(data1 == 118){
//                 if(data2 > 63){
//                     try{
//                         if(fileoutput != null)
//                             fileoutput.write(22050f, new File("harms.wav"));
//                     }catch(Exception exc){exc.printStackTrace();}
//                 }

//                 if(data1 >= 81 && data1 <= 88){
//                     // top row simple encoder (below buttons)
//                 }else if(data1 >= 89 && data1 <= 96){
            }else if(data1 >= 89 && data1 <= 96){
                    // second row simple encoder
                int index = data1 - 89;
                if(index < output.getNumberOfControls()){
                    output.setControlValue(index, data2);
                    status(output.getControlName(index)+" "+
                           output.getControlValueString(index));
                }
            }else{
                switch(data1){
                    // third row simple encoder
                case 97:{
                    osc.setGlauberState(data2);
                    updateAmplitudes();
                    status("Glauber state "+osc.getEnergy());
                    break;
                }
                case 98:{
                    osc.setWavelength(data2);
                    status("wavelength "+osc.getWavelength());
                    break;
                }
                case 99:{
                    osc.setDistance(data2);
                    status("distance "+osc.getDistance());
                    break;
                }
                case 100:{
                    osc.setTimeStep(data2);
                    status("time step "+data2);
                    break;
                }
                case 101:{
                    output.setScaleFactor(data2);
                    view.setScaleFactor(data2);
                    status("scale factor "+data2);
                    break;
                }
                }
            }
        }
    }

//     public class MidiInputHandler extends ShortMessageReceiver {
//         public void send(ShortMessage msg, long time){
//             switch(msg.getStatus()){
//             case ShortMessage.NOTE_ON: {
//                 int index = msg.getData1() - 60;
//                 if(index >= 0 && index < osc.getControls())
//                     osc.setSingleState(index);
//             }
//             }
//         }
//     }

    public class ControlSurfaceHandler extends ShortMessageReceiver {

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
            }
        }
    }

    public BCRHarmonicOscillator()
        throws Exception{
        super(new BorderLayout());
        eventHandler = new EventHandler();
        midiControl = new ControlSurfaceHandler();
        runner = new Runner();
        presets = new HarmonicOscillator[4];
        configuration = new BCRHarmonicOscillatorConfiguration();
        configuration.setUpdateAction(new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    try{
                        configure(configuration);
                        setControlValues();
                        cc_controls[117].setValue(0);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });
        configuration.setCancelAction(new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    try{
                        cc_controls[117].setValue(0);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });

        // statusbar
        statusbar = new JLabel();
        statusbar.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(statusbar, BorderLayout.SOUTH);

//         configuration.init(); // runs configuration initialisation
        configure(configuration); // initialise resources from configuration settings

        JPanel mainarea = new JPanel();
        mainarea.setLayout(new BoxLayout(mainarea, BoxLayout.Y_AXIS));

        // add oscillator graph
        view = new OscillatorPanel(512); // todo fix sizes
        view.setMinimumSize(new Dimension(512, 100));
        view.setPreferredSize(new Dimension(512, 200));
        mainarea.add(view);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.X_AXIS));

        // the channel that all controls are tuned to listen and talk on
        int channel = 0; // 0 is midi channel 1

        // create rotary encoders and buttons
        List list = new ArrayList();
        for(int i=0; i<8; ++i){
            MidiControl control = 
                new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i,
                                  0, "amplitude "+(1+i));
            list.add(control);
            rows.add(control.getComponent());

            // add 'invisible' button to back up the push encoder
            // todo: replace with PushEncoder class that can handle double-click as press
            TriggerButton trigger = new TriggerButton(1+i, ShortMessage.CONTROL_CHANGE, channel, 33+i, 0, "not in use");
            list.add(trigger);
        }
        mainarea.add(rows);
        // two rows of simple buttons
        rows = new JPanel(new GridLayout(2, 8));
        for(int i=0; i<8; ++i){
//             ToggleButton button = 
//                 new ToggleButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
//                                  "play slice "+(i+1));
            TriggerButton button = 
                new TriggerButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
                                 "set single state "+(i+1));
            list.add(button);
            rows.add(button.getComponent());
        }
        for(int i=0; i<8; ++i){
            TriggerButton button = 
                new TriggerButton(41+i, ShortMessage.CONTROL_CHANGE, channel, 73+i, 0,
                                 "not in use");
//             TriggerButton trigger = 
//                 new TriggerButton(41+i, ShortMessage.CONTROL_CHANGE, channel, 73+i, 0,
//                                   "play "+NoteParser.getStringNote(i+61)+" arpeggio");
            list.add(button);
            rows.add(button.getComponent());
        }
        mainarea.add(rows);
        rows = new JPanel(new GridLayout(3, 8));

        // first row of simple encoders (below buttons)
        for(int i=0; i<8; ++i){
            MidiControl control = 
                new RotaryEncoder(33+i, ShortMessage.CONTROL_CHANGE, channel, 81+i, 
                                  0, "not in use");
            list.add(control);
            rows.add(control.getComponent());
        }

        // second row of simple encoders
        int numberOfControls = output.getNumberOfControls();
        if(numberOfControls > 8)
            numberOfControls = 8;
        for(int i=0; i<numberOfControls; ++i){
            MidiControl control = new RotaryEncoder(41+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, output.getControlValue(i), output.getControlName(i));
            list.add(control);
            rows.add(control.getComponent());
        }
        for(int i=numberOfControls; i<8; ++i){
            MidiControl control = 
                new RotaryEncoder(41+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, 0, "not in use");
            list.add(control);
            rows.add(control.getComponent());
        }

        // third row of simple encoders
        MidiControl control;
        control = new RotaryEncoder(49, ShortMessage.CONTROL_CHANGE, channel, 97, 0, "Glauber state");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(50, ShortMessage.CONTROL_CHANGE, channel, 98, 0, "wavelength");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(51, ShortMessage.CONTROL_CHANGE, channel, 99, 0, "distance");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(52, ShortMessage.CONTROL_CHANGE, channel, 100, 0, "time step");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(53, ShortMessage.CONTROL_CHANGE, channel, 101, 0, "scale factor");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(54, ShortMessage.CONTROL_CHANGE, channel, 102, 0, "not in use");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(55, ShortMessage.CONTROL_CHANGE, channel, 103, 0, "not in use");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(56, ShortMessage.CONTROL_CHANGE, channel, 104, 0, "not in use");
        list.add(control);
        rows.add(control.getComponent());
        mainarea.add(rows);

        this.add(mainarea, BorderLayout.CENTER);

        // buttonarea comprises the content in the right hand column of the screen
        Box buttonarea = new Box(BoxLayout.Y_AXIS);
        JLabel label = new JLabel(ResourceLocator.getIcon("bcr-harms/icon.png"));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        buttonarea.add(label);
        buttonarea.add(buttonarea.createVerticalStrut(200));

        // encoder buttons
        ToggleButton[] toggles = new ToggleButton[4];
        for(int i=0; i<4; ++i){
            toggles[i] = new ToggleButton(57+i, ShortMessage.CONTROL_CHANGE, channel, 111+i, (i == 0 ? 127 : 0), "oscillator preset "+(i+1));
            list.add(toggles[i]);
        }
        addFourButtons(buttonarea, toggles);
        buttonarea.add(buttonarea.createVerticalStrut(20));

        // store/learn/edit/exit
        toggles = new ToggleButton[]{
            new ToggleButton(53, ShortMessage.CONTROL_CHANGE, channel, 115, 0, "start/stop"),
            new TriggerButton(54, ShortMessage.CONTROL_CHANGE, channel, 116, 0, "not in use"),
            new ToggleButton(55, ShortMessage.CONTROL_CHANGE, channel, 117, 0, "setup"),
            new TriggerButton(56, ShortMessage.CONTROL_CHANGE, channel, 118, 0, "not in use")
        };
        for(int i=0; i<4; ++i)
            list.add(toggles[i]);
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
        String[] tooltips = new String[]{"not in use", "not in use", "not in use", "not in use"};
        for(int i=0; i<4; ++i){
            toggles[i] = new TriggerButton(49+i, ShortMessage.CONTROL_CHANGE, channel, 105+i, 
                                           0, tooltips[i]);
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

        for(int i=0; i<controls.length; ++i){
            controls[i].setCallback(eventHandler);
            controls[i].getComponent().addFocusListener(new ToolTipFocusAdapter(i));
        }

        configuration.init(); // runs configuration initialisation
        configure(configuration); // initialise resources from configuration settings
        setControlValues();
        view.setAndScaleData(osc.calculate());
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

    public void updateMidiControl(){
        try{
            for(int i=0; i<controls.length; ++i)
                controls[i].updateMidiControl();
        }catch(InvalidMidiDataException exc){
            exc.printStackTrace();
        }
    }

    // initialise resources from configuration settings
    public void configure(BCRHarmonicOscillatorConfiguration configuration)
        throws Exception {
        int width = 8; // number of controls
        int sampleWidth = configuration.getSampleWidth();
        float outputFrequency = configuration.getOutputFrequency();
        int buffersize = configuration.getBufferSize();
        for(int i=0; i<presets.length; ++i){
            presets[i] = new HarmonicOscillator(sampleWidth, width);
            presets[i].setGlauberState(i*10 + 20); // initialises control values
        }
        osc = presets[0];
        output = new AudioLineOutput(sampleWidth, AudioOutput.PCM16SL);
        output.openLine(outputFrequency, buffersize);
        midiInput = new HarmonicOscillatorSynth(osc);

        // update MIDI devices from configuration settings
        MidiDevice device = configuration.getMidiInput();
        if(device != null){
            device.open();
            device.getTransmitter().setReceiver(midiInput);
//             midiInput.setTransmitter(device.getTransmitter());
        }

        device = configuration.getMidiControlInput();
        if(device != null){
            device.open();
            midiControl.setTransmitter(device.getTransmitter());
        }

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

    public void setControlValues(){
        eventHandler.update();
        try{
            // second row of simple knobs
            for(int i=0; i<output.getNumberOfControls(); ++i)
                cc_controls[89+i].setValue(output.getControlValue(i));
            // third row of simple knobs
            cc_controls[97].setValue(osc.getEnergy());
            cc_controls[98].setValue(osc.getWavelength());
            cc_controls[99].setValue(osc.getDistance());
            cc_controls[100].setValue(osc.getTimeStep());
            cc_controls[101].setValue(output.getScaleFactor());
        }catch(InvalidMidiDataException exc){exc.printStackTrace();}
    }

    public void status(String msg){
        System.out.println(msg);
        statusbar.setText(msg);
    }

    public void start(){
        try{
            cc_controls[115].setValue(127);
        }catch(Exception exc){exc.printStackTrace();}
        runner.start();
        status("start");
    }

    public void stop(){
        runner.stop();
        try{
            cc_controls[115].setValue(0);
        }catch(Exception exc){exc.printStackTrace();}
        status("stop");
    }

    public void sendSysexMessages(Receiver receiver)
        throws InvalidMidiDataException {
        List list = new ArrayList();
        BCRSysexMessage.createMessage(list, "$rev R1");
        BCRSysexMessage.createMessage(list, "$preset");
        BCRSysexMessage.createMessage(list, "  .name 'bcr harms               '");
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

        menu = new JMenu("bcr harms");
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
                        stop();
                        configuration.open();
                        cc_controls[117].setValue(127);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            };
        menu.add(new JMenuItem(action));
        action = new AbstractAction("start"){
                public void actionPerformed(ActionEvent event) {
                    try{
                        start();
                    }catch(Exception exc){exc.printStackTrace();}
                }
            };
        menu.add(new JMenuItem(action));
        action = new AbstractAction("stop"){
                public void actionPerformed(ActionEvent event) {
                    try{
                        stop();
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
        action = new AbstractAction("quit"){
                public void actionPerformed(ActionEvent event) {
                    System.exit(0);
                }
            };
        menu.add(new JMenuItem(action));
        menubar.add(menu);
        return menubar;
    }

    public void destroy(){
//         midiInput.close();
//         midiOutput.close();
        midiControl.close();
    }

    public static void main(String[] args)
        throws Exception {
        BCRHarmonicOscillator osc = new BCRHarmonicOscillator();

        // create frame
        JFrame frame = new JFrame("bcr harms");
        frame.setJMenuBar(osc.getMenuBar());
        frame.setSize(625, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(osc);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}