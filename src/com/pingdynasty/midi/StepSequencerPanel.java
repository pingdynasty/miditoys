package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class StepSequencerPanel extends JPanel implements Receiver {

    private StepSequencer sequencer;
    private ReceiverPlayer player;
    private Transmitter transmitter;
    private JLabel statusbar;
    private StepsTableModel model;

    abstract class IntValueChangeListener implements ChangeListener {
        protected int value;
        IntValueChangeListener(int value){
            this.value = value;
        }
    }

    class ReceiverActionListener implements ActionListener {
        MidiDevice device;
        public ReceiverActionListener(MidiDevice device){
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

    class TransmitterActionListener implements ActionListener {
        MidiDevice device;
        public TransmitterActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                if(transmitter != null)
                    transmitter.close();
                device.open();
                transmitter = device.getTransmitter();
                transmitter.setReceiver(getReceiver());
                status("MIDI IN device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public StepSequencerPanel(){
        JPanel content = new JPanel(new BorderLayout());

        // statusbar
        statusbar = new JLabel();
        content.add(statusbar, BorderLayout.SOUTH);

        int width = 8;
        try{
            MidiDevice device = DeviceLocator.getDevice(Receiver.class);
            status("MIDI IN device: "+device.getDeviceInfo().getName());
            player = new SchedulingPlayer(device.getReceiver());
            this.sequencer = new StepSequencer(player, width);
            device = DeviceLocator.getDevice(Transmitter.class);
            if(device != null){
                status("MIDI OUT device: "+device.getDeviceInfo().getName());
                transmitter = device.getTransmitter();
                transmitter.setReceiver(this);
            }
        }catch(MidiUnavailableException exc){
            exc.printStackTrace();
        }

        model = new StepsTableModel(sequencer);
        JTable steps = new JTable(model);
//         steps.getColumnModel().getColumn(0).setCellRenderer(model);
//         steps.getColumnModel().getColumn(3).setCellRenderer(model);
//         steps.setDefaultRenderer(Object.class, model);
        content.add(steps, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        // create play button
        JButton button = new JButton("play");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    sequencer.play();
                }
            });
        buttons.add(button);
        button = new JButton("loop");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    sequencer.start();
                }
            });
        buttons.add(button);
        button = new JButton("stop");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event) {
                    sequencer.stop();
                }
            });
        buttons.add(button);
        content.add(buttons, BorderLayout.EAST);

        // BPM Slider
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 20, 380, 
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

    public void send(MidiMessage message, long time){
        ShortMessage msg;
        if(message instanceof ShortMessage){
            msg = (ShortMessage)message;
        }else{
            return;
        }
        switch(msg.getStatus()){
        case ShortMessage.CONTROL_CHANGE: {
            status("midi cc <"+msg+"><"+time+"><"+
                   msg.getCommand()+"><"+msg.getChannel()+"><"+
                   msg.getData1()+"><"+msg.getData2()+">");
            int cmd = msg.getData1();
            if(cmd >= 1 && cmd <= 9){
                sequencer.getStep(cmd-1).note = msg.getData2();
            }else if(cmd >= 81 && cmd <= 88){
                sequencer.getStep(cmd-81).velocity = msg.getData2();                
            }else if(cmd >= 89 && cmd <= 96){
                sequencer.getStep(cmd-89).duration = msg.getData2();                
            }else if(cmd >= 97 && cmd <= 104){
                sequencer.getStep(cmd-97).modulation = msg.getData2();                
            }
            repaint();
            break;
        }
        default:
            status("midi in <"+msg+"><"+time+">");
        }
    }

    public Receiver getReceiver(){
        return this;
    }

    public void status(String msg){
        statusbar.setText(msg);
//         statusbar.repaint();
    }

    public static void main(String[] args)
        throws Exception {
        StepSequencerPanel panel = new StepSequencerPanel();

        // create frame
        JFrame frame = new JFrame("step sequencer");
        // menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("MIDI OUT");
        String[] devicenames = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new ReceiverActionListener(device));
            menu.add(item); 
        }
        devicenames = DeviceLocator.getDeviceNames(Synthesizer.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new ReceiverActionListener(device));
            menu.add(item); 
        }
        menubar.add(menu);
        menu = new JMenu("MIDI IN");
        devicenames = DeviceLocator.getDeviceNames(Transmitter.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new TransmitterActionListener(device));
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
        public Object getValueAt(int row, int col) { 
            if(col == 0)
                return labels[row];
            switch(row){
            case 0:
                return sequencer.getStep(col-1).note;
            case 1:
                return sequencer.getStep(col-1).velocity;
            case 2:
                return sequencer.getStep(col-1).duration;
            case 3:
                return sequencer.getStep(col-1).modulation;
            case 4:
                return sequencer.getStep(col-1).bend;
            }
            return null;
        }
        public Class getColumnClass(int row, int col) { 
            if(col == 0)
                return String.class;
            return Integer.class;
        }
        public boolean isCellEditable(int row, int col) { 
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