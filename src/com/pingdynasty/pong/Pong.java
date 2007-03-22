package com.pingdynasty.pong;

import javax.sound.midi.*;
/*
 * Notes: No double-buffering; some source pulled from http://www.xnet.se/javaTest/jPong/jPong.html
 * some source pulled from http://www.eecs.tufts.edu/~mchow/excollege/s2006/examples.php
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.pingdynasty.midi.*;

public class Pong extends JPanel implements Runnable, MouseListener, MouseMotionListener {

    public static final int GAME_END_SCORE = 11;	
    public static final int SCREEN_WIDTH_HEIGHT = 500;	

    public static final void main(String[] args)
        throws Exception {
        JFrame frame = new JFrame("pong");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(Pong.SCREEN_WIDTH_HEIGHT, Pong.SCREEN_WIDTH_HEIGHT);
        Pong pong = new Pong();
        frame.setContentPane(pong);
        frame.setVisible(true);
        pong.start();
    }

    private Thread animator;
    private Rectangle plane;
    private Ball ball;
    private Player player;
    private Enemy enemy;
    private boolean start, death;
    private int playerScore, enemyScore, adjustment;
	
    public Pong(){
        death = false;
        playerScore = 0;
        enemyScore = 0;
        adjustment = 1;
        plane = new Rectangle(15, 15, Pong.SCREEN_WIDTH_HEIGHT, Pong.SCREEN_WIDTH_HEIGHT - 50);
        ball = new Ball();
        player = new Player();
        enemy = new Enemy();
        initSound();
        addMouseListener(this);
        addMouseMotionListener(this);
        repaint();
    }
	
	public void run()
	{
		while (Thread.currentThread() == animator)
		{
			checkPlayer();
			checkEnemy();
			checkWalls();
			ball.move();
                        enemy.move();
			repaint();
			try
			{
				Thread.sleep(35);
			}
			catch (InterruptedException e)
			{ 
				System.err.println("An error occurred: " + e.toString());
				break;
			}
		}	
	}
	
	public void start()
	{
		animator = new Thread(this);
		animator.start();			
	}
	
	public void stop()
	{
		animator = null;		
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
            int racketHit = pos.y - racket.pos.y + 25;
            sound(racketHit);
            speed.y += racketHit / 7;
            speed.x *= -1;
//             // calculate time for ball to reach plane.width
//             int time = plane.width / speed.x;
//             // calculate y position when x = plane.width
// //             int y = speed.y * time;
//             Point delta = new Point(plane.width, speed.y * time);
//             double distance = Math.sqrt(delta.x * delta.x + delta.y * delta.y);

//                 double spd = distance / 80;
//                 System.out.println("distance "+distance+" "+spd);
//                 // calculate x/y speeds
//                 double xratio = (double)speed.y / (double)speed.x;
//                 double yratio = (double)speed.x / (double)speed.y;
//                 speed.x = (int)(xratio * spd);
//                 speed.y = (int)(yratio * spd);
//                 System.out.println("ratio "+xratio+" "+yratio);
//                 System.out.println("speed "+speed.x+" "+speed.y);

//             if(speed.x > 0){
//                 // determine total distance to other side (hypothenuse)
// //                 double distance = Math.sqrt(pos.y * pos.y + plane.width * plane.width);
//                 // determine x/y speed as distance divided by constant time
//                 // speed = pixels / s (s = 1000ms)
//                 double spd = distance / 80;
//                 System.out.println("distance "+distance+" "+spd);
//                 // calculate x/y speeds
//                 double xratio = (double)speed.y / (double)speed.x;
//                 double yratio = (double)speed.x / (double)speed.y;
//                 speed.x = (int)(xratio * spd);
//                 speed.y = (int)(yratio * spd);
//                 System.out.println("ratio "+xratio+" "+yratio);
//                 System.out.println("speed "+speed.x+" "+speed.y);
//             }else{
// //                 int distance = (int)Math.sqrt((plane.height - pos.y) * (plane.height - pos.y) + plane.width * plane.width);
// //                 double distance = (int)Math.sqrt(pos.y * pos.y + plane.width * plane.width);
//                 // determine x/y speed as distance divided by constant time
//                 // speed = pixels / s (s = 1000ms)
//                 double spd = distance / 80;
//                 System.out.println("distance "+distance+" "+spd);
//                 // calculate x/y speeds
//                 double xratio = (double)speed.y / (double)speed.x;
//                 double yratio = (double)speed.x / (double)speed.y;
//                 speed.x = (int)(xratio * spd);
//                 speed.y = (int)(yratio * spd);
//                 System.out.println("ratio "+xratio+" "+yratio);
//                 System.out.println("speed "+speed.x+" "+speed.y);
//             }
//             // calculate speed required to reach target in given time
        }
    }

    class Racket {
        Point pos;
        Point size = new Point(6, 50);
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
		if (ball.speed.x < 0)
		{
			playerScore = (playerScore + 1);
			if (adjustment > 2)
				adjustment = (adjustment - 1);
		}
		else
			enemyScore = (enemyScore + 1);
		ball.speed.x *= -1;
		ball.pos.x += ball.speed.x;
		
		for (int i = 3; i > 0; i = (i - 1))
		{
			death = true;
			repaint();
			try
			{
				Thread.sleep(35);
			}
			catch (InterruptedException e) { };
			death = false;
			repaint();
// 			try
// 			{
// 				Thread.sleep(300);
// 			}
// 			catch (InterruptedException e) { };
		}

                ball = new Ball();
// 		ball.pos.x = Pong.SCREEN_WIDTH_HEIGHT/2;
//                 ball.pos.y = Pong.SCREEN_WIDTH_HEIGHT/2;
// 		ball.speed.x = 0;
// 		ball.speed.y = 0;
                ball.speed.x = 14;
                ball.speed.y = 5;
		start = false;
	}

	public void paintComponent (Graphics g)
	{
		g.setColor(Color.black);
		g.fillRect(0, 0, Pong.SCREEN_WIDTH_HEIGHT, Pong.SCREEN_WIDTH_HEIGHT);
		
		if (death == false)
			g.setColor(Color.white);
		else
			g.setColor(Color.lightGray);
		
		Font defaultFont = new Font("Arial", Font.BOLD, 18);
		g.setFont(defaultFont);
		if (playerScore == Pong.GAME_END_SCORE && playerScore > enemyScore)
		{
			g.drawString("YOU WIN!", 25, 35);
		}
		else if (enemyScore == Pong.GAME_END_SCORE && enemyScore > playerScore)
		{
			g.drawString("YOU LOSE!", 25, 35);
		}
		else
		{
			g.drawString(Integer.toString(enemyScore), 100, 35);
			g.drawString(Integer.toString(playerScore), 400, 35);
			g.clipRect(plane.x, plane.y, plane.width - 28, plane.height + 1);
			g.drawRect(plane.x, plane.y, plane.width - 30, plane.height);
			g.fillRect(player.pos.x, player.pos.y, 6,50);
			g.fillRect(enemy.pos.x, enemy.pos.y, 6, 50);
			g.fillOval(ball.pos.x, ball.pos.y, 8, 8);
		}
	}
	
    public void mouseMoved (MouseEvent e){
        player.pos.y = e.getY() - 25;
        repaint();
    }
	
    public void mouseDragged (MouseEvent e) {}
	
    public void mouseClicked(MouseEvent e){
        if (start == false){
            ball.speed.x = 14;
            ball.speed.y = 5;
            start = true;
        }
    }

    public void mousePressed (MouseEvent e) {}
    
    public void mouseReleased (MouseEvent e) {}
    
    public void mouseEntered (MouseEvent e) {}

    public void mouseExited (MouseEvent e) {}

    private ReceiverPlayer midi;

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
            device.open();
            midi = new ReceiverPlayer(device.getReceiver());
            midi.setVelocity(80);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void sound(int value){
        System.out.println(value);
        try{
            midi.noteon(value);
            Thread.sleep(35);
            midi.noteoff(value);
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }
}
