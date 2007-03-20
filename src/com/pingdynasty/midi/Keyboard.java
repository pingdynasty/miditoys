package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.Toolkit;
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

/**
 * this program the MIDI percussion channel with a Swing window.  It monitors
 * keystrokes and mouse motion in the window and uses them to create music.
 * Keycodes between 35 and 81, inclusive, generate different percussive sounds.
 * See the VK_ constants in java.awt.event.KeyEvent, or just experiment.
 * Mouse position controls volume: move the mouse to the right of the window
 * to increase the volume.
 */
public class Keyboard extends JFrame {
    private Player player; // either play through a Receiver or a MidiChannel
    private KeyboardMapper mapper;
    private boolean noteOn[] = new boolean[1024]; // keep track of notes that are on
    private static int bank = 0;
    private static int channel = 0;
    private static int program = 0;

    public static void main(String[  ] args) 
        throws MidiUnavailableException, Exception {
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for(int i=0; i<devices.length; ++i){
            System.out.print("MIDI device "+i+": ");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Receiver)
                System.out.print("receiver\t");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Transmitter)
                System.out.print("transmitter\t");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Synthesizer)
                System.out.print("synthesizer\t");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Sequencer)
                System.out.print("sequencer");
            System.out.println();
            System.out.println("\t"+devices[i].getName()+" ("+
                               devices[i].getDescription()+")\t");
        }

        System.out.println("choose synthesizer or receiver device:");
        int i = System.in.read() - 48;
        System.out.println(devices[i].getName());
        System.in.read();// eat the eol
//         System.out.println("choose instrument: 0-9");
//         instrument = System.in.read() - 48;

        MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
        device.open();
        Player player;
        if(device instanceof Synthesizer){
            Synthesizer synthesizer = (Synthesizer)device;
            player = new SynthesizerPlayer(synthesizer);
        }else if(device instanceof Receiver){
            Receiver receiver = (Receiver)device;
            player = new ReceiverPlayer(receiver);
        }else{
            throw new Exception("invalid MIDI device: "+devices[i].getName());
        }
        player.programChange(bank, program);
        player.setChannel(channel);
        JFrame frame = new Keyboard(player);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(255, 255);
        frame.setVisible(true);
    }

    public Keyboard(Player play)
        throws Exception {
        super("Keyboard");
        player = play;
        
//         Properties props = new Properties();
//         props.setProperty("A", "0");
//         props.setProperty("W", "1");
//         props.setProperty("S", "2");
//         props.setProperty("E", "3");
//         props.setProperty("D", "4");
//         props.setProperty("F", "5");
//         props.setProperty("T", "6");
//         props.setProperty("G", "7");
//         props.setProperty("Y", "8");
//         props.setProperty("H", "9");
//         props.setProperty("U", "10");
//         props.setProperty("J", "11");
        mapper = new KeyboardMapper(Locale.getDefault());

        addKeyListener(new KeyAdapter( ) {
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode();
                    try{
                        if(key == KeyEvent.VK_UP){
                            player.programChange(bank, ++program);
                            System.out.println("program "+program);
                        }else if(key == KeyEvent.VK_DOWN){
                            player.programChange(bank, --program);
                            System.out.println("program "+program);
                        }else if(key == KeyEvent.VK_LEFT){
                            if((e.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0){
                                player.setChannel(--channel);
                                System.out.println("channel "+channel);
                            }else{
                                player.programChange(--bank, program);
                                System.out.println("bank "+bank);
                            }
                        }else if(key == KeyEvent.VK_RIGHT){
                            if((e.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0){
                                player.setChannel(++channel);
                                System.out.println("channel "+channel);
                            }else{
                                player.programChange(++bank, program);
                                System.out.println("bank "+bank);
                            }
                        }else{
                            int note = mapper.getNote(key);
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
                        int note = mapper.getNote(e.getKeyCode());
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
            });
        addMouseMotionListener(new MouseMotionAdapter( ) {
                public void mouseMoved(MouseEvent event) {
                    int velocity = event.getX()/2;
                    System.out.println("velocity "+velocity);
                    player.setVelocity(velocity);
                    int bend = 128 - event.getY()/2;
                    System.out.println("bend "+bend);
                    try{
                        if((event.getModifiersEx() & 
                            MouseEvent.SHIFT_DOWN_MASK) != 0)
                            player.modulate(bend);
                        else
                            player.bend(bend);
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
            });
        JMenuBar menubar = new JMenuBar();
        JMenu channelMenu = new JMenu("Channel");
        JMenuItem item = new JMenuItem("Set Channel 1");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    try{
                        player.setChannel(0);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });
        channelMenu.add(item);
        item = new JMenuItem("Set Channel 2");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    try{
                        player.setChannel(1);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });
        channelMenu.add(item);
        item = new JMenuItem("Set Channel 3");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    try{
                        player.setChannel(2);
                    }catch(Exception exc){exc.printStackTrace();}
                }
            });
        channelMenu.add(item);
        menubar.add(channelMenu);
        setJMenuBar(menubar);
    }
}