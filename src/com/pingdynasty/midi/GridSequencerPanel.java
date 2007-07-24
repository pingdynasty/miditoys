package com.pingdynasty.midi;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GridSequencerPanel extends JPanel {
    private GridSequencer sequencer;

    private class ButtonAction extends AbstractAction {
        private int step;
        private int note;

        public ButtonAction(int step, int note){
            this.step = step;
            this.note = note;
        }

        public void actionPerformed(ActionEvent event){
//             System.out.println("step "+step+"/"+note+": "+sequencer.isNoteOn(step, note));
            sequencer.toggleNote(step, note);
        }
    }

    public GridSequencerPanel(GridSequencer sequencer){
        this.sequencer = sequencer;
        this.setLayout(new SpringLayout());
        int length = sequencer.getLength();
        AbstractButton button;
        for(int step=0; step<length; ++step){
            for(int note=60; note<length+60; ++note){
                button = new JRadioButton(new ButtonAction(step, note));
                if(sequencer.isNoteOn(step, note))
                    button.setSelected(true);
                add(button);
            }
        }
        SpringUtilities.makeCompactGrid(this, length, length, 15, 15, 5, 5);
    }

}