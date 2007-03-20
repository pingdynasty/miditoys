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

// class Player  {
//     private Receiver receiver;
//     private int channel = 0;
//     private int velocity;

//     public Player(Receiver receiver){
//         this.receiver = receiver;
//     }

//     public void setDevice(MidiDevice device)
//         throws MidiUnavailableException{
//         receiver.close();
//         device.open();
//         receiver = device.getReceiver();
//     }

//     public void setVelocity(int velocity){
//         this.velocity = velocity;
//     }

//     public void noteon(int note)
//         throws InvalidMidiDataException{
//         ShortMessage msg = new ShortMessage();
//         msg.setMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
//         receiver.send(msg, -1);
//     }

//     public void noteoff(int note)
//         throws InvalidMidiDataException{
//         ShortMessage msg = new ShortMessage();
//         msg.setMessage(ShortMessage.NOTE_OFF,  channel, note, 0);
//         receiver.send(msg, -1);
//     }

//     public void bend(int degree)
//         throws InvalidMidiDataException{
//         ShortMessage msg = new ShortMessage();
//         msg.setMessage(ShortMessage.PITCH_BEND,  channel, degree, degree);
//         receiver.send(msg, -1);
//     }

//     public void modulate(int degree)
//         throws InvalidMidiDataException{
//         ShortMessage msg = new ShortMessage();
//         msg.setMessage(ShortMessage.CONTROL_CHANGE,  channel, 1, degree);
//         receiver.send(msg, -1);
//     }

//     public void programChange(int bank, int program)
//         throws InvalidMidiDataException{
//         ShortMessage sm = new ShortMessage( );
//         sm.setMessage(ShortMessage.PROGRAM_CHANGE, channel, bank, program);
//         receiver.send(sm, -1);
//     }

//     public void setChannel(int channel)
//         throws InvalidMidiDataException{
//         this.channel = channel;
//     }

//     public void allNotesOff()
//         throws InvalidMidiDataException{
//         ShortMessage msg = new ShortMessage();
//         msg.setMessage(ShortMessage.CONTROL_CHANGE,  channel, 123, 0);
//         receiver.send(msg, -1);
//     }
// }

public class WalkingBass extends JFrame {
    private Player player;
    private boolean noteOn[] = new boolean[512]; // keep track of notes that are on
    private int[] steps = new int[]{-2, -2, -1, -1, 1, 1, 1, 1, 2, 2};
    private double skew = 0.20;
    private boolean normal = false; // use normal or uniform distribution
    private int duration = 100; // note duration
    private int period = 500; // time between notes
    private static int channel = 0;
    private int[][] scales = new int[][]{
        // C minor blues scale: C Eb F F# G Bb C
        {0, 3, 5, 6, 7, 10},
        // C major blues scale: C D D# E G A C
        {0, 2, 3, 4, 7, 9},

// the seven modes of the diatonic major scale and added-note scales.
// Ionian mode 	C D E F G A B C 	(associated with C Major 7 chord)
               {0,2,4,5,7,9,11},

// Dorian mode 	C D Eb F G A Bb C 	(associated with C-7 chord)
               {0,2,3, 5,7,9,12},

// Phrygian mode C Db Eb F G Ab Bb C 	(associated with C Phrygian chord)
                {0,1, 3, 5,7,10,12},
// Lydian mode 	C D E F# G A B C 	(associated with C Maj7 #4 chord)
               {0,2,4,6, 7,9,11},
// Mixolydian mode C D E F G A Bb C 	(associated with C7 chord)
                  {0,2,4,5,7,9,12},

// Aeolian mode D Eb F G Ab Bb C 	(associated with C-7 b6 chord)
               {2,3, 5,7,8, 12},
// Locrian mode	C Db Eb F Gb Ab Bb C 	(associated with C-7b5 chord)
               {0,1, 3, 5,6, 8, 12}
    };
    int scaleindex = 0;

class DeviceActionListener implements ActionListener {

    private MidiDevice device;

    public DeviceActionListener(MidiDevice device){
        this.device = device;
    }

    public void actionPerformed(ActionEvent event) {
        try{
            player.close();
            device.open();
            player = new ReceiverPlayer(device.getReceiver());
        }catch(Exception exc){exc.printStackTrace();}
    }
}

