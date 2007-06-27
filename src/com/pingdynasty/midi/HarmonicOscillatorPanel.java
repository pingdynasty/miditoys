package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class HarmonicOscillatorPanel extends JPanel {

    private HarmonicOscillator osc;
    private HarmonicOscillatorControlPanel control;
    private OscillatorPanel view;
    private AudioLineOutput output;

    public HarmonicOscillatorPanel(int samples, int controls, int width, int height)
        throws Exception {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        osc = new HarmonicOscillator(samples, controls);
        control = new HarmonicOscillatorControlPanel(osc);
        view = new OscillatorPanel(width);
        output = new AudioLineOutput(samples, AudioOutput.PCM16SL);
        output.openLine(22050.0f, samples * 4);

        Box controlbox = Box.createHorizontalBox();
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 127, 63);
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
                    int value = (int)source.getValue();
                    output.setScaleFactor(value);
                }
            });
        controlbox.add(slider);
        slider = new JSlider(JSlider.VERTICAL, 0, 180, 30);
        slider.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent event) {
                    JSlider source = (JSlider)event.getSource();
                    int value = (int)source.getValue();
                    osc.setTimeStep(0.001 * value);
                }
            });
        controlbox.add(slider);

        add(control);
        add(view);
        add(controlbox);

        Dimension dim = new Dimension(width, height);
        setPreferredSize(dim);
        setMinimumSize(dim);
        height = height / 3;
        dim = new Dimension(width, height);
        control.setPreferredSize(dim);
        control.setMinimumSize(dim);
        view.setPreferredSize(dim);
        view.setMinimumSize(dim);
        controlbox.setPreferredSize(dim);
        controlbox.setMinimumSize(dim);
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
        int controls = 10;
        int width = 512;
        int height = 200;
        HarmonicOscillatorPanel panel = new HarmonicOscillatorPanel(samples, controls, width, height);

        JFrame frame = new JFrame("harmonic oscillator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height + 60);
        frame.setContentPane(panel);
        frame.setVisible(true);

        for(;;)
            panel.tick();
    }
}
