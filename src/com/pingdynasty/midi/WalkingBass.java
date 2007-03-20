package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
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

public class WalkingBass extends JFrame implements KeyListener {
    private Player player;
    private ControlSurfacePanel surface;
    private JLabel statusbar;
    private boolean noteOn[] = new boolean[512]; // keep track of notes that are on
    private int[] steps = new int[]{-2, -2, -1, -1, 1, 1, 1, 1, 2, 2};
    private double skew = 0.20;
    private boolean normal = false; // use normal or uniform distribution
    private int duration = 100; // note duration
    private int period = 500; // time between notes
    private boolean doplay = false;
    private static int channel = 0;
    private String[] scalenames = new String[]{
        "C minor blues scale",
        "C major blues scale",
        "Ionian mode",
        "Dorian mode",
        "Phrygian mode",
        "Lydian mode",
        "Mixolydian mode",
        "Aeolian mode",
        "Locrian mode"};

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
    int scaleindex = 2;

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
            surface.setPlayer(player);
        }catch(Exception exc){exc.printStackTrace();}
    }
}

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

class ScaleActionListener implements ActionListener {
    private int scale;
    public ScaleActionListener(int scale){
        this.scale = scale;
    }

    public void actionPerformed(ActionEvent event) {
        scaleindex = scale;
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
        bass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        bass.setSize(512, 255);
        bass.pack();
        bass.setVisible(true);
        bass.status("all systems go");
        bass.walk();
    }

    public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+")");
        try{
            if(key == KeyEvent.VK_LEFT){
                player.setChannel(--channel);
                System.out.println("channel "+channel);
            }else if(key == KeyEvent.VK_RIGHT){
                player.setChannel(++channel);
                System.out.println("channel "+channel);
            }else if(key == KeyEvent.VK_SPACE){
                doplay = !doplay;
                status("play: "+doplay);
            }else if(key == KeyEvent.VK_ESCAPE){
                status("escape: reset");
                player.allNotesOff();
                player.bend(0);
                player.modulate(0);
            }else{
                if(key < 0x30 || key > 0x5a)
                    return;
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

    public WalkingBass(Player play) 
        throws Exception {
        super("Walking Bass");
        player = play;

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.gray));
        content.addKeyListener(this);
        content.setFocusable(true);

        statusbar = new JLabel("initializing");
        content.add(statusbar, BorderLayout.SOUTH);

        surface = new ControlSurfacePanel(player);
        surface.setOpaque(true);
        surface.setFocusable(true);
        surface.addKeyListener(this);
        content.add(surface, BorderLayout.WEST);

//         getRootPane().registerKeyBoardAction(..)

        // menus
        JMenuBar menubar = new JMenuBar();
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
        menu = new JMenu("Channels");
        for(int i=0; i<6; ++i){
            JMenuItem item = new JMenuItem("channel "+i);
            item.addActionListener(new ChannelActionListener(i));
            menu.add(item);
        }
        menubar.add(menu);
        menu = new JMenu("Scales");
        for(int i=0; i<scales.length; ++i){
            JMenuItem item = new JMenuItem(scalenames[i]);
            item.addActionListener(new ScaleActionListener(i));
            menu.add(item);
        }
        menubar.add(menu);
        setJMenuBar(menubar);

        // buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        for(int i=0; i<scales.length; ++i){
            JRadioButton button = new JRadioButton(scalenames[i]);
            button.addActionListener(new ScaleActionListener(i));
            button.addKeyListener(this);
            if(i == scaleindex)
                button.setSelected(true);
            group.add(button);
            buttons.add(button);
        }
        buttons.setBorder(BorderFactory.createLineBorder(Color.black));
        content.add(buttons, BorderLayout.EAST);

        setContentPane(content);
    }

    public void status(String msg){
        statusbar.setText(msg);
//         statusbar.repaint();
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
            while(!doplay){
                Thread.currentThread().sleep(period);                
            }
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
//         System.out.println("key "+key+" ("+key % scales[scaleindex].length+"): "+note);
//         System.out.println("key / scales[scaleindex].length = "+key / scales[scaleindex].length);
        note += (key / scales[scaleindex].length) * 12;
        System.out.println("note "+note);
        // 12 is the length of an octave in midi notes
        return note;
    }
}