    public static void main(String[  ] args) 
        throws MidiUnavailableException, Exception {
        // choose first available Syntheziser or Receiver device
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
        Player player = new ReceiverPlayer(device.getReceiver());
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
//                     System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+")");
                   if(key < 0x30 || key > 0x5a)
                        return;
                    try{
                        if(key == KeyEvent.VK_LEFT){
                            player.setChannel(--channel);
                            System.out.println("channel "+channel);
                        }else if(key == KeyEvent.VK_RIGHT){
                            player.setChannel(++channel);
                            System.out.println("channel "+channel);
                        }else if(key == KeyEvent.VK_ESCAPE){
                            player.allNotesOff();
                            player.bend(0);
                            player.modulate(0);
                        }else{
                            int note = getNote(key - 36);
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
                    int key = e.getKeyCode();
                    if(key < 0x30 || key > 0x5a)
                        return;
                    KeyEvent nextPress = (KeyEvent)Toolkit.getDefaultToolkit().
                        getSystemEventQueue().
                        peekEvent(KeyEvent.KEY_PRESSED);
                    if((nextPress != null) &&
                       (nextPress.getWhen() == e.getWhen()) &&
                       (nextPress.getKeyCode() == key))
                        return;
                    int note = getNote(key - 36);
                    if(note > 0 && noteOn[note]){
                        try{
                            noteOn[note] = false;
                            player.noteoff(note);
                        }catch(Exception exc){
                            exc.printStackTrace();
                        }
                    }
                }
            });
        addMouseMotionListener(new MouseMotionAdapter( ) {
                public void mouseMoved(MouseEvent event) {
                    int velocity = event.getX()/2;
//                     System.out.println("velocity "+velocity);
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
                item.addActionListener(new DeviceActionListener(devices[i]));
                menu.add(item); 
           }
        }
        menubar.add(menu);

        menu = new JMenu("Scales");
        JMenuItem item = new JMenuItem("C minor blues scale");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 0;
                }
            });
        menu.add(item);
        item = new JMenuItem("C major blues scale");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 1;
                }
            });
        menu.add(item);
        item = new JMenuItem("Ionian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 2;
                }
            });
        menu.add(item);
        item = new JMenuItem("Dorian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 3;
                }
            });
        menu.add(item);
        item = new JMenuItem("Phrygian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 4;
                }
            });
        menu.add(item);
        item = new JMenuItem("Lydian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 5;
                }
            });
        menu.add(item);
        item = new JMenuItem("Mixolydian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 6;
                }
            });
        menu.add(item);
        item = new JMenuItem("Aeolian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 7;
                }
            });
        menu.add(item);
        item = new JMenuItem("Locrian mode");
        item.addActionListener(new ActionListener(  ) {
                public void actionPerformed(ActionEvent event) {
                    scaleindex = 8;
                }
            });
        menu.add(item);

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
        int key = (60 / 12) * scales[scaleindex].length; // Middle C equivalent position
        int note = 0;
        for(;;){
            if(normal)
                rand = random.nextGaussian(); // Gaussian (normal) distribution
            else
                rand = random.nextDouble(); // uniform distribution
            rand *= steps.length - 1;
            rand += (skew * direction);
            bucket = (int)Math.round(rand);
            if(bucket >= steps.length)
                bucket = steps.length - 1;
            key += steps[bucket];
            direction = bucket > (steps.length / 2) ? 1 : -1;
            note = getNote(key);
            player.noteon(note);
            Thread.currentThread().sleep(duration);
            player.noteoff(note);
            Thread.currentThread().sleep(period-duration);
        }
    }

    /** map a keypress code to a MIDI note */
    public int getNote(int key){
        int note = scales[scaleindex][key % scales[scaleindex].length];
        System.out.println("key "+key+" ("+key % scales[scaleindex].length+"): "+note);
        System.out.println("key / scales[scaleindex].length = "+key / scales[scaleindex].length);
        note += (key / scales[scaleindex].length) * 12;
        System.out.println("note "+note);
        // 12 is the length of an octave in midi notes
        return note;
    }
}