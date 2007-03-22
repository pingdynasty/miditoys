package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChannelPanel extends JPanel {

    private Player player;
    private JRadioButton[] buttons;

    class ChannelActionListener implements ActionListener {
        private int channel;
        public ChannelActionListener(int channel){
            this.channel = channel;
        }
        public void actionPerformed(ActionEvent event){
            try{
                player.setChannel(channel);
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public ChannelPanel(Player player){
        this.player = player;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buttons = new JRadioButton[16];
        ButtonGroup group = new ButtonGroup();
        for(int i=0; i<buttons.length; ++i){
            buttons[i] = new JRadioButton("channel "+(i+1));
            buttons[i].addActionListener(new ChannelActionListener(i));
            if(i == 0)
                buttons[i].setSelected(true);
            group.add(buttons[i]);
            add(buttons[i]);
        }
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public JMenu getMenu(){
        JMenu menu = new JMenu("Channels");
        for(int i=0; i<16; ++i){
            JMenuItem item = new JMenuItem("channel "+(i+1));
            item.addActionListener(new ChannelActionListener(i));
            menu.add(item);
        }
        return menu;
    }

    public void setPlayer(Player player){
        this.player = player;
    }

    public void addKeyListener(KeyListener listener){
        super.addKeyListener(listener);
        for(int i=0; i<buttons.length; ++i)
            buttons[i].addKeyListener(listener);
    }
}