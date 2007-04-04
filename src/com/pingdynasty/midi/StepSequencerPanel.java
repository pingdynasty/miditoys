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
    private ReceiverPlayer player;
    private JLabel statusbar;
    private StepsTableModel model;
    private Timer timer;
    private JSlider slider;
    private ControlSurfaceHandler control;
    private boolean midiSync = false;

    public class ControlSurfaceHandler implements Receiver {
        private Transmitter transmitter;
        private Receiver receiver;
        MidiDevice device;

        ControlSurfaceHandler(){}

        ControlSurfaceHandler(Transmitter transmitter, Receiver receiver){
            this.transmitter = transmitter;
            this.receiver = receiver;
        }

        public void setTransmitter(Transmitter transmitter){
            if(this.transmitter != null)
                this.transmitter.close();
            this.transmitter = transmitter;
            transmitter.setReceiver(this);
        }

        public void setReceiver(Receiver receiver){
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
                    sequencer.getStep(cmd-1).note = msg.getData2();
                }else if(cmd >= 81 && cmd <= 88){
                    sequencer.getStep(cmd-81).velocity = msg.getData2();
                }else if(cmd >= 89 && cmd <= 96){
                    sequencer.getStep(cmd-89).duration = msg.getData2();
                }else if(cmd >= 97 && cmd <= 104){
                    sequencer.getStep(cmd-97).modulation = msg.getData2();
                }else if(cmd >= 33 && cmd <= 40){
                    sequencer.play(sequencer.getStep(cmd-33));
                }else if(cmd == 105){
                    if(sequencer.isStarted())
                        sequencer.stop();
                    else
                        sequencer.start();
                }
                repaint(); // todo: check if necessary
                break;
            }
            case ShortMessage.NOTE_ON:
            case ShortMessage.NOTE_OFF:
            case ShortMessage.PITCH_BEND:
                break;
            default:
                status("midi in <"+msg+"><"+msg.getStatus()+">");
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
            for(int i=0; i<8; ++i){
                StepSequencer.Step step = sequencer.getStep(i); 
                try{
                    controlChange(1+i, step.note);
                    controlChange(81+i, step.velocity);
                    controlChange(89+i, step.duration);
                    controlChange(97+i, step.modulation);
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

    class ReceiverActionListener implements ActionListener {
        MidiDevice device;
        public ReceiverActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                device.open();
                control.setReceiver(device.getReceiver());
                status("Control Surface output device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    class TransmitterActionListener implements ActionListener {
        MidiDevice device;
        public TransmitterActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                device.open();
                control.setTransmitter(device.getTransmitter());
                status("Control Surface output device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    class PlayerActionListener implements ActionListener {
        MidiDevice device;
        public PlayerActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                player.close();
                device.open();
                player.setReceiver(device.getReceiver());
                status("MIDI OUT device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public StepSequencerPanel(int width)
        throws MidiUnavailableException{
        timer = new Timer();
        control = new ControlSurfaceHandler();
        JPanel content = new JPanel(new BorderLayout());

        // statusbar
        statusbar = new JLabel();
        content.add(statusbar, BorderLayout.SOUTH);

        MidiDevice device = DeviceLocator.getDevice(Synthesizer.class);
        device.open();
        status("MIDI OUT device: "+device.getDeviceInfo().getName());
        player = new SchedulingPlayer(device.getReceiver());
        this.sequencer = new StepSequencer(player, width);

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
        // dodgy send settings button
        button = new JButton("send");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    status("send");
                    control.synchronize();
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

    public static void main(String[] args)
        throws Exception {
        StepSequencerPanel panel = new StepSequencerPanel(8);

        // create frame
        JFrame frame = new JFrame("step sequencer");
        // menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("MIDI OUT");
        String[] devicenames = DeviceLocator.getDeviceNames(Synthesizer.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new PlayerActionListener(device));
            menu.add(item); 
        }
        devicenames = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new PlayerActionListener(device));
            menu.add(item); 
        }
        menubar.add(menu);
        menu = new JMenu("Control Surface");
        menu.add(new JLabel("input"));
        devicenames = DeviceLocator.getDeviceNames(Transmitter.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new TransmitterActionListener(device));
            menu.add(item); 
        }
        menu.add(new JLabel("output"));
        devicenames = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new ReceiverActionListener(device));
            menu.add(item); 
        }
        menubar.add(menu);
        frame.setJMenuBar(menubar);
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
                    sequencer.getStep(col-1).note = note;
                    break;
                case 1:
                    sequencer.getStep(col-1).velocity = Integer.parseInt(str);
                    break;
                case 2:
                    sequencer.getStep(col-1).duration = Integer.parseInt(str);
                    break;
                case 3:
                    sequencer.getStep(col-1).modulation = Integer.parseInt(str);
                    break;
                case 4:
                    sequencer.getStep(col-1).bend = Integer.parseInt(str);
                    break;
                }
            }catch(Throwable exc){
                status(exc.getMessage());
            }
        }

        public Object getValueAt(int row, int col) { 
            if(col == 0)
                return labels[row];
            switch(row){
            case 0:
                return NoteParser.getStringNote(sequencer.getStep(col - 1).note);
            case 1:
                return new Integer(sequencer.getStep(col-1).velocity);
            case 2:
                return new Integer(sequencer.getStep(col-1).duration);
            case 3:
                return new Integer(sequencer.getStep(col-1).modulation);
            case 4:
                return new Integer(sequencer.getStep(col-1).bend);
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