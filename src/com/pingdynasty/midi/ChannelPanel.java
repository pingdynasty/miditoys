package com.pingdynasty.midi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChannelPanel extends JPanel {

    private Player player;
    private Action[] actions;
    private AbstractButton[] buttons;

    class ChannelActionListener extends AbstractAction {
        private int channel;
        public ChannelActionListener(int channel){
            super("channel "+(channel+1));
            this.channel = channel;
        }
        public void actionPerformed(ActionEvent event){
            try{
                player.setChannel(channel);
                buttons[channel].setSelected(true);
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public ChannelPanel(Player player){
        this.player = player;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        actions = new Action[16];
        buttons = new JRadioButton[actions.length];
        ButtonGroup group = new ButtonGroup();
        for(int i=0; i<buttons.length; ++i){
            actions[i] = new ChannelActionListener(i);
            buttons[i] = new JRadioButton(actions[i]);
            if(i == 0)
                buttons[i].setSelected(true);
            group.add(buttons[i]);
            add(buttons[i]);
        }
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public JMenu getMenu(){
        JMenu menu = new JMenu("Channels");
        for(int i=0; i<actions.length; ++i){
            JMenuItem item = new JMenuItem(actions[i]);
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