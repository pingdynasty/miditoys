package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class HarmonicOscillatorPanel extends JPanel {

    private HarmonicOscillator osc;
    private HarmonicOscillatorControlPanel control;
    private OscillatorPanel view;
    private AudioOutput output;

    public HarmonicOscillatorPanel(int samples, int width, int height)
        throws Exception {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        osc = new HarmonicOscillator(samples);
        control = new HarmonicOscillatorControlPanel(osc);
        view = new OscillatorPanel(width);
        output = new AudioOutput(samples);

        Box controls = Box.createHorizontalBox();
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 127, 63); // hopefully midways is good
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
                    int value = (int)source.getValue();
                    output.setSampleRate(value);
                }
            });
        controls.add(slider);
        slider = new JSlider(JSlider.VERTICAL, 0, 255, 100);
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
                    double value = (double)source.getValue();
                    output.setScaleFactor(value);
                }
            });
        controls.add(slider);
        slider = new JSlider(JSlider.VERTICAL, 0, 180, 30);
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
                    int value = (int)source.getValue();
                    osc.setTimeStep(0.001 * value);
                }
            });
        controls.add(slider);

        add(control);
        add(view);
        add(controls);

        Dimension dim = new Dimension(width, height);
        setPreferredSize(dim);
        setMinimumSize(dim);
        height = height / 3;
        dim = new Dimension(width, height);
        control.setPreferredSize(dim);
        control.setMinimumSize(dim);
        view.setPreferredSize(dim);
        view.setMinimumSize(dim);
        controls.setPreferredSize(dim);
        controls.setMinimumSize(dim);
    }

    public void tick(){
        //             double[] values = osc.calculateNormalized();
        double[] values = osc.calculate();
        //             panel.setNormalizedData(values);
        //             if(i % 10 == 0)
        view.setData(values);
        output.write(values);
        //             Thread.sleep(10);
        osc.increment();
    }

    public static void main(String[] args)
        throws Exception {
        //         int samples = 4096;
        int samples = 512;
        int width = 512;
        int height = 200;
        HarmonicOscillatorPanel panel = new HarmonicOscillatorPanel(samples, width, height);

        JFrame frame = new JFrame("harmonic oscillator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height + 60);
        frame.setContentPane(panel);
        frame.setVisible(true);

        for(;;)
            panel.tick();
    }
}
