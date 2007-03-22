package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Random;
import java.util.Locale;

public class WalkingBass extends JFrame implements KeyListener, ChangeListener {
    private Player player;
    private ControlSurfacePanel surface;
    private ScaleMapper scales;
    private KeyboardMapper keyboard;
    private ChannelPanel channelpanel;
    private JLabel statusbar;
    private boolean noteOn[] = new boolean[512]; // keep track of notes that are on
    private int[] steps = new int[]{-2, -2, -1, -1, -1, 1, 1, 1, 2, 2};
    private double skew = 0.8; // tendency to stay near middle C
    private boolean normal = false; // use normal (true) or uniform (false) distribution
    private int duration = 100; // note duration
    private int period = 500; // time between notes. 500ms == 120bpm.
    private boolean doplay = false;
    private static int channel = 0;

    class DeviceActionListener implements ActionListener {

        private MidiDevice device;

        public DeviceActionListener(MidiDevice device){
            this.device = device;
        }

        public void actionPerformed(ActionEvent event) {
            try{
                int velocity = player.getVelocity();
                int duration = player.getDuration();
                player.close();
                device.open();
                player = new ReceiverPlayer(device.getReceiver());
                player.setVelocity(velocity);
                player.setDuration(duration);
                surface.setPlayer(player);
                channelpanel.setPlayer(player);
                status("MIDI device: "+device.getDeviceInfo().getName());
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
                device = devices[i];
                break;
            }
        }
        device.open();
        Player player = new ReceiverPlayer(device.getReceiver());
        player.setChannel(channel);
        player.setVelocity(60);

        WalkingBass bass = new WalkingBass(player);
        bass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        bass.pack();
        bass.setVisible(true);
        bass.status("MIDI device: "+device.getDeviceInfo().getName());
        bass.walk();
    }

    public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
//         System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+")");
        try{
            if(key == KeyEvent.VK_LEFT){
                skew -= 0.2;
                status("skew "+skew);
            }else if(key == KeyEvent.VK_RIGHT){
                skew += 0.2;
                status("skew "+skew);
            }else if(key == KeyEvent.VK_UP){
                keyboard.changeOctaveUp();
                status("octave "+keyboard.getOctave());
            }else if(key == KeyEvent.VK_DOWN){
                keyboard.changeOctaveDown();
                status("octave "+keyboard.getOctave());
            }else if(key == KeyEvent.VK_SPACE){
                doplay = !doplay;
                status("play: "+doplay);
            }else if(key == KeyEvent.VK_ESCAPE){
                player.allNotesOff();
                player.bend(64);
                player.modulate(0);
                status("escape: reset");
            }else{
                if(key < 0x30 || key > 0x5a)
                    return;
                int note = keyboard.getNote(key);
                if(note >= 0 && !noteOn[note]){
                    try{
                        noteOn[note] = true;
                        player.noteon(note);
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                    status("note "+NoteParser.getStringNote(note)+" ("+note+")");
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
        int note = keyboard.getNote(key);
        if(note >= 0 && noteOn[note]){
            try{
                noteOn[note] = false;
                player.noteoff(note);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    // bpm / period slider change handler
    public void stateChanged(ChangeEvent event) {
        JSlider source = (JSlider)event.getSource();
        if (!source.getValueIsAdjusting()) {
            int bpm = (int)source.getValue();
            period = 60000 / bpm;
        }
    }

    public WalkingBass(Player play) 
        throws Exception {
        super("Walking Bass");
        player = play;
        scales = new ScaleMapper(Locale.getDefault());
        keyboard = new KeyboardMapper(Locale.getDefault());

        JPanel content = new JPanel(new BorderLayout());
//         JPanel content = new JPanel();
//         content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.addKeyListener(this);
        content.setFocusable(true);

        statusbar = new JLabel("initializing");
        content.add(statusbar, BorderLayout.SOUTH);

        JPanel midsection = new JPanel();
        midsection.setLayout(new BoxLayout(midsection, BoxLayout.X_AXIS));
        content.add(midsection, BorderLayout.CENTER);

        surface = new ControlSurfacePanel(player);
//         surface.setOpaque(true);
        surface.setFocusable(true);
        surface.addKeyListener(this);
        midsection.add(surface, BorderLayout.WEST);

//         getRootPane().registerKeyBoardAction(..)

        // configuration controls panel
        JPanel cpanel = new JPanel();
//         cpanel.setLayout(new BoxLayout(cpanel, BoxLayout.X_AXIS));

        // scale buttons
        String[] scalenames = scales.getScaleNames();
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        for(int i=0; i<scalenames.length; ++i){
            JRadioButton button = new JRadioButton(scalenames[i]);
            button.addActionListener(new ScaleActionListener(i));
            button.addKeyListener(this);
            if(i == scales.getScaleIndex())
                button.setSelected(true);
            group.add(button);
            buttons.add(button);
        }
        buttons.setBorder(BorderFactory.createLineBorder(Color.black));
        cpanel.add(buttons);

        // channel buttons
        channelpanel = new ChannelPanel(player);
        channelpanel.addKeyListener(this);
        cpanel.add(channelpanel);
        midsection.add(cpanel);

        // BPM Slider
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 20, 300, 60000/period);
        slider.addChangeListener(this);
        //Turn on labels at major tick marks.
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addKeyListener(this);
        content.add(slider, BorderLayout.NORTH);

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

        menu = channelpanel.getMenu();
        menubar.add(menu);
        menu = new JMenu("Scales");
        for(int i=0; i<scalenames.length; ++i){
            JMenuItem item = new JMenuItem(scalenames[i]);
            item.addActionListener(new ScaleActionListener(i));
            menu.add(item);
        }
        menubar.add(menu);
        setJMenuBar(menubar);

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
        int key = scales.getKey(60); // approximate middle C equivalent key
        int note = 0;
        for(;;){
            while(!doplay){
                Thread.currentThread().sleep(period);                
            }
            if(normal){
                // Gaussian (normal) distribution with mean 0 and standard deviation 1.0
//                 rand = random.nextGaussian() * (steps.length / 4);
                rand = random.nextGaussian();
                key += rand + skew * direction;
            }else{
                // uniform distribution using distribution buckets
                rand = random.nextDouble() * steps.length + skew * direction;
                bucket = (int)Math.round(rand);
                if(bucket >= steps.length)
                    bucket = steps.length - 1;
                else if(bucket < 0)
                    bucket = 0;
                key += steps[bucket];
            }
            direction = key > scales.getKey(60) ? -1 : 1; // higher or lower than middle C
            note = scales.getNote(key);
//             System.out.println("note "+note+" \tskew "+(skew * direction)+" \trand "+rand);
            if(note > -1){
                player.noteon(note);
                Thread.currentThread().sleep(duration);
                player.noteoff(note);
                Thread.currentThread().sleep(period-duration);
            }
        }
    }
}