package com.pingdynasty.pong;

import javax.sound.midi.*;
/*
 * Notes: some source pulled from http://www.xnet.se/javaTest/jPong/jPong.html
 * some source pulled from http://www.eecs.tufts.edu/~mchow/excollege/s2006/examples.php
 */

import java.util.Locale;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;

public class Pong extends JPanel implements Runnable  {

    public static final int GAME_END_SCORE = 11;	
    public static final int SCREEN_WIDTH = 300;
    public static final int SCREEN_HEIGHT = 300;
    public static final long FRAME_DELAY = 20;
    public static final int HORIZONTAL_SPEED = 6;

    private Thread animator;
    private Court court;
    private Ball ball;
    private LeftRacket leftRacket;
    private RightRacket rightRacket;
    private RacketController leftController;
    private RacketController rightController;
    private boolean running = true;
    private boolean started = false;

    class KeyAction extends AbstractAction {
        public void actionPerformed(ActionEvent event){
            started = !started;
        }
    }

    class ChangeSpeedAction extends AbstractAction {
        private int speed;
        public ChangeSpeedAction(int speed){
            this.speed = speed;
        }
        public void actionPerformed(ActionEvent event){
            ball.speed.x = speed;
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

    class Court extends Rectangle {

        public Court() {
            super(15, 15, Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT - 50);
        }

        public void check(Ball ball){
            if(ball.pos.x + ball.speed.x <= court.x){
                // goal on left side
                sound(Math.abs(ball.speed.y) * 2 + 10, 100);
                leftController.missed();
                rightController.serve(ball);
            }else if(ball.pos.x + ball.speed.x >= court.width - 20){
                // goal on right side
                sound(Math.abs(ball.speed.y) * 2 + 10, 100);
                rightController.missed();
                leftController.serve(ball);
            }else if(ball.pos.y + ball.speed.y <= court.y){
                // hit top wall
                ball.speed.y *= -1;
            }else if(ball.pos.y + ball.speed.y >= court.height + ball.radius){
                // hit bottom wall
                ball.speed.y *= -1;
            }
        }
    }

    class LeftRacket extends Racket {
        public LeftRacket(){
            // position at left end plus 20 (margin)
            super(new Point(20, Pong.SCREEN_WIDTH / 2 - 25));
        }

        public boolean isLeft(){
            return true;
        }

        public void check(Ball ball){
            if(ball.pos.x + ball.speed.x <= pos.x + size.x // enemyPoint.x + 4
               && ball.pos.x > pos.x
               && ball.pos.y + ball.radius > pos.y 
               && ball.pos.y < pos.y + size.y){
                int offset = hit(ball);
                sound(Math.abs(offset) + 20, Math.abs(ball.speed.y) * 40);
            }
        }

        public void serve(Ball ball){
            ++score;
            // start ball off right away
            ball.pos.x = pos.x + 20 + ball.speed.x; // compensate for extra distance behind racket
            ball.pos.y = pos.y + 25;
//             ball.pos.y = court.height / 2;
            if(pos.y < court.height / 2)
                ball.speed.y = 4;
            else
                ball.speed.y = -4;
        }
    }

    class RightRacket extends Racket {
        public RightRacket(){
            // position at right end minus 20 (margin) and width of pad (6)
            super(new Point(Pong.SCREEN_WIDTH - 26, Pong.SCREEN_HEIGHT / 2 - 25));
        }

        public boolean isLeft(){
            return false;
        }

        public void check(Ball ball){
            if(ball.pos.x + ball.speed.x >= pos.x // - size.x // racketPoint.x - 6
               && ball.pos.x < pos.x
               && ball.pos.y + ball.radius > pos.y 
               && ball.pos.y < pos.y + size.y){
                int offset = hit(ball);
                sound(Math.abs(offset) + 20, Math.abs(ball.speed.y) * 40);
            }
        }

        public void serve(Ball ball){
            ++score;
            // start ball off right away
            ball.pos.x = pos.x - 26 - ball.speed.x; // compensate for extra distance behind racket
            ball.pos.y = pos.y + 25;
            if(pos.y < court.height / 2)
                ball.speed.y = 4;
            else
                ball.speed.y = -4;
        }
    }

    public Pong(){
        court = new Court();
        ball = new Ball();
        rightRacket = new RightRacket();
        leftRacket = new LeftRacket();
        leftController = new ComputerController(leftRacket, ball);
        rightController = new MouseController(rightRacket, this);
//         rightController = new JInputController(rightRacket);
//         rightController = new KeyboardController(rightRacket, this, KeyEvent.VK_UP, KeyEvent.VK_DOWN);

        // set action handler for start/stop game (space bar)
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "start/stop game");
        getActionMap().put("start/stop game", new AbstractAction(){
                public void actionPerformed(ActionEvent event){
                    started = !started;
                }
            });

        ball.speed.x = HORIZONTAL_SPEED;
        ball.speed.y = 2;
        animator = new Thread(this);
        animator.start();
    }

//     public void startOrStop(boolean start){
//         started = start;
//     }

