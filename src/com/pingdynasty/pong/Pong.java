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

public class Pong extends JPanel implements Runnable, MouseListener  {

    public static final int GAME_END_SCORE = 11;	
    public static final int SCREEN_WIDTH = 300;
    public static final int SCREEN_HEIGHT = 300;

    private Thread animator;
    private Court court;
    private Ball ball;
    private LeftRacket lefty;
    private RightRacket righty;
    private ComputerController computer;
    private RacketController player;
    private boolean running;
    private boolean started;

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
                righty.serve(ball);
                if(computer.adjustment > 1)
                    --computer.adjustment;
                miss();
            }else if(ball.pos.x + ball.speed.x >= court.width - 20){
                lefty.serve(ball);
                miss();
            }else if(ball.pos.y + ball.speed.y <= court.y){
                ball.speed.y *= -1;
            }else if(ball.pos.y + ball.speed.y >= court.height + ball.radius){
                ball.speed.y *= -1;
            }
        }
    }

    class LeftRacket extends Racket {
        public LeftRacket(){
            // position at left end plus 20 (margin)
            super(new Point(20, Pong.SCREEN_WIDTH / 2 - 25));
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
            ball.pos.x = pos.x + 20 + ball.speed.x; // compensate for extra distance behind player
            ball.pos.y = pos.y + 25;
//             ball.pos.y = court.height / 2;
            ball.speed.y = 4;
        }
    }

    class RightRacket extends Racket {
        public RightRacket(){
            // position at right end minus 20 (margin) and width of pad (6)
            super(new Point(Pong.SCREEN_WIDTH - 26, Pong.SCREEN_HEIGHT / 2 - 25));
        }

        public void check(Ball ball){
            if(ball.pos.x + ball.speed.x >= pos.x - size.x // racketPoint.x - 6
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
            ball.pos.x = pos.x - 26 - ball.speed.x; // compensate for extra distance behind player
            ball.pos.y = pos.y + 25;
            ball.speed.y = 4;
        }
    }

    public static final void main(String[] args)
        throws Exception {
        JFrame frame = new JFrame("pong");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT);
        Pong pong = new Pong();
        frame.setContentPane(pong);
        frame.setVisible(true);

        // add menu bar
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("Devices");
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        MidiDevice[] devices = new MidiDevice[info.length];
        for(int i=0; i<info.length; ++i){
            devices[i] = MidiSystem.getMidiDevice(info[i]); 
            if(devices[i] instanceof Receiver ||
               devices[i] instanceof Synthesizer){
                JMenuItem item = new JMenuItem(info[i].getName());
                item.addActionListener(pong.new DeviceActionListener(devices[i]));
                menu.add(item); 
           }
        }
        menubar.add(menu);
        menu = new JMenu("Scales");
        String[] scalenames = pong.scales.getScaleNames();
        for(int i=0; i<scalenames.length; ++i){
            JMenuItem item = new JMenuItem(scalenames[i]);
            item.addActionListener(pong.new ScaleActionListener(i));
            menu.add(item);
        }
        menubar.add(menu);
        frame.setJMenuBar(menubar);

        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
        // does this do anything?

        pong.start();
    }
	
    public Pong(){
        court = new Court();
        ball = new Ball();
        righty = new RightRacket();
        lefty = new LeftRacket();
        computer = new ComputerController(lefty, ball);
//         player = new MouseController(righty, this);
        player = new JInputController(righty, this);

        initSound();
        addMouseListener(this);
    }
	
    public void run(){
        while(running){
            // collision detection
            if(ball.speed.x < 0)
                lefty.check(ball);
            else
                righty.check(ball);
            court.check(ball);
            // move
            ball.move();
            computer.move();
            // update screen
            repaint();
            try{
                Thread.sleep(20);
            }catch(InterruptedException e){}
	}
    }

    // start thread
    public void start(){
        running = true;
        animator = new Thread(this);
        animator.start();
        repaint();
    }

    public void stop(){
        running = false;
    }
	
    public void miss(){
        sound(Math.abs(ball.speed.y) * 2 + 10, 100);
        // flash screen and/or border?
//             ball = new Ball();
//             started = false;
    }

    public void paintComponent(Graphics g){
		g.setColor(Color.black);
		g.fillRect(0, 0, Pong.SCREEN_WIDTH, Pong.SCREEN_HEIGHT);
		Font defaultFont = new Font("Arial", Font.BOLD, 18);
		g.setFont(defaultFont);
// 		if (righty.score == Pong.GAME_END_SCORE && righty.score > lefty.score){
//                     g.drawString("YOU WIN!", 25, 35);
//                 }else if (lefty.score == Pong.GAME_END_SCORE && lefty.score > righty.score){
//                     g.drawString("YOU LOSE!", 25, 35);
// 		}else{
                    g.setColor(Color.gray);
                    g.drawString(Integer.toString(lefty.score), 50, 35);
                    g.drawString(Integer.toString(righty.score), court.width - 50, 35);
                    g.setColor(Color.white);
                    g.clipRect(court.x, court.y, court.width - 28, court.height + 1);
                    g.drawRect(court.x, court.y, court.width - 30, court.height);
                    righty.paint(g);
                    lefty.paint(g);
                    ball.paint(g);
// 		}
	}

    public void startGame(){
        ball.speed.x = 10;
        ball.speed.y = 4;
        started = true;
    }
	
    public void mouseMoved(MouseEvent e){
        righty.pos.y = e.getY() - 25;
        repaint();
    }
	
    public void mouseDragged (MouseEvent e) {}
	
    public void mouseClicked(MouseEvent e){
        if(!started)
            startGame();
    }

    public void mousePressed(MouseEvent e) {}
    
    public void mouseReleased(MouseEvent e) {}
    
    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    private ScaleMapper scales;

    class ScaleActionListener implements ActionListener {
        private int scale;
        public ScaleActionListener(int scale){
            this.scale = scale;
        }

        public void actionPerformed(ActionEvent event) {
            scales.setScale(scale);
        }
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
        System.out.println("value "+value+" \tnote "+note+" \tduration "+duration);
        try{
            midi.play(note);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }
}
