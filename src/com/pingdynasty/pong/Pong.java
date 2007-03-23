package com.pingdynasty.pong;

import javax.sound.midi.*;
/*
 * Notes: some source pulled from http://www.xnet.se/javaTest/jPong/jPong.html
 * some source pulled from http://www.eecs.tufts.edu/~mchow/excollege/s2006/examples.php
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;

public class Pong extends JPanel implements Runnable, MouseListener, MouseMotionListener {

    public static final int GAME_END_SCORE = 11;	
    public static final int SCREEN_WIDTH_HEIGHT = 300;

    public static final void main(String[] args)
        throws Exception {
        JFrame frame = new JFrame("pong");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(Pong.SCREEN_WIDTH_HEIGHT, Pong.SCREEN_WIDTH_HEIGHT);
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
        frame.setJMenuBar(menubar);

        // Create a general double-buffering strategy
        frame.createBufferStrategy(2);
        // does this do anything?

        pong.start();
    }

    private Thread animator;
    private Rectangle plane;
    private Ball ball;
    private Player player;
    private Enemy enemy;
    private int adjustment = 4;
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
	
    public Pong(){
        plane = new Rectangle(15, 15, Pong.SCREEN_WIDTH_HEIGHT, Pong.SCREEN_WIDTH_HEIGHT - 50);
        ball = new Ball();
        player = new Player();
        enemy = new Enemy();
        initSound();
        addMouseListener(this);
        addMouseMotionListener(this);
    }
	
    public void run(){
        while(running){
            checkPlayer();
            checkEnemy();
            checkWalls();
            ball.move();
            enemy.move();
            repaint();
            try{
                Thread.sleep(35);
            }catch(InterruptedException e){}
	}
    }
	
    public void start(){
        running = true;
        animator = new Thread(this);
        animator.start();
        repaint();
    }

    public void stop(){
        running = false;
    }

    class Ball{
        Point pos = new Point(0, 0);
        Point speed = new Point(0, 0);
        int radius = 8;

        Ball(){
            pos = new Point(Pong.SCREEN_WIDTH_HEIGHT / 2, Pong.SCREEN_WIDTH_HEIGHT / 2);
            speed = new Point(0, 0);
        }

        Ball(Point pos, Point speed){
            this.pos = pos;
            this.speed = speed;
        }

	public void move(){
            pos.x = pos.x + speed.x;
            pos.y = pos.y + speed.y;
	}		

	public void hit(Racket racket){
            int racketHit = pos.y - (racket.pos.y + 25);
            sound(Math.abs(racketHit)+50);
            speed.y += racketHit / 7;
            speed.x *= -1;
        }
    }

    class Racket {
        Point pos;
        Point size = new Point(6, 50);
        int score;
        Racket(Point pos){
            this.pos = pos;
        }
    }

    class Player extends Racket {
        public Player(){
            super(new Point(Pong.SCREEN_WIDTH_HEIGHT - 35, 
                            Pong.SCREEN_WIDTH_HEIGHT / 2 - 25));
        }
        public void check(Ball ball){
            if(ball.pos.x + ball.speed.x >= pos.x - size.x 
               && ball.pos.x < pos.x
               && ball.pos.y + ball.radius > pos.y 
               && ball.pos.y < pos.y + size.y)
                    ball.hit(this);
        }
    }

    class Enemy extends Racket {
        public Enemy(){
            super(new Point(35, ((Pong.SCREEN_WIDTH_HEIGHT / 2) - 25)));
        }
        public void check(Ball ball){
            if(ball.pos.x + ball.speed.x <= pos.x + size.x 
               && ball.pos.x > pos.x
               && ball.pos.y + ball.radius > pos.y 
               && ball.pos.y < pos.y + size.y)
                ball.hit(this);
        }

	public void move(){
            int enemyPos = pos.y + 25;
            int dist = java.lang.Math.abs(ball.pos.y - enemyPos);
            if (ball.speed.x < 0){
                if (enemyPos < (ball.pos.y - 3))
                    pos.y = (pos.y + dist/adjustment);
                else if (enemyPos > (ball.pos.y + 3))
                    pos.y = (pos.y - dist/adjustment);
            }else{
                if (enemyPos < (Pong.SCREEN_WIDTH_HEIGHT / 2 - 3))
                    pos.y = (pos.y + 2);
                else if (enemyPos > (Pong.SCREEN_WIDTH_HEIGHT / 2 + 3))
                    pos.y = (pos.y - 2);
            }
	}
    }

    public void checkPlayer(){
        if (ball.speed.x < 0)
            return;
        player.check(ball);
    }
	
    public void checkEnemy(){
        if (ball.speed.x > 0)
            return;
        enemy.check(ball);
    }
	
    public void checkWalls(){
        if((ball.pos.x + ball.speed.x) <= plane.x)
            miss();
        if((ball.pos.x + ball.speed.x) >= (plane.width - 20))
            miss();	
        if((ball.pos.y + ball.speed.y) <= plane.y)
            ball.speed.y *= -1;
        if((ball.pos.y + ball.speed.y) >= (plane.height + 8))
            ball.speed.y *= -1;
    }
	
	public void miss(){
            sound(Math.abs(ball.speed.y)*4+20);
		if(ball.speed.x < 0){
                    ++player.score;
                    if(adjustment > 1)
                        --adjustment;
		}else{
                    ++enemy.score;
                }
		ball.speed.x *= -1;
		ball.pos.x += ball.speed.x;
                // flash screen and/or border?

                ball = new Ball();
		ball.pos.x = enemy.pos.x;
                ball.pos.y = enemy.pos.y;

                // start ball off right away
                ball.speed.x = 14;
                ball.speed.y = 5;
// 		started = false;
	}

    public void paintComponent(Graphics g){
		g.setColor(Color.black);
		g.fillRect(0, 0, Pong.SCREEN_WIDTH_HEIGHT, Pong.SCREEN_WIDTH_HEIGHT);
                g.setColor(Color.white);
		Font defaultFont = new Font("Arial", Font.BOLD, 18);
		g.setFont(defaultFont);
// 		if (player.score == Pong.GAME_END_SCORE && player.score > enemy.score){
//                     g.drawString("YOU WIN!", 25, 35);
//                 }else if (enemy.score == Pong.GAME_END_SCORE && enemy.score > player.score){
//                     g.drawString("YOU LOSE!", 25, 35);
// 		}else{
                    g.drawString(Integer.toString(enemy.score), 100, 35);
                    g.drawString(Integer.toString(player.score), plane.width - 100, 35);
                    g.clipRect(plane.x, plane.y, plane.width - 28, plane.height + 1);
                    g.drawRect(plane.x, plane.y, plane.width - 30, plane.height);
                    g.fillRect(player.pos.x, player.pos.y, 6,50);
                    g.fillRect(enemy.pos.x, enemy.pos.y, 6, 50);
                    g.fillOval(ball.pos.x, ball.pos.y, 8, 8);
// 		}
	}
	
    public void mouseMoved(MouseEvent e){
        player.pos.y = e.getY() - 25;
        repaint();
    }
	
    public void mouseDragged (MouseEvent e) {}
	
    public void mouseClicked(MouseEvent e){
        if(!started){
            ball.speed.x = 14;
            ball.speed.y = 5;
            started = true;
        }
    }

    public void mousePressed(MouseEvent e) {}
    
    public void mouseReleased(MouseEvent e) {}
    
    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    private com.pingdynasty.midi.Player midi;

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

    public void initSound(MidiDevice device)
        throws Exception{
            device.open();
            midi = new SchedulingPlayer(device.getReceiver());
            midi.setVelocity(80);
            midi.setDuration(240); // duration in milliseconds

    }

    public void sound(int value){
//         System.out.println("note "+value);
        try{
            midi.play(value);
//             midi.noteon(value);
//             Thread.sleep(35);
//             midi.noteoff(value);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }
}
