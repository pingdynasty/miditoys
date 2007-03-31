package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class StepSequencerPanel extends JPanel {

    private StepSequencer sequencer;
    private Player player;

    abstract class IntValueChangeListener implements ChangeListener {
        protected int value;
        IntValueChangeListener(int value){
            this.value = value;
        }
    }

    class DeviceActionListener implements ActionListener {
        private MidiDevice device;
        public DeviceActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                int velocity = player.getVelocity();
                int duration = player.getDuration();
                player.close();
                device.open();
                player = new ReceiverPlayer(device.getReceiver());
                sequencer.setPlayer(player);
                player.setVelocity(velocity);
                player.setDuration(duration);
//                 status("MIDI device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public StepSequencerPanel(){
        int width = 8;
        try{
            player = DeviceLocator.getPlayer(Receiver.class);
            this.sequencer = new StepSequencer(player, width);
        }catch(MidiUnavailableException exc){
            exc.printStackTrace();
        }
        JPanel content = new JPanel(new BorderLayout());

        StepsTableModel model = new StepsTableModel(width);
        JTable steps = new JTable(model);
//         steps.getColumnModel().getColumn(0).setCellRenderer(model);
//         steps.getColumnModel().getColumn(3).setCellRenderer(model);
        steps.setDefaultRenderer(Object.class, model);
        content.add(steps, BorderLayout.NORTH);

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
        content.add(slider, BorderLayout.SOUTH);

        this.add(content);
    }

    public static void main(String[] args)
        throws Exception {
        StepSequencerPanel panel = new StepSequencerPanel();

        // create frame
        JFrame frame = new JFrame("step sequencer");
        // menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("Devices");
        String[] devicenames = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new DeviceActionListener(device));
            menu.add(item); 
        }
        devicenames = DeviceLocator.getDeviceNames(Synthesizer.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(panel.new DeviceActionListener(device));
            menu.add(item); 
        }
        menubar.add(menu);
        frame.setJMenuBar(menubar);
        // configure frame
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);

    }

    class StepsTableModel extends AbstractTableModel implements TableCellRenderer {
        int width;
        int height = 3;
        String[] labels = new String[]{"note", "velocity", "duration", "modulation", "bend"};

        JComponent[][] cells;

        StepsTableModel(int width){
            this.width = ++width; // +1 for labels
            cells = new JComponent[width][height];
            for(int i=0; i<height; ++i){
                cells[0][i] = new JLabel(labels[i], SwingConstants.RIGHT);
            }
            Dimension dimension = new Dimension(50, 20);
            for(int i=1; i<width; ++i){
                // note control
                JSpinner spinner = new JSpinner();
//                 spinner.setPreferredSize(dimension);
                spinner.setValue(sequencer.getStep(i-1).note);
                spinner.addChangeListener(new IntValueChangeListener(i-1){
                        public void stateChanged(ChangeEvent event) {
                            JSpinner source = (JSpinner)event.getSource();
                            int value = ((Integer)source.getValue()).intValue();
                            if(value > 0 && value < 128)
                                sequencer.getStep(this.value).note = value;
                        }
                    });
                cells[i][0] = spinner;
                // velocity control
                spinner = new JSpinner();
                spinner.setValue(sequencer.getStep(i-1).velocity);
//                 spinner.setPreferredSize(dimension);
                spinner.addChangeListener(new IntValueChangeListener(i-1){
                        public void stateChanged(ChangeEvent event) {
                            JSpinner source = (JSpinner)event.getSource();
                            int value = ((Integer)source.getValue()).intValue();
                            if(value > 0 && value < 128)
                                sequencer.getStep(this.value).note = value;
                        }
                    });
                cells[i][1] = spinner;
                // bend control
                spinner = new JSpinner();
                spinner.setValue(sequencer.getStep(i-1).bend);
//                 spinner.setPreferredSize(dimension);
                spinner.addChangeListener(new IntValueChangeListener(i-1){
                        public void stateChanged(ChangeEvent event) {
                            JSpinner source = (JSpinner)event.getSource();
                            int value = ((Integer)source.getValue()).intValue();
                            if(value > 0)
                                sequencer.getStep(this.value).bend = value;
                        }
                    });
                cells[i][2] = spinner;
            }
        }
        public int getColumnCount() { return width; }
        public int getRowCount() { return height;}
        public Object getValueAt(int row, int col) { 
            return cells[col][row]; 
        }
        public Class getColumnClass(int row, int col) { 
//             return cells[col][row].getClass(); 
            return JComponent.class;
        }
        public boolean isCellEditable(int row, int col) { return false; }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col){
            return cells[col][row];
        }
    }
}