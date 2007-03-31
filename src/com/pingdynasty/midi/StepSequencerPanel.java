package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

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
        JPanel steps = new JPanel();
        steps.setLayout(new BoxLayout(steps, BoxLayout.X_AXIS));
        Dimension dimension = new Dimension(50, 20);
        // labels
        JPanel step = new JPanel();
        // todo: change to gridbag layout
        step.setLayout(new BoxLayout(step, BoxLayout.Y_AXIS));
        step.add(new JLabel("note", SwingConstants.RIGHT));
        step.add(new JLabel("velocity", SwingConstants.RIGHT));
        step.add(new JLabel("duration", SwingConstants.RIGHT));
        step.add(new JLabel("modulation", SwingConstants.RIGHT));
        step.add(new JLabel("bend", SwingConstants.RIGHT));
        steps.add(step);
        for(int i=0; i<width; ++i){
            step = new JPanel();
            step.setLayout(new BoxLayout(step, BoxLayout.Y_AXIS));
            // note control
            JSpinner spinner = new JSpinner();
            spinner.setPreferredSize(dimension);
            spinner.setValue(60);
            step.add(spinner);
            spinner.addChangeListener(new IntValueChangeListener(i){
                    public void stateChanged(ChangeEvent event) {
                        JSpinner source = (JSpinner)event.getSource();
                        int value = ((Integer)source.getValue()).intValue();
                        if(value > 0 && value < 128)
                            sequencer.getStep(this.value).note = value;
                    }
                });
            // velocity control
            spinner = new JSpinner();
            spinner.setValue(80);
            spinner.setPreferredSize(dimension);
            step.add(spinner);
            spinner.addChangeListener(new IntValueChangeListener(i){
                    public void stateChanged(ChangeEvent event) {
                        JSpinner source = (JSpinner)event.getSource();
                        int value = ((Integer)source.getValue()).intValue();
                        if(value > 0 && value < 128)
                            sequencer.getStep(this.value).note = value;
                    }
                });
            // duration control
            spinner = new JSpinner();
            spinner.setValue(80);
            spinner.setPreferredSize(dimension);
            step.add(spinner);
            spinner.addChangeListener(new IntValueChangeListener(i){
                    public void stateChanged(ChangeEvent event) {
                        JSpinner source = (JSpinner)event.getSource();
                        int value = ((Integer)source.getValue()).intValue();
                        if(value > 0)
                            sequencer.getStep(this.value).duration = value;
                    }
                });
            // modulation control
            spinner = new JSpinner();
            spinner.setValue(80);
            spinner.setPreferredSize(dimension);
            step.add(spinner);
            spinner.addChangeListener(new IntValueChangeListener(i){
                    public void stateChanged(ChangeEvent event) {
                        JSpinner source = (JSpinner)event.getSource();
                        int value = ((Integer)source.getValue()).intValue();
                        if(value > 0)
                            sequencer.getStep(this.value).modulation = value;
                    }
                });
            // bend control
            spinner = new JSpinner();
            spinner.setValue(80);
            spinner.setPreferredSize(dimension);
            step.add(spinner);
            spinner.addChangeListener(new IntValueChangeListener(i){
                    public void stateChanged(ChangeEvent event) {
                        JSpinner source = (JSpinner)event.getSource();
                        int value = ((Integer)source.getValue()).intValue();
                        if(value > 0)
                            sequencer.getStep(this.value).bend = value;
                    }
                });

            steps.add(step);
        }
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
}