    public void run(){
        while(running){
            if(started){
                // collision detection
                if(ball.speed.x < 0)
                    leftRacket.check(ball);
                else
                    rightRacket.check(ball);
                court.check(ball);
                // allow ball to move
                ball.move();
            }
            // allow rackets to move
            leftController.move();
            rightController.move();
            // update screen
            repaint();
            try{
                Thread.sleep(FRAME_DELAY);
            }catch(InterruptedException e){}
	}
    }

    public void paintComponent(Graphics g){
        g.setColor(Color.black);
        g.fillRect(0, 0, Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT);
        Font defaultFont = new Font("Arial", Font.BOLD, 18);
        g.setFont(defaultFont);
// 		if(rightRacket.score == Pong.GAME_END_SCORE && rightRacket.score > leftRacket.score){
//                     g.drawString("YOU WIN!", 25, 35);
//                 }else if(leftRacket.score == Pong.GAME_END_SCORE && leftRacket.score > rightRacket.score){
//                     g.drawString("YOU LOSE!", 25, 35);
// 		}else{
        g.setColor(Color.gray);
        if(!started)
            g.drawString("space to start", court.width / 2 - 50, court.height / 2);
        g.drawString(Integer.toString(leftRacket.score), 50, 35);
        g.drawString(Integer.toString(rightRacket.score), court.width - 50, 35);
        g.setColor(Color.white);
        g.clipRect(court.x, court.y, court.width - 28, court.height + 1);
        g.drawRect(court.x, court.y, court.width - 30, court.height);
        rightRacket.paint(g);
        leftRacket.paint(g);
        ball.paint(g);
// 		}
	}

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
            // choose first available Syntheziser
            MidiDevice device = null;
            MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
            MidiDevice[] devices = new MidiDevice[info.length];
            for(int i=0; i<info.length; ++i){
                devices[i] = MidiSystem.getMidiDevice(info[i]);
                if(devices[i] instanceof Synthesizer){
                    device = devices[i];
                    break;
                }
            }
            initSound(device);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    private com.pingdynasty.midi.Player midi;
    private int baseduration = 500;

    public void initSound(MidiDevice device)
        throws Exception{
        device.open();
        midi = new SchedulingPlayer(device.getReceiver());
        midi.setVelocity(80);
        midi.setDuration(1200); // duration in milliseconds
        scales = new ScaleMapper(Locale.getDefault());
    }

    public void sound(int value, int duration){
        int note = scales.getNote(value);
        duration += baseduration;
        midi.setDuration(duration);
//         System.out.println("value "+value+" \tnote "+note+" \tduration "+duration);
        try{
            midi.play(note);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void destroy(){
        stop();
    }

    public void stop(){
        running = false;
    }

    public JMenuBar getMenuBar(){
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

        // devices menu
        menu = new JMenu("MIDI");
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length; ++i){
            try{
                devices[i] = MidiSystem.getMidiDevice(info[i]); 
                if(devices[i] instanceof Receiver ||
                   devices[i] instanceof Synthesizer){
                    JMenuItem item = new JMenuItem(info[i].getName());
                    item.addActionListener(new DeviceActionListener(devices[i]));
                    menu.add(item); 
                }
            }catch(MidiUnavailableException exc){
                System.err.println(exc.getMessage());
            }
        }
        menubar.add(menu);
        // scales menu
        menu = new JMenu("Scale");
        String[] scalenames = scales.getScaleNames();
        for(int i=0; i<scalenames.length; ++i){
            JMenuItem item = new JMenuItem(scalenames[i]);
            item.addActionListener(new ScaleActionListener(i));
            menu.add(item);
        }
        menubar.add(menu);

        // speed menu
        menu = new JMenu("Speed");
        group = new ButtonGroup();
        for(int i=4; i<18; i+=2){
            button = new JRadioButtonMenuItem(""+i);
            if(i == HORIZONTAL_SPEED)
                button.setSelected(true);
            button.addActionListener(new ChangeSpeedAction(i));
            group.add(button);
            menu.add(button);
        }
        menubar.add(menu);

        // channel menu
        menu = new JMenu("Channel");
        group = new ButtonGroup();
        for(int i=1; i<16; ++i){
            button = new JRadioButtonMenuItem(""+i);
            if(i == 1)
                button.setSelected(true);
            button.addActionListener(new ChangeChannelAction(i));
            group.add(button);
            menu.add(button);
        }
        menubar.add(menu);

        return menubar;
    }

    protected Component getComponent(){
        return this;
    }

    public static final void main(String[] args)
        throws Exception {
        // create pong
        Pong pong = new Pong();
        pong.initSound();

        // create frame
        JFrame frame = new JFrame("pong");
        frame.setJMenuBar(pong.getMenuBar());
        frame.setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT + 40);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // does this do anything?
        frame.setContentPane(pong);
        frame.setVisible(true);
        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
    }
}
