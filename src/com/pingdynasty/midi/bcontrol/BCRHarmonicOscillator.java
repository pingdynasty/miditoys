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

public class BCRHarmonicOscillator extends JPanel {

    private HarmonicOscillator osc;
    private OscillatorPanel view;
    private AudioOutput output;

    private MidiControl[] controls;
    private MidiControl[] cc_controls; // quick index for CC controls
    private JLabel statusbar;
    private EventHandler eventHandler;
    private ControlSurfaceHandler midiControl;
    private BCRBeatSlicerConfiguration configuration;
    private Runner runner;

    class Runner implements Runnable {
        boolean running;

        public void start(){
            running = true;
            Thread thread = new Thread(this);
            thread.start();
        }

        public void stop(){
            running = false;
        }

        public void run(){
            while(running){
                double[] values = osc.calculate();
//                 view.setData(values);
                view.setAndScaleData(values);
                output.write(values);
                if(output.clipping())
                    status("clipping!");
                osc.increment();
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

//     public class AboutFrame extends JFrame {
//         public AboutFrame(){
//             super("about");
//             Icon icon = ResourceLocator.getIcon("bcr-beats/about.png");
//             getContentPane().add(new JLabel(icon));
//             addMouseListener(new MouseAdapter(){
//                     public void mouseClicked(MouseEvent e){
//                         setVisible(false);
//                         dispose();
//                     }
//                 });
//             setResizable(false);
//             setUndecorated(true);
//             getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
//             pack();
//             Dimension dim = getToolkit().getScreenSize();
//             setLocation(dim.width/2 - getWidth()/2, dim.height/2 - getHeight()/2);
//         }
//     }

//     public class HelpFrame extends JFrame {
//         public HelpFrame(){
//             super("help");
//             try{
//                 URL url = ResourceLocator.getResourceURL("bcr-beats/help.html");
//                 JEditorPane text = new JEditorPane(url);
//                 text.setEditable(false);
//                 text.setMargin(new Insets(8, 8, 16, 16));
//                 text.addHyperlinkListener(new HyperlinkListener() {
//                         public void hyperlinkUpdate(HyperlinkEvent e){
//                             if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
//                                 JEditorPane pane = (JEditorPane)e.getSource();
//                                 try{
//                                     pane.setPage(e.getURL());
//                                 }catch(Exception exc){
//                                     status(exc.toString());
//                                     exc.printStackTrace();
//                                 }
//                             }
//                         }
//                     });
//                 getContentPane().add(new JScrollPane(text));
//             }catch(Exception exc){exc.printStackTrace();}
//             setSize(500, 500);
//             Dimension dim = getToolkit().getScreenSize();
//             setLocation(dim.width/2 - getWidth()/2, dim.height/2 - getHeight()/2);
//         }
//     }

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
                cc_controls[100].setValue(osc.getEnergy());
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
                status("amplitude "+data1+": "+data2);
            }else if(data1 >= 33 && data1 <= 40){
                // push encoder pressed
            }else if(data1 >= 65 && data1 <= 72){
                // button row 1 pressed
                if(data2 > 63){
                    osc.setSingleState(data1 - 65);
                    update();
                    status("single state "+(data1 - 64));
                }
            }else if(data1 >= 73 && data1 <= 80){
                // button row 2 pressed
            }else if(data1 >= 105 && data1 <= 108){
                // mode buttons - top two of four buttons in bottom right corner
            }else if(data1 == 115){
                // store button
                if(data2 < 64){
                    runner.stop();
                    status("stop");
                }else{
                    runner.start();
                    status("start");
                }
            }else if(data1 == 116){
                // learn button
            }else{
//                 if(data1 >= 81 && data1 <= 88){
//                     // top row simple encoder (below buttons)
//                 }else if(data1 >= 89 && data1 <= 96){
//                     // second row simple encoder
//                 }else if(data1 >= 97 && data1 <= 104){
                switch(data1){
                    // third row simple encoder
                case 97:{
                    double dt = 0.001d * data2;
                    osc.setTimeStep(dt);
                    status("time step "+dt);
                    break;
                }
                case 98:{
                    output.setSampleRate(data2);
                    status("sample rate "+output.getSampleRate());
                    break;
                }
                case 99:{
                    output.setScaleFactor(data2);
                    view.setScaleFactor(data2);
                    status("scale factor "+data2);
                    break;
                }
                case 100:{
                    if(data2 > 100)
                        data2 = 100;
                    osc.setEnergy(data2);
                    updateAmplitudes();
                    status("glauber state "+data2);
                    break;
                }
                case 101:{
                    // HALFd = controls / 2.0 : max 8, min 0
                    double value = (data2 / 127.0d) * 8.0d;
                    osc.setHalfDepth(value);
                    status("half depth "+osc.getHalfDepth());
                    break;
                }
                case 102:{
                    // HalfSize = samples / controls
                    // max = samples / controls * 2;
                    // min = 1;
                    int value = data2 + 1;
                    osc.setHalfSize(value);
                    status("half size "+osc.getHalfSize());
                    break;
                }
                }
//                 }
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
                runner.start();
                status("start");
                break;
            }
            case ShortMessage.STOP: {
                runner.stop();
                status("stop");
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

        public void close(){
            if(this.transmitter != null)
                this.transmitter.close();
        }
    }

    public BCRHarmonicOscillator(int samples)
        throws Exception{
        super(new BorderLayout());
        // the channel that all controls are tuned to listen and talk on
        int channel = 0; // 0 is midi channel 1
        int width = 8; // number of controls
        osc = new HarmonicOscillator(samples, width);
        view = new OscillatorPanel(512); // todo fix sizes
        view.setAndScaleData(osc.calculate());
        view.setMinimumSize(new Dimension(512, 100));
        view.setPreferredSize(new Dimension(512, 200));
        output = new AudioOutput(samples);
        runner = new Runner();
        eventHandler = new EventHandler();
        midiControl = new ControlSurfaceHandler();

        // statusbar
        statusbar = new JLabel();
        statusbar.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(statusbar, BorderLayout.SOUTH);

        JPanel mainarea = new JPanel();
        mainarea.setLayout(new BoxLayout(mainarea, BoxLayout.Y_AXIS));

        // add oscillator graph
        mainarea.add(view);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.X_AXIS));

