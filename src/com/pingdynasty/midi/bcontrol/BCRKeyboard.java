package com.pingdynasty.midi.bcontrol;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Locale;
import java.util.Properties;
import com.pingdynasty.midi.*;

public class BCRKeyboard extends JPanel {
    private ReceiverPlayer player;
    private ChannelPanel channelpanel;
    private boolean noteOn[] = new boolean[1024]; // keep track of notes that are on
    private JLabel statusbar;
    private MessageReceiver control;

    public class MessageReceiver implements Receiver {
        private Transmitter transmitter;

        public void setTransmitter(Transmitter transmitter){
            if(this.transmitter != null)
                this.transmitter.close();
            this.transmitter = transmitter;
            transmitter.setReceiver(this);
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

        public void send(ShortMessage msg, long time)
            throws InvalidMidiDataException {
            status("midi msg <"+msg.getStatus()+"><"+msg.getCommand()+"><"+
                   msg.getData1()+"><"+msg.getData2()+">");
            switch(msg.getStatus()){
            case ShortMessage.CONTROL_CHANGE: {
//             status("midi cc <"+msg+"><"+time+"><"+
//                    msg.getCommand()+"><"+msg.getChannel()+"><"+
//                    msg.getData1()+"><"+msg.getData2()+">");
//                 int cmd = msg.getData1();
//                 if(cmd >= 65 && cmd <= 80)
//                     if(
//                     player.noteOn(
//                 repaint();
                break;
            }
            case 153: // note on channel 10
//                 status("midi note <"+msg.getCommand()+"><"+msg.getStatus()+"><"+
//                        msg.getData1()+"><"+msg.getData2()+">");
                int note = msg.getData1();
                if(msg.getData2() == 100){
                    status("note on: "+note);
                    player.noteon(note);
                }else{
                    status("note off: "+note);
                    player.noteoff(note);
                }
                break;
            case ShortMessage.NOTE_ON:
                player.noteon(msg.getData1());
                break;
            case ShortMessage.NOTE_OFF:
                player.noteoff(msg.getData1());
                break;
            case ShortMessage.PITCH_BEND:
                player.bend(msg.getData1());
                break;
            default:
            }
        }

        public void close(){
            if(this.transmitter != null)
                this.transmitter.close();
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

    class TransmitterActionListener implements ActionListener {
        MidiDevice device;
        public TransmitterActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                control.close();
                device.open();
                control.setTransmitter(device.getTransmitter());
                status("MIDI IN device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public BCRKeyboard(ReceiverPlayer player)
        throws Exception {
        this.player = player;
        player.setVelocity(80);
        control = new MessageReceiver();

        JPanel content = new JPanel(new BorderLayout());

        // add status bar
        statusbar = new JLabel("initializing");
        content.add(statusbar, BorderLayout.SOUTH);

        // configuration panel
        JPanel cpanel = new JPanel();

        // channel buttons
        channelpanel = new ChannelPanel(player);
        cpanel.add(channelpanel);
        content.add(cpanel, BorderLayout.EAST);

        add(content);
    }

    public void status(String msg){
        System.err.println(msg);
        statusbar.setText(msg);
    }

    public static void main(String[] args) 
        throws MidiUnavailableException, Exception {

        // choose first available Syntheziser or Receiver device
        MidiDevice device = DeviceLocator.getDevice(Synthesizer.class);
        device.open();
        ReceiverPlayer player = new ReceiverPlayer(device.getReceiver());

        BCRKeyboard keyboard = new BCRKeyboard(player);
        keyboard.status("MIDI OUT device: "+device.getDeviceInfo().getName());

        // create frame
        JFrame frame = new JFrame("B-Control Rotary Keyboard");

        // create menubar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("MIDI IN");
        String[] devicenames = DeviceLocator.getDeviceNames(Transmitter.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(keyboard.new TransmitterActionListener(device));
            menu.add(item); 
        }
        menubar.add(menu);
        menu = new JMenu("MIDI OUT");
        devicenames = DeviceLocator.getDeviceNames(Synthesizer.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(keyboard.new PlayerActionListener(device));
            menu.add(item); 
        }
        devicenames = DeviceLocator.getDeviceNames(Receiver.class);
        for(int i=0; i<devicenames.length; ++i){
            JMenuItem item = new JMenuItem(devicenames[i]);
            device = DeviceLocator.getDevice(devicenames[i]);
            item.addActionListener(keyboard.new PlayerActionListener(device));
            menu.add(item); 
        }
        menubar.add(menu);
        frame.setJMenuBar(menubar);

        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(keyboard);
        frame.setVisible(true);
    }
}