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
    private ScaleMapper scales;
    private JLabel statusbar;
    private boolean noteOn[] = new boolean[512]; // keep track of notes that are on
    private int[] steps = new int[]{-2, -2, -1, -1, 1, 1, 1, 1, 2, 2};
    private double skew = 0.20;
    private boolean normal = false; // use normal or uniform distribution
    private int duration = 100; // note duration
    private int period = 500; // time between notes
    private boolean doplay = false;
    private static int channel = 0;

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
        scales.setScale(scale);
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
        scales = new ScaleMapper();

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
        String[] scalenames = scales.getScaleNames();
        menu = new JMenu("Scales");
        for(int i=0; i<scalenames.length; ++i){
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
        for(int i=0; i<scalenames.length; ++i){
            JRadioButton button = new JRadioButton(scalenames[i]);
            button.addActionListener(new ScaleActionListener(i));
            button.addKeyListener(this);
            if(i == 0)
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
        int key = scales.getKey(60); // approximate middle C equivalent 
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
            note = scales.getNote(key);
            player.noteon(note);
            Thread.currentThread().sleep(duration);
            player.noteoff(note);
            Thread.currentThread().sleep(period-duration);
        }
    }

    /** map a keypress code to a MIDI note */
    public int getNote(int key){
        int note = scales.getNote(key);
        return note;
    }
}