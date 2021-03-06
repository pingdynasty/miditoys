package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Locale;
import java.util.Properties;

//       ShortMessage.NOTE_OFF
//       ShortMessage.NOTE_ON
//       ShortMessage.POLY_PRESSURE
//       ShortMessage.CONTROL_CHANGE
//       ShortMessage.PROGRAM_CHANGE
//       ShortMessage.CHANNEL_PRESSURE
//       ShortMessage.PITCH_BEND

public class Keyboard extends JFrame implements KeyListener {
    private Player player; // either play through a Receiver or a MidiChannel
    private KeyboardMapper keyboard;
    private ControlSurfacePanel surface;
    private ChannelPanel channelpanel;
    private boolean noteOn[] = new boolean[1024]; // keep track of notes that are on
    private static int bank = 0;
    private static int program = 0;
    private JLabel statusbar;

    class DeviceActionListener implements ActionListener {

        private MidiDevice device;

        public DeviceActionListener(MidiDevice device){
            this.device = device;
        }

        public void actionPerformed(ActionEvent event) {
            try{
                int velocity = player.getVelocity();
                int duration = player.getDuration();
                int channel = player.getChannel();
                player.close();
                device.open();
                player = new ReceiverPlayer(device.getReceiver());
                player.setVelocity(velocity);
                player.setDuration(duration);
                player.setChannel(channel);
                surface.setPlayer(player);
                channelpanel.setPlayer(player);
                status("MIDI device: "+device.getDeviceInfo().getName());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        try{
            if(key == KeyEvent.VK_UP){
                keyboard.changeOctaveUp();
                status("octave "+keyboard.getOctave());
            }else if(key == KeyEvent.VK_DOWN){
                keyboard.changeOctaveDown();
                status("octave "+keyboard.getOctave());
            }else if(key == KeyEvent.VK_ESCAPE){
                player.allNotesOff();
                surface.reset();
                status("escape: reset");
            }else{
                int note = keyboard.getNote(key);
                if(note >= 0 && !noteOn[note]){
                    try{
                        noteOn[note] = true;
                        player.noteon(note);
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
            }
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void keyReleased(KeyEvent e) {
        KeyEvent nextPress = (KeyEvent) Toolkit.getDefaultToolkit().
            getSystemEventQueue().
            peekEvent(KeyEvent.KEY_PRESSED);
        if ((nextPress == null) ||
            (nextPress.getWhen() != e.getWhen()) ||
            (nextPress.getKeyCode() != e.getKeyCode())) {
            int note = keyboard.getNote(e.getKeyCode());
            if(note >= 0 && noteOn[note]){
                try{
                    noteOn[note] = false;
                    player.noteoff(note);
                }catch(Exception exc){
                    exc.printStackTrace();
                }
            }
        }
    }

    public Keyboard(Player play)
        throws Exception {
        super("Keyboard");
        player = play;
        keyboard = new KeyboardMapper(Locale.getDefault());
        surface = new ControlSurfacePanel(player);

        JPanel content = new JPanel(new BorderLayout());
        content.addKeyListener(this);
        content.setFocusable(true);

        // add status bar
        statusbar = new JLabel("initializing");
        content.add(statusbar, BorderLayout.SOUTH);

        surface.setFocusable(true);
        surface.addKeyListener(this);
        content.add(surface, BorderLayout.CENTER);

        // configuration panel
        JPanel cpanel = new JPanel();

        // channel buttons
        channelpanel = new ChannelPanel(player);
        channelpanel.addKeyListener(this);
        cpanel.add(channelpanel);
        content.add(cpanel, BorderLayout.EAST);

        setContentPane(content);

        // add menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("Devices");
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem button;
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length; ++i){
            System.out.println("MIDI device: "+info[i].getName());
            try{
                devices[i] = MidiSystem.getMidiDevice(info[i]);
                if(devices[i].getMaxReceivers() != 0){
                    // max is -1 for unlimited
                    button = new JRadioButtonMenuItem(info[i].getName());
                    if(menu.getItemCount() == 0)
                        button.setSelected(true);
                    button.addActionListener(new DeviceActionListener(devices[i]));
                    group.add(button);
                    menu.add(button); 
                }
            }catch(MidiUnavailableException exc){
                System.err.println(exc.getMessage());
            }
        }
// //         String[] devicenames = DeviceLocator.getDeviceNames(Receiver.class);
//         String[] devicenames = DeviceLocator.getReceiverDeviceNames();
// //         String[] devicenames = DeviceLocator.getTransmitterDeviceNames();
//         for(int i=0; i<devicenames.length; ++i){
//             JMenuItem item = new JMenuItem(devicenames[i]);
//             MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
//             item.addActionListener(new DeviceActionListener(device));
//             menu.add(item); 
//         }
// //         devicenames = DeviceLocator.getDeviceNames(Synthesizer.class);
// //         for(int i=0; i<devicenames.length; ++i){
// //             JMenuItem item = new JMenuItem(devicenames[i]);
// //             MidiDevice device = DeviceLocator.getDevice(devicenames[i]);
// //             item.addActionListener(new DeviceActionListener(device));
// //             menu.add(item); 
// //         }
        menubar.add(menu);
        menubar.add(channelpanel.getMenu());
        setJMenuBar(menubar);

        // add key listener
        addKeyListener(this);
    }

    public void status(String msg){
        statusbar.setText(msg);
    }

    public static void main(String[] args) 
        throws MidiUnavailableException, Exception {

        // choose first available Syntheziser or Receiver device
        // choose first available Receiver or Synthesizer device
        MidiDevice device = DeviceLocator.getDevice(Receiver.class);
        if(device == null)
            device = DeviceLocator.getDevice(Synthesizer.class);
        Player player = DeviceLocator.getPlayer(device);

        Keyboard keyboard = new Keyboard(player);
        keyboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        keyboard.pack();
        keyboard.setVisible(true);
        keyboard.status("MIDI device: "+device.getDeviceInfo().getName());
    }
}