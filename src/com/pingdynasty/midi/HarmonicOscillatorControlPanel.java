package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class HarmonicOscillatorControlPanel extends JPanel {

    private HarmonicOscillator osc;
    private JSlider[] amplitudes;
    private JSlider energy;
    private JButton[] states;
    private boolean synchronising; // not thread safe

    class AmplitudeChangeListener implements ChangeListener {
        private int control;
        public AmplitudeChangeListener(int control){
            this.control = control;
        }
        public void stateChanged(ChangeEvent event) {
            if(!synchronising){
                JSlider source = (JSlider)event.getSource();
                int value = (int)source.getValue();
                osc.setControl(control, value);
                updateEnergy();
            }
        }
    }

    class SingleStateActionListener extends AbstractAction {
        private int control;
        public SingleStateActionListener(int control){
            this.control = control;
        }
        public void actionPerformed(ActionEvent event){
            if(!synchronising){
                osc.setSingleState(control);
                update();
            }
        }
    }

    public HarmonicOscillatorControlPanel(HarmonicOscillator oscillator){
        this.osc = oscillator;

        int controls = osc.getControls();
        amplitudes = new JSlider[controls];
        states = new JButton[controls];

        Box row = Box.createHorizontalBox();
        Box[] columns = new Box[controls];

        Dimension dim = new Dimension(40, 20);
        for(int i=0; i<controls; ++i){
            columns[i] = Box.createVerticalBox();
//             // create text label
//             columns[i].add(new JLabel(""+(i+1)));
            // create single state button
            states[i] = new JButton(""+(i+1));
	states[i].setPreferredSize(dim);
        states[i].setMinimumSize(dim);

            states[i].addActionListener(new SingleStateActionListener(i));
            columns[i].add(states[i]);
            // create amplitude slider
            amplitudes[i] = new JSlider(JSlider.VERTICAL, 0, 127, osc.getControl(i));
            amplitudes[i].addChangeListener(new AmplitudeChangeListener(i));
            columns[i].add(amplitudes[i]);
            row.add(columns[i]);
        }
        energy = new JSlider(JSlider.VERTICAL, 0, 127, osc.getEnergy());
        energy.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    if(!synchronising){
                        JSlider source = (JSlider)event.getSource();
                        int value = (int)source.getValue();
                        osc.setEnergy(value);
                        updateAmplitudes();
                    }
                }
            });
        row.add(energy);
        add(row);
    }

    // update controls from oscillator values
    public void update(){
        this.updateAmplitudes();
        this.updateEnergy();
    }

    public void updateAmplitudes(){
        synchronising = true;
        for(int i=0; i<amplitudes.length; ++i){
            amplitudes[i].setValue(osc.getControl(i));
            System.out.println(i+":\t"+osc.getControl(i));
        }
        synchronising = false;
    }

    public void updateEnergy(){
        synchronising = true;
        energy.setValue(osc.getEnergy());
        System.out.println("e:\t"+osc.getEnergy());
        synchronising = false;
    }
}