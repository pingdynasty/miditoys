package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

// todo: write out control surface settings when selecting device.
// classes to handle control surface device, sync device, output device, MIDI in device (for arpeggiator).
// arpeggiator class.
// 4 time programs (sets of steps) to match push encoders.
// 4 different modes for the bottom 3 encoder rows.
public class StepSequencerPanel extends JPanel {

    private StepSequencer sequencer;
    private JLabel statusbar;
    private StepsTableModel model;
    private Timer timer;
    private JSlider slider;
    private boolean midiSync = false;

    private ControlSurfaceHandler midiControl;
    private StepSequencerArpeggio midiInput;
    private ReceiverPlayer midiOutput;

    private final static String midiControlInputName = "Control input";
    private final static String midiControlOutputName = "Control output";
    private final static String midiInputName = "MIDI input";
    private final static String midiOutputName = "MIDI output";
    private DevicePanel devicepanel = 
        new DevicePanel(new String[]{midiInputName, midiControlInputName},  
                        new String[]{midiOutputName, midiControlOutputName});

    public class ControlSurfaceHandler implements Receiver {
        private Transmitter transmitter;
        private Receiver receiver;

        ControlSurfaceHandler(){}

        ControlSurfaceHandler(Transmitter transmitter, Receiver receiver){
            this.transmitter = transmitter;
            this.receiver = receiver;
        }

        public void setTransmitter(Transmitter transmitter){
            if(this.transmitter == transmitter)
                return;
            if(this.transmitter != null)
                this.transmitter.close();
            this.transmitter = transmitter;
            transmitter.setReceiver(this);
        }

        public void setReceiver(Receiver receiver){
            if(this.receiver == receiver)
                return;
            if(this.receiver != null)
                this.receiver.close();
            this.receiver = receiver;
            synchronize();
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
//         case ShortMessage.MIDI_TIME_CODE: {
// //             status("midi time code <"+msg+"><"+time+"><"+
// //                    msg.getCommand()+"><"+msg.getChannel()+"><"+
// //                    msg.getData1()+"><"+msg.getData2()+">");
//             timer.update();
//             break;
//         }
            case ShortMessage.TIMING_CLOCK: {
                if(midiSync)
                    timer.update();
                break;
            }
            case ShortMessage.START: {
                status("start");
                if(midiSync)
                    timer.start();
                sequencer.start();
                break;
            }
            case ShortMessage.STOP: {
                status("stop");
                sequencer.stop();
                break;
            }
            case ShortMessage.CONTROL_CHANGE: {
//             status("midi cc <"+msg+"><"+time+"><"+
//                    msg.getCommand()+"><"+msg.getChannel()+"><"+
//                    msg.getData1()+"><"+msg.getData2()+">");
                int cmd = msg.getData1();
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
            if(this.receiver != null)
                this.receiver.close();
        }

        public void controlChange(int code, int value)
            throws InvalidMidiDataException{
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, code, value);
            receiver.send(msg, -1);
        }

        public void synchronize(){
            if(receiver == null)
                return;
            for(int i=0; i<8; ++i){
                Step step = sequencer.getStep(i); 
                try{
                    controlChange(1+i, step.getNote());
                    controlChange(81+i, step.getVelocity());
                    controlChange(89+i, step.getDuration());
                    controlChange(97+i, step.getModulation());
                }catch(Exception exc){
                    exc.printStackTrace();
                }
            }
        }
    }

    public class Timer {
        boolean timing = false;
        long tick;
        public void start(){
            tick = System.currentTimeMillis();
            timing = true;
        }
        public void update(){
            if(timing){
                long now = System.currentTimeMillis();
                int period = (int)(24 * (now - tick));
                //     period = 60000 / ( 24 * bpm ) = 2500 / bpm
                //     bpm = 60000 / (24 * period) = 2500 / period
                //     delay = 1000 / (bpm / 60) = 60000 / bpm
                sequencer.setPeriod(period);
//                 status("period: "+period);
                tick = now;
            }else{
                tick = System.currentTimeMillis();
                timing = true;
            }
        }
    }

    public StepSequencerPanel(int width)
        throws MidiUnavailableException{
        timer = new Timer();
        midiControl = new ControlSurfaceHandler();
        JPanel content = new JPanel(new BorderLayout());

        // statusbar
        statusbar = new JLabel();
        content.add(statusbar, BorderLayout.SOUTH);

        MidiDevice device = DeviceLocator.getDevice(Synthesizer.class);
        devicepanel.setDevice(midiOutputName, device);
        device.open();
        status("MIDI OUT device: "+device.getDeviceInfo().getName());
        midiOutput = new SchedulingPlayer(device.getReceiver());

        // create StepSequencer
        this.sequencer = new StepSequencer(midiOutput, width);
        // create Arpeggio player
        midiInput = new StepSequencerArpeggio(sequencer);

        model = new StepsTableModel(sequencer);
        JTable steps = new JTable(model);
//         steps.getColumnModel().getColumn(0).setCellRenderer(model);
//         steps.setDefaultRenderer(Object.class, model);
        content.add(steps, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        // add BPM sync button
        JButton button = new JButton("sync");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("MIDI sync");
                    if(midiSync)
                        sequencer.setPeriod(60000 / (int)slider.getValue());
//                         slider.setValue(60000 / sequencer.getPeriod());
                    midiSync = !midiSync;
                    slider.setEnabled(!midiSync);
                }
            });
        buttons.add(button);
        // create play button
        button = new JButton("play");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("play");
                    sequencer.play();
                }
            });
        buttons.add(button);
        button = new JButton("start");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("start");
                    sequencer.start();
                }
            });
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
                        JFrame frame = devicepanel.getFrame();
                        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        frame.pack();
                        frame.setVisible(true);
                        frame.addWindowListener(new WindowAdapter() {
                                public void windowDeactivated(WindowEvent e){
                                    try{
                                        updateDevices();
                                    }catch(Exception exc){exc.printStackTrace();}
                                }
                                public void windowClosing(WindowEvent e){
                                    try{
                                        updateDevices();
                                    }catch(Exception exc){exc.printStackTrace();}
                                }
                            });
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
            });
        buttons.add(button);
        content.add(buttons, BorderLayout.EAST);

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
        content.add(slider, BorderLayout.NORTH);

        this.add(content);
    }

    public void close(){}

    public void status(String msg){
        System.out.println(msg);
        statusbar.setText(msg);
//         statusbar.repaint();
    }

    protected void initialiseDevices(){

    }

    protected void updateDevices()
        throws MidiUnavailableException {
        status("updating midi devices");
        // update devices from devicepanel settings
        MidiDevice device = devicepanel.getDevice(midiInputName);
        if(device != null){
            device.open();
            midiInput.setTransmitter(device.getTransmitter());
        }

        device = devicepanel.getDevice(midiControlInputName);
        if(device != null){
            device.open();
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
            midiControl.setReceiver(device.getReceiver());
        }
    }

    public static void main(String[] args)
        throws Exception {
        StepSequencerPanel panel = new StepSequencerPanel(8);

        // create frame
        JFrame frame = new JFrame("step sequencer");
        // configure frame
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }

    class StepsTableModel extends AbstractTableModel {
        int width;
        int height = 5;
        String[] labels = new String[]{"note", "velocity", "duration", "modulation", "bend"};
        StepSequencer sequencer;

        StepsTableModel(StepSequencer sequencer){
            this.sequencer = sequencer;
            width = sequencer.getLength() + 1;
        }

        public int getColumnCount() { return width; }

        public int getRowCount() { return height;}

        public void setValueAt(Object value, int row, int col){
            if(col == 0)
                return;
            try{
                String str = (String)value;
                switch(row){
                case 0:
                    int note = NoteParser.getMidiNote(str);
                    if(note < 0)
                        throw new IllegalArgumentException("invalid note: "+str);
                    sequencer.getStep(col-1).setNote(note);
                    break;
                case 1:
                    sequencer.getStep(col-1).setVelocity(Integer.parseInt(str));
                    break;
                case 2:
                    sequencer.getStep(col-1).setDuration(Integer.parseInt(str));
                    break;
                case 3:
                    sequencer.getStep(col-1).setModulation(Integer.parseInt(str));
                    break;
                case 4:
                    sequencer.getStep(col-1).setBend(Integer.parseInt(str));
                    break;
                }
                midiControl.synchronize();
            }catch(Throwable exc){
                status(exc.getMessage());
            }
        }

        public Object getValueAt(int row, int col) { 
            if(col == 0)
                return labels[row];
            switch(row){
            case 0:
                return NoteParser.getStringNote(sequencer.getStep(col - 1).getNote());
            case 1:
                return new Integer(sequencer.getStep(col-1).getVelocity());
            case 2:
                return new Integer(sequencer.getStep(col-1).getDuration());
            case 3:
                return new Integer(sequencer.getStep(col-1).getModulation());
            case 4:
                return new Integer(sequencer.getStep(col-1).getBend());
            }
            return null;
        }
 
        public Class getColumnClass(int row, int col) { 
            if(col == 0 || row == 0)
                return String.class;
            return Integer.class;
        }
        public boolean isCellEditable(int row, int col) { 
//             return false;
            return col != 0; 
        }

//         public Component getTableCellRendererComponent(JTable table,
//                                                        Object value,
//                                                        boolean isSelected,
//                                                        boolean hasFocus,
//                                                        int row,
//                                                        int col){
//             return cells[col][row];
//         }
    }
}