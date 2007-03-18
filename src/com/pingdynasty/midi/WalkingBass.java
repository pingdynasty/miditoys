package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.Toolkit;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;

//       ShortMessage.NOTE_OFF
//       ShortMessage.NOTE_ON
//       ShortMessage.POLY_PRESSURE
//       ShortMessage.CONTROL_CHANGE
//       ShortMessage.PROGRAM_CHANGE
//       ShortMessage.CHANNEL_PRESSURE
//       ShortMessage.PITCH_BEND

class DeviceActionListener implements ActionListener {

    private MidiDevice device;
    private Player player;

    public DeviceActionListener(MidiDevice device, Player player){
        this.device = device;
        this.player = player;
    }

    public void actionPerformed(ActionEvent event) {
        try{
            player.setDevice(device);
        }catch(Exception exc){exc.printStackTrace();}
    }
}

class Player  {
    private Receiver receiver;
    private int channel = 0;
    private int velocity;

    public Player(Receiver receiver){
        this.receiver = receiver;
    }

    public void setDevice(MidiDevice device)
        throws MidiUnavailableException{
        receiver.close();
        device.open();
        receiver = device.getReceiver();
    }

    public void setVelocity(int velocity){
        this.velocity = velocity;
    }

    public void noteon(int note)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
        receiver.send(msg, -1);
    }

    public void noteoff(int note)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.NOTE_OFF,  channel, note, 0);
        receiver.send(msg, -1);
    }

    public void bend(int degree)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.PITCH_BEND,  channel, degree, degree);
        receiver.send(msg, -1);
    }

    public void modulate(int degree)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.CONTROL_CHANGE,  channel, 1, degree);
        receiver.send(msg, -1);
    }

    public void programChange(int bank, int program)
        throws InvalidMidiDataException{
        ShortMessage sm = new ShortMessage( );
        sm.setMessage(ShortMessage.PROGRAM_CHANGE, channel, bank, program);
        receiver.send(sm, -1);
    }

    public void setChannel(int channel)
        throws InvalidMidiDataException{
        this.channel = channel;
    }
}

public class WalkingBass extends JFrame {
    final Player player;
    private boolean noteOn[] = new boolean[1024]; // keep track of notes that are on
    private int[] steps = new int[]{-2, -1, -1, -1, 0, 0, 1, 1, 1, 2};
    private double skew = 0.10;
    private boolean normal = false; // use normal or uniform distribution
    private int duration = 100; // note duration
    private int period = 500; // time between notes
    private static int channel = 0;

    public static void main(String[  ] args) 
        throws MidiUnavailableException, Exception {
//         System.in.read();// eat the eol
//         System.out.println("choose instrument: 0-9");
//         instrument = System.in.read() - 48;

        MidiDevice device = null;
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length; ++i){
            devices[i] = MidiSystem.getMidiDevice(info[i]);
            if(devices[i] instanceof Receiver ||
               devices[i] instanceof Synthesizer){
                System.out.println(info[i].getName());
                device = devices[i];
                break;
            }
        }
        device.open();
        Player player = new Player(device.getReceiver());
        player.setChannel(channel);
        WalkingBass bass = new WalkingBass(player);
        bass.walk();
    }

    public WalkingBass(Player play) 
        throws Exception {
        super("WalkingBass");
        player = play;

        addKeyListener(new KeyAdapter( ) {
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode();
                    try{
                        if(key == KeyEvent.VK_LEFT){
                            player.setChannel(--channel);
                            System.out.println("channel "+channel);
                        }else if(key == KeyEvent.VK_RIGHT){
                            player.setChannel(++channel);
                            System.out.println("channel "+channel);
                        }else{
                            int note = getNote(key);
                            if(note > 0 && !noteOn[note]){
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
                    KeyEvent nextPress = (KeyEvent)Toolkit.getDefaultToolkit().
                        getSystemEventQueue().
                        peekEvent(KeyEvent.KEY_PRESSED);
                    if ((nextPress == null) ||
                        (nextPress.getWhen() != e.getWhen()) ||
                        (nextPress.getKeyCode() != e.getKeyCode())) {
                        int note = getNote(e.getKeyCode());
                        if(note > 0 && noteOn[note]){
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
                    duration = event.getY() * 2;
//                     int bend = 128 - event.getY()/2;
//                     System.out.println("bend "+bend);
//                     try{
//                         if((event.getModifiersEx() & 
//                             MouseEvent.SHIFT_DOWN_MASK) != 0)
//                             player.modulate(bend);
//                         else
//                             player.bend(bend);
//                     }catch(Exception exc){
//                         exc.printStackTrace();
//                     }
                }
            });
        JMenuBar menubar = new JMenuBar();
//         JMenu channelMenu = new JMenu("Channel");
//         JMenuItem item;
//         for(int i=0; i<6; ++i){
//             item = new JMenuItem("Set Channel "+i);
//             item.addActionListener(new ActionListener(  ) {
//                     public void actionPerformed(ActionEvent event) {
//                         try{
//                             player.setChannel(i);
//                         }catch(Exception exc){exc.printStackTrace();}
//                     }
//                 });
//             channelMenu.add(item);
//         }
//         menubar.add(channelMenu);

        JMenu menu = new JMenu("Devices");
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length; ++i){
            devices[i] = MidiSystem.getMidiDevice(info[i]); 
            if(devices[i] instanceof Receiver ||
               devices[i] instanceof Synthesizer){
                JMenuItem item = new JMenuItem(info[i].getName());
                item.addActionListener(new DeviceActionListener(devices[i], player));
                menu.add(item); 
           }
        }
        menubar.add(menu);
        setJMenuBar(menubar);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(255, 255);
        setVisible(true);
    }

    public void walk()
        throws Exception{
        Random random = new Random();
        int direction = 1; // 1 or -1 depending on previous direction - up or down.
        double rand;
        int bucket;
        int note = 61;
        for(;;){
            if(normal)
                rand = random.nextGaussian(); // Gaussian (normal) distribution
            else
                rand = random.nextDouble(); // uniform distribution
            rand *= steps.length - 1;
            rand += (skew * direction);
            bucket = (int)Math.round(rand);
//             bucket = (int)Math.floor(rand);
            if(bucket >= steps.length)
                bucket = steps.length - 1;
            System.out.println("bucket "+bucket+": "+steps[bucket]+" ("+rand+")");
            note += steps[bucket];
            direction = bucket > (steps.length / 2) ? 1 : -1;
            player.noteon(note);
            Thread.currentThread().sleep(duration);
            player.noteoff(note);
            Thread.currentThread().sleep(period-duration);
        }
    }

    /** map a keypress code to a MIDI note */
    public int getNote(int key){
        System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+")");
        if(key >= 0x30 && key <= 0x5a)
            return key;
        return 0;
    }
}