        // create rotary encoders and buttons
        List list = new ArrayList();
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(1+i, ShortMessage.CONTROL_CHANGE, channel, 1+i,
                                  osc.getControl(i), "amplitude "+(1+i));
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
//             ToggleButton button = 
//                 new ToggleButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
//                                  "play slice "+(i+1));
            TriggerButton button = 
                new TriggerButton(33+i, ShortMessage.CONTROL_CHANGE, channel, 65+i, 0,
                                 "set single state "+(i+1));
            list.add(button);
            rows.add(button.getComponent());
        }
        for(int i=0; i<width; ++i){
            ToggleButton button = 
                new ToggleButton(41+i, ShortMessage.CONTROL_CHANGE, channel, 73+i, 0,
                                 "not in use");
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
                                  0, "not in use");
            list.add(control);
            rows.add(control.getComponent());
        }

        // second row of simple encoders
        for(int i=0; i<width; ++i){
            MidiControl control = 
                new RotaryEncoder(41+i, ShortMessage.CONTROL_CHANGE, channel, 89+i, 
                                  0, "not in use");
            list.add(control);
            rows.add(control.getComponent());
        }

        // third row of simple encoders
        MidiControl control = new RotaryEncoder(49, ShortMessage.CONTROL_CHANGE, channel, 97, 63, "set time step");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(50, ShortMessage.CONTROL_CHANGE, channel, 98, 63, "set sample rate");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(51, ShortMessage.CONTROL_CHANGE, channel, 99, 63, "set scale factor");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(52, ShortMessage.CONTROL_CHANGE, channel, 100, 74, "set glauber state");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(53, ShortMessage.CONTROL_CHANGE, channel, 101, 63, "set half depth");
        list.add(control);
        rows.add(control.getComponent());
        control = new RotaryEncoder(54, ShortMessage.CONTROL_CHANGE, channel, 102, 63, "set half size");
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
        String[] tooltips = new String[]{"start/stop", "not in use", "not in use", "not in use"};
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
        tooltips = new String[]{"not in use", "not in use", "not in use", "not in use"};
        for(int i=0; i<4; ++i){
            toggles[i] = new ToggleButton(49+i, ShortMessage.CONTROL_CHANGE, channel, 105+i, 
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

    public void initialiseMidiDevices()
        throws MidiUnavailableException {
        configuration = new BCRBeatSlicerConfiguration();
        configuration.setUpdateAction(new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    try{
                        updateMidiDevices();
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });
        configuration.init(); // runs configuration initialisation
        updateMidiDevices();
    }

    public void updateMidiDevices()
        throws MidiUnavailableException {
        // update devices from configuration settings
        MidiDevice device;// = configuration.getMidiInput();
//         if(device != null){
//             device.open();
//             device.getTransmitter().setReceiver(slicer);
//         }

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

    public void status(String msg){
        System.out.println(msg);
        statusbar.setText(msg);
    }

    public void sendSysexMessages(Receiver receiver)
        throws InvalidMidiDataException {
        List list = new ArrayList();
        BCRSysexMessage.createMessage(list, "$rev R1");
        BCRSysexMessage.createMessage(list, "$preset");
        BCRSysexMessage.createMessage(list, "  .name 'bcr oscillator          '");
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

        menu = new JMenu("bcr oscillator");
//         action  = new AbstractAction("about"){
//                 public void actionPerformed(ActionEvent event) {
//                     JFrame frame = new AboutFrame();
//                     frame.setVisible(true);
//                 }
//             };
//         menu.add(action);
        action = new AbstractAction("setup"){
                public void actionPerformed(ActionEvent event) {
                    try{
                        configuration.open();
                    }catch(Exception exc){exc.printStackTrace();}
                }
            };
        menu.add(new JMenuItem(action));
//         action = new AbstractAction("help"){
//                 public void actionPerformed(ActionEvent event) {
//                     JFrame frame = new HelpFrame();
//                     frame.setVisible(true);
//                 }
//             };
//         menu.add(action);
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
        int samples = 512;
        BCRHarmonicOscillator osc = new BCRHarmonicOscillator(samples);
        osc.initialiseMidiDevices();

        // create frame
        JFrame frame = new JFrame("bcr oscillator");
        frame.setJMenuBar(osc.getMenuBar());
        frame.setSize(625, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(osc);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}