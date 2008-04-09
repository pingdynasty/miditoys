package com.pingdynasty.pong;

/*
 * Notes: some source pulled from http://www.xnet.se/javaTest/jPong/jPong.html
 * some source pulled from http://www.eecs.tufts.edu/~mchow/excollege/s2006/examples.php
 */
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import com.pingdynasty.midi.*;

// todo:
// control bpm
// ball bounces off middle of rhs pad and outside lhs pad
// control velocity and note duration
public class Pong extends JPanel implements Receiver  {

//     public static final int GAME_END_SCORE = 11;	
    public static final int SCREEN_WIDTH = 510;
    public static final int SCREEN_HEIGHT = 520;
    private PongConfiguration cfg;
    private MidiSync internalSync; // internal midi sync/scheduling thread
    private Transmitter externalSync;
    private Court court;
    private Ball ball;
    private LeftRacket leftRacket;
    private RightRacket rightRacket;
    private RacketController leftController;
    private RacketController rightController;
    private Animator animator;
    private boolean started = false;
    private static List eventqueue = Collections.synchronizedList(new ArrayList());

    private Player midi;
    private int clock;

    class Animator extends Thread {
//         public static final long FRAME_DELAY = 40; // 25fps
        long FRAME_DELAY = 20; // 50fps
//         public static final long FRAME_DELAY = 10; // 100fps
        private boolean running = true;
        private int tick = 0;
//         private int adjust;
        private long lastAdjust;
        private boolean waiting;

        public Animator(){
            setDaemon(true);
        }

        public void setClock(int clock){
//             int newtick = clock * ticksperclock;
//             adjust += newtick - tick;
//             tick = newtick;
            tick = clock * cfg.ticksperclock;
            if(clock == 1)
                waiting = false;
            interrupt();
        }

        public void adjust(){
            long now = System.currentTimeMillis();
            long delta = now - lastAdjust;
            if(delta < 250)
                delta = 250; // 240 bpm
            else if(delta > 2000) // 30 bpm
                delta = 2000;
            FRAME_DELAY = delta / (cfg.ticksperclock * cfg.clocksperbeat);
            // set frames per second to match the clocks (by factor of ticksperclock)
            lastAdjust = now;
        }

        public void run(){
            while(running){
                // allow rackets to move
                leftController.move();
                rightController.move();
                if(started && !waiting){
                    // collision detection
                    if(ball.speed.x < 0){
                        if(!leftRacket.check(ball)){
                            waiting = true;
                        }else if(!court.check(ball)){
                            leftController.missed();
                            rightController.serve(ball);
                            Pong.enqueue(new Event(Event.SCORE, Event.RIGHT, ball.distance(leftController.racket)));
                            waiting = true;
                        }else{
                            ball.move(++tick);
                        }
                    }else{
                        if(!rightRacket.check(ball)){
                            waiting = true;
                        }else if(!court.check(ball)){
                            Pong.enqueue(new Event(Event.SCORE, Event.LEFT, ball.distance(rightController.racket)));
                            rightController.missed();
                            leftController.serve(ball);
                            waiting = true;
                        }else{
                            ball.move(++tick);
                        }
                    }
                }
                // update screen
                repaint();
                try{
                    sleep(FRAME_DELAY);
                }catch(InterruptedException e){}
            }
        }

        public void close(){
            running = false;
            interrupt();
        }
    }

    class ChangeSpeedAction extends AbstractAction {
        private int speed;
        public ChangeSpeedAction(int speed){
            this.speed = speed;
        }
        public void actionPerformed(ActionEvent event){
            setBPM(speed);
        }
    }

    class ChangeChannelAction extends AbstractAction {
        private int channel;
        public ChangeChannelAction(int channel){
            this.channel = channel;
        }
        public void actionPerformed(ActionEvent event){
            try{
                midi.setChannel(channel);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    class DeviceActionListener implements ActionListener {
        private MidiDevice device;
        public DeviceActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                midi.close();
                initSound(device);
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    class SyncDeviceActionListener implements ActionListener {
        private MidiDevice device;
        public SyncDeviceActionListener(MidiDevice device){
            this.device = device;
        }
        public void actionPerformed(ActionEvent event) {
            try{
                internalSync.disable();
                device.open();
                externalSync = device.getTransmitter();
                externalSync.setReceiver(getMidiSyncReceiver());
            }catch(Exception exc){exc.printStackTrace();}
        }
    }

    public Pong(){
//         super(new PongConfiguration());
        cfg = new PongConfiguration();
        internalSync = new MidiSync(cfg.bpm);
        internalSync.setReceiver(this);
        court = new Court(cfg);
        ball = new Ball(cfg);
//         ball.distance = court.rightgoal - court.leftgoal;
//         ball.resolution = clocksperbeat * ticksperclock;
//         log("ball distance: "+ball.distance);
        rightRacket = new RightRacket(cfg);
        leftRacket = new LeftRacket(cfg);
        leftController = new ComputerController(leftRacket, ball);
        rightController = new MouseController(rightRacket, this);
//         rightController = new JInputController(rightRacket);
//         rightController = new KeyboardController(rightRacket, this, KeyEvent.VK_UP, KeyEvent.VK_DOWN);

        // set action handler for start/stop game (space bar)
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "start/stop game");
//         getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "start/stop game");
//         getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "start/stop game");
        getActionMap().put("start/stop game", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    if(started)
                        stop();
                    else
                        start();
                }
            });

        // set action handler for game reset - escape key
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "reset game");
        getActionMap().put("reset game", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    reset();
                }
            });

        // set action handlers for ticks per clock
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0), "key1command");
        getActionMap().put("key1command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    setClocksPerBeat(cfg.clocksperbeat-12);
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0), "key2command");
        getActionMap().put("key2command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    setClocksPerBeat(cfg.clocksperbeat+12);
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0), "key3command");
        getActionMap().put("key3command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    setTicksPerClock(cfg.ticksperclock-1);
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0), "key4command");
        getActionMap().put("key4command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    setTicksPerClock(cfg.ticksperclock+1);
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_5, 0), "key5command");
        getActionMap().put("key5command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.noterange.x -= 6;
                    log("Note range: "+cfg.noterange.x+" to "+(cfg.noterange.x+cfg.noterange.y));
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0), "key6command");
        getActionMap().put("key6command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.noterange.x += 6;
                    log("Note range: "+cfg.noterange.x+" to "+(cfg.noterange.x+cfg.noterange.y));
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0), "key7command");
        getActionMap().put("key7command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.noterange.y -= 6;
                    log("Note range: "+cfg.noterange.x+" to "+(cfg.noterange.x+cfg.noterange.y));
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_8, 0), "key8command");
        getActionMap().put("key8command", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.noterange.y += 6;
                    log("Note range: "+cfg.noterange.x+" to "+(cfg.noterange.x+cfg.noterange.y));
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "w");
        getActionMap().put("w", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.doWalls = !cfg.doWalls;
                    log("walls "+(cfg.doWalls ? "on" : "off"));
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "b");
        getActionMap().put("b", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.doBend = !cfg.doBend;
                    log("bend "+(cfg.doBend ? "on" : "off"));
                }
            });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "m");
        getActionMap().put("m", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    cfg.doModulation = !cfg.doModulation;
                    log("modulation "+(cfg.doModulation ? "on" : "off"));
                }
            });

//         addMouseMotionListener(new MouseMotionAdapter(){
        addMouseListener(new MouseAdapter(){
                public void mouseExited(MouseEvent e){
                    requestFocusInWindow();
                }
                public void mouseClicked(MouseEvent e){
                    requestFocusInWindow();
                }
            });

        setFocusable(true);
        setBPM(cfg.bpm);

        reset();
        // start pad moving animator thread
        animator = new Animator();
        animator.start();
        internalSync.start(); // start sending ticks for racket movements
    }

    public static void enqueue(Event event){
        eventqueue.add(event);
    }

    private boolean doLog = true;
    public void log(String msg){
        if(cfg.doLog)
            System.err.println(msg);
    }

    public void sendevents(){
        while(!eventqueue.isEmpty()){
            Event event = (Event)eventqueue.remove(0);
            int note = cfg.noterange.x + (event.value * cfg.noterange.y / 30);
//             note = scales.getNote(note);
            log(event.getEventName()+": "+note+" / "+event.value);
            switch(event.type){
            case Event.HIT:
                midi.setVelocity(100);
                try{
                    midi.play(note);
                }catch(Exception exc){exc.printStackTrace();}
                break;
            case Event.MISS:
                midi.setVelocity(100);
                try{
                    midi.play(note);
                }catch(Exception exc){exc.printStackTrace();}
                break;
            case Event.SCORE:
                midi.setVelocity(120);
                try{
                    midi.play(note);
                }catch(Exception exc){exc.printStackTrace();}
                break;
            case Event.WALL:
                midi.setVelocity(60);
                try{
                    midi.play(note);
                }catch(Exception exc){exc.printStackTrace();}
                break;
            }
        }
        try{
            if(cfg.doModulation)
                midi.modulate((ball.pos.y * 120) / cfg.height);
            if(cfg.doBend)
                midi.bend(8192 + (ball.pos.y - cfg.height / 2) * 8192 / cfg.height);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public synchronized void setClocksPerBeat(int cpb){
        if(cpb >= 12){
            cfg.clocksperbeat = cpb;
            ball.update();
            log("clocks per beat: "+cfg.clocksperbeat);
        }
    }
        
    public synchronized void setTicksPerClock(int tpc){
        if(tpc > 0 && tpc <= 6){
            cfg.ticksperclock = tpc;
            ball.update();
            log("ticks per clock: "+cfg.ticksperclock);
        }
            //         animator.adjust();
    }

    public void start(){
        started = true;
//         internalSync.start();
        tick();
    }

    public void stop(){
        started = false;
//         internalSync.stop();
    }

    public void reset(){
//                     if(started)
//                         stop();
        log("reset!");
        rightController.reset();
//         leftController.serve(ball);
        leftController.reset();
//         ball.reset();
        clock = cfg.clockoffset;
        ball.move(cfg.clockoffset*cfg.ticksperclock);
        ball.pos.y = cfg.height / 2;
        ball.speed.y = 2;
    }

    public void tick(){
        if(started){
            animator.setClock(++clock);
            sendevents();
            if(clock == cfg.clocksperbeat){
                clock = 0;
                animator.adjust();
            }
        }
    }

    public void paintComponent(Graphics g){
//         Point location = getLocation();
        Dimension dim = getSize();
        g.setColor(Color.black);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setFont(cfg.font);
// 		if(rightRacket.score == Pong.GAME_END_SCORE && rightRacket.score > leftRacket.score){
//                     g.drawString("YOU WIN!", 25, 35);
//                 }else if(leftRacket.score == Pong.GAME_END_SCORE && leftRacket.score > rightRacket.score){
//                     g.drawString("YOU LOSE!", 25, 35);
// 		}else{
        g.setColor(Color.gray);
        if(!started)
            g.drawString("space to start", cfg.width / 2 - 50, cfg.height / 2);
        g.drawString(Integer.toString(leftRacket.score), 50, 35);
        g.drawString(Integer.toString(rightRacket.score), cfg.width - 50, 35);
        g.setColor(Color.white);
        g.clipRect(cfg.x, cfg.y, cfg.width+1, cfg.height+1);
        court.paint(g);
        rightRacket.paint(g);
        leftRacket.paint(g);
        ball.paint(g);
// 		}
	}

//         g.clipRect(court.x, court.y, court.width - 28, court.height + 1);
//         g.drawRect(court.x, court.y, court.width - 30, court.height);

    private ScaleMapper scales;

    public class ScaleActionListener implements ActionListener {
        private int scale;
        public ScaleActionListener(int scale){
            this.scale = scale;
        }

        public void actionPerformed(ActionEvent event) {
            scales.setScale(scale);
        }
    }

    public String[] getScales(){
        return scales.getScaleNames();
    }

    public void initSound(){
        try{
            // choose first available Receiver or Syntheziser
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
            initSound(device);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void initSound(MidiDevice device)
        throws Exception{
        device.open();
        midi = new SchedulingPlayer(device.getReceiver());
        midi.setVelocity(80);
        midi.setDuration(600); // duration in milliseconds
        scales = new ScaleMapper(Locale.getDefault());
    }

    public void sound(int value, int duration){
        final int baseduration = 500;
        int note = scales.getNote(value);
        duration += baseduration;
        midi.setDuration(duration);
//         log("value "+value+" \tnote "+note+" \tduration "+duration);
        try{
            midi.play(note);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void destroy(){
        stop();
        animator.close();
    }

    public JMenuBar getMenuBar(boolean includeDeviceMenu){
        // create menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu;
        // controller menu, left
        menu = new JMenu("Left");
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem button = new JRadioButtonMenuItem("keyboard");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    leftController.close();
                    leftController = new KeyboardController(leftRacket, getComponent(), KeyEvent.VK_UP, KeyEvent.VK_DOWN);
                }
            });
        group.add(button);
        menu.add(button);
        button = new JRadioButtonMenuItem("mouse");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    leftController.close();
                    leftController = new MouseController(leftRacket, getComponent());
                }
            });
        group.add(button);
        menu.add(button);
//         button = new JRadioButtonMenuItem("gamepad");
//         button.addActionListener(new AbstractAction(){
//                 public void actionPerformed(ActionEvent event){
//                     leftController.close();
//                     leftController = new JInputController(leftRacket);
//                 }
//             });
//         group.add(button);
//         menu.add(button);
        button = new JRadioButtonMenuItem("computer");
        button.setSelected(true);
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    leftController.close();
                    leftController = new ComputerController(leftRacket, ball);
                }
            });
        group.add(button);
        menu.add(button);
        menubar.add(menu);

        // controller menu, right
        menu = new JMenu("Right");
        group = new ButtonGroup();
        button = new JRadioButtonMenuItem("keyboard");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    rightController.close();
                    rightController = new KeyboardController(rightRacket, getComponent(), KeyEvent.VK_UP, KeyEvent.VK_DOWN);
                }
            });
        group.add(button);
        menu.add(button);
        button = new JRadioButtonMenuItem("mouse");
        button.setSelected(true);
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    rightController.close();
                    rightController = new MouseController(rightRacket, getComponent());
                }
            });
        group.add(button);
        menu.add(button);
//         button = new JRadioButtonMenuItem("gamepad");
//         button.addActionListener(new AbstractAction(){
//                 public void actionPerformed(ActionEvent event){
//                     rightController.close();
//                     rightController = new JInputController(rightRacket);
//                 }
//             });
//         group.add(button);
//         menu.add(button);
        button = new JRadioButtonMenuItem("computer");
        button.addActionListener(new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    rightController.close();
                    rightController = new ComputerController(rightRacket, ball);
                }
            });
        group.add(button);
        menu.add(button);
        menubar.add(menu);

        if(includeDeviceMenu){
            // devices menu
            menu = new JMenu("MIDI");
            group = new ButtonGroup();
            MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
            MidiDevice[] devices = new MidiDevice[info.length];
            for(int i=0; i<info.length; ++i){
                try{
                    devices[i] = MidiSystem.getMidiDevice(info[i]); 
                    if(devices[i] instanceof Receiver ||
                       devices[i] instanceof Synthesizer){
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
            menubar.add(menu);
        }

        // scales menu
        menu = new JMenu("Scale");
        group = new ButtonGroup();
        String[] scalenames = scales.getScaleNames();
        for(int i=0; i<scalenames.length; ++i){
            button = new JRadioButtonMenuItem(scalenames[i]);
            if(i == scales.getScaleIndex())
                button.setSelected(true);
            button.addActionListener(new ScaleActionListener(i));
            group.add(button);
            menu.add(button);
        }
        menubar.add(menu);

        // speed menu
        menu = new JMenu("BPM");
        group = new ButtonGroup();
        for(int i=20; i<200; i+=20){
            button = new JRadioButtonMenuItem(""+i);
            if(i == cfg.bpm)
                button.setSelected(true);
            button.addActionListener(new ChangeSpeedAction(i));
            group.add(button);
            menu.add(button);
        }
        menubar.add(menu);

        // channel menu
        menu = new JMenu("Channel");
        group = new ButtonGroup();
        for(int i=0; i<16; ++i){
            button = new JRadioButtonMenuItem(Integer.toString(i+1));
            if(i == 0)
                button.setSelected(true);
            button.addActionListener(new ChangeChannelAction(i));
            group.add(button);
            menu.add(button);
        }
        menubar.add(menu);

        // midi sync
        menu = new JMenu("Sync");
        group = new ButtonGroup();
        button = new JRadioButtonMenuItem(new AbstractAction("internal"){
                public void actionPerformed(ActionEvent event){
                    if(externalSync != null)
                        externalSync.close();
                    internalSync.enable();
                }
            });
        button.setSelected(true);
        group.add(button);
        menu.add(button); 
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length; ++i){
            try{
                devices[i] = MidiSystem.getMidiDevice(info[i]); 
                if(devices[i] instanceof Transmitter){
                    button = new JRadioButtonMenuItem(info[i].getName());
                    button.addActionListener(new SyncDeviceActionListener(devices[i]));
                    group.add(button);
                    menu.add(button); 
                }
            }catch(MidiUnavailableException exc){
                System.err.println(exc.getMessage());
            }
        }
        menubar.add(menu);

        return menubar;
    }

    protected Component getComponent(){
        return this;
    }

    public void send(MidiMessage msg, long time){
        if(msg instanceof ShortMessage){
            try{
                send((ShortMessage)msg, time);
            }catch(Exception exc){
                exc.printStackTrace();
            }
        }else{
            return;
        }
    }

    public void send(ShortMessage msg, long time)
        throws InvalidMidiDataException {
        switch(msg.getStatus()){
        case ShortMessage.TIMING_CLOCK: {
            tick();
            break;
        }
        case ShortMessage.START: {
            if(!started)
                start();
            break;
        }
        case ShortMessage.STOP: {
            if(started)
                stop();
            else
                reset();
            break;
        }
        case ShortMessage.CONTROL_CHANGE: {
            switch(msg.getData1()){
            case 21:
                cfg.noterange.x = msg.getData2();
                log("Note range: "+cfg.noterange.x+" to "+(cfg.noterange.x+cfg.noterange.y));
                break;
            case 22:
                cfg.noterange.y = msg.getData2();
                log("Note range: "+cfg.noterange.x+" to "+(cfg.noterange.x+cfg.noterange.y));
                break;
            case 23:
                cfg.clockoffset = msg.getData2();
                log("Clock offset: "+cfg.clockoffset);
                break;
            case 24:
                cfg.doWalls = msg.getData2() > 63;
                log("walls "+(cfg.doWalls ? "on" : "off"));
                break;
            case 25:
                cfg.doBend = msg.getData2() > 63;
                log("bend "+(cfg.doBend ? "on" : "off"));
                break;
            case 26:
                cfg.doModulation = msg.getData2() > 63;
                log("modulation "+(cfg.doModulation ? "on" : "off"));
                break;
            case 27:
                cfg.doLog = msg.getData2() > 63;
                log("log output "+(cfg.doLog ? "on" : "off"));
                break;

            case 30:
                cfg.computerRacketSkill = msg.getData2();
                leftController.update();
                rightController.update();
                log("Computer skill: "+cfg.computerRacketSkill);
                break;                
            case 31:
                cfg.computerRacketFudge = msg.getData2();
                leftController.update();
                rightController.update();
                log("Computer fudge factor: "+cfg.computerRacketFudge);
                break;                
            }
        }
        }
    }

    public void close(){
        stop();
        animator.close();
    }

    public Receiver getMidiSyncReceiver(){
        return this;
    }

    public void setBPM(int bpm){
        cfg.bpm = bpm;
        ball.speed.x = cfg.width / 48;
//         ball.speed.x = (cfg.width - cfg.x - 20 - ball.speed.x - ball.speed.x) / 48;
        log("width/speed "+cfg.width+"/"+ball.speed.x);
        internalSync.setBPM(bpm);
    }

    public static final void main(String[] args)
        throws Exception {
        // create pong
        Pong pong = new Pong();
        pong.initSound();

        // create frame
        JFrame frame = new JFrame("pong");
        frame.setJMenuBar(pong.getMenuBar(true));
        frame.setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(pong);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
        // does this do anything?
    }
}
