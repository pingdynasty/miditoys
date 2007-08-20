package com.pingdynasty.midp;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.midlet.*;
import javax.microedition.media.*;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

public class HelloMobileCanvas extends GameCanvas implements Runnable {

    private volatile boolean running;
    private Display display;
    private TiledLayer board;
    private LayerManager layout;

    private Player[] players;
    private static final String[] names = new String[]{
        "Bass Pluck Hi Hat.wav",
        "Bass Bend.wav",
        "Bass.wav",
        "Snare and Bass Pluck.wav",
        "Snare and Trumpet.wav",
        "Snare.wav",
        "Trumpet.wav"};
//         "start.wav",
//         "shot.wav",
//         "hit.wav",
//         "loose.wav",
//         "win.wav"};
//         "start.wav",
//         "shot.wav",
//         "hit.wav",
//         "loose.wav",
//         "win.wav",
//         "foo.mp3",
//         "bar.mp3",
//         "snippet.mp3"};

    private static final int[] keys = new int[]{
        KEY_NUM0,
        KEY_NUM1,
        KEY_NUM2,
        KEY_NUM3,
        KEY_NUM4,
        KEY_NUM5,
        KEY_NUM6,
        KEY_NUM7,
        KEY_NUM8,
        KEY_NUM9};
  
    public HelloMobileCanvas(Display display) 
        throws IOException, MediaException {
        super(false); // suppress key events
        board = createBoard();
        players = createPlayers();
        layout = new LayerManager();
        layout.append(board);
        layout.setViewWindow( 0, 0, 160, 160 );
//         layout.setViewWindow(0, 0, getWidth(), getHeight());
        this.display = display;
    }

    public Player[] createPlayers()
        throws IOException, MediaException {
        Player[] players = new Player[names.length];
        for(int i=0; i<players.length; ++i){
            InputStream is = getClass().getResourceAsStream(names[i]);
            if(names[i].endsWith(".wav"))
                players[i] = Manager.createPlayer(is, "audio/x-wav");
            else if(names[i].endsWith(".mp3"))
                players[i] = Manager.createPlayer(is, "audio/x-mp3");
            else
                throw new IOException("unsupported file format: "+names[i]);
            players[i].realize();
            players[i].prefetch();
//             if(i>0)
//                 players[i].setTimeBase(players[i-1].getTimeBase());
        }
        return players;
// Player p = Manager.createPlayer(Manager.MIDI_DEVICE_LOCATOR);
// MIDIControl control = p.getControl("MIDIControl");
    }

    private TiledLayer createBoard() 
        throws IOException {
        // 176 * 220
        Image image = Image.createImage("/shapes.png");
        TiledLayer tiledLayer = new TiledLayer(8, 6, image, 20, 20);
//         int[] map = {
//             1,  2,  3,  4,  5,  6,  7,  8,
//             9, 10, 11, 12, 13, 14, 15, 16,
//             1,  2,  3,  4,  5,  6,  7,  8,
//             1,  2,  3,  4,  5,  6,  7,  8,
//             1,  2,  3,  4,  5,  6,  7,  8,
//             1,  2,  3,  4,  5,  6,  7,  8,
//             1,  2,  3,  4,  5,  6,  7,  8,
//             1,  2,  3,  4,  5,  6,  7,  8,
//             1,  2,  3,  4,  5,  6,  7,  8,
//         };
//         for(int i = 0; i < map.length; i++) {
//             int column = i % 10;
//             int row = (i - column) / 10;
//             tiledLayer.setCell(column, row, map[i]);
//         }
        for(int i=0; i< 8; ++i)
            for(int j=0; j< 6; ++j)
                tiledLayer.setCell(i, j, i*8+j+1);
        return tiledLayer;
    }
  
    public void start() {
        running = true;
        Thread t = new Thread(this);
        t.start();
    }
  
    public void play(int index){
        if(index > -1 && index < players.length){
            try{
                players[index].start();
                board.setCell(2, 2, index+1);
            }catch(MediaException exc){ 
                error(exc.toString());
            }
        }
    }

    public void run() {
        Graphics g = getGraphics();
        int timeStep = 80;
        int lastRenderTime = 0;
        while(running){
            long start = System.currentTimeMillis();
            tick();
            input();
            render(g);
            renderTime(g, lastRenderTime);
            flushGraphics();
            long end = System.currentTimeMillis();
            lastRenderTime = (int)(end - start);
            if(lastRenderTime < timeStep) {
                try{ 
                    Thread.sleep(timeStep - lastRenderTime); 
                }catch(InterruptedException ie) { 
                    stop(); 
                }
            }
        }
    }
  
    private void tick() {

    }

    private void input() {
//         int keyStates = getKeyStates();
//         for(int i=0; i<keys.length; ++i)
//             if((keyStates & keys[i]) != 0)
//                 play(i);
    }

    protected void keyPressed(int keycode){
        for(int i=1; i<keys.length; ++i)
            if(keycode == keys[i])
                play(i-1);
//         switch(keycode){
//         case KEY_NUM0 :
//             play(0);
//             break;
    }

    private void render(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(0xffffff);
        g.fillRect(0, 0, w, h);
        int x = (w - 160) / 2;
        int y = (h - 160) / 2;
        layout.paint(g, x, y);
        g.setColor(0x000000);
        g.drawRect(x, y, 160, 160);
    }
  
    private void renderTime(Graphics g, int time) {
        int w = getWidth();
        int h = getHeight();
        StringBuffer sb = new StringBuffer();
        sb.append(Integer.toString(time));
        sb.append(" ms");
        String s = sb.toString();
        Font font = g.getFont();
        int sw = font.stringWidth(s) + 2;
        int sh = font.getHeight();
        g.setColor(0xffffff);
        g.fillRect(w - sw, h - sh, sw, sh);
        g.setColor(0x000000);
        g.drawRect(w - sw, h - sh, sw, sh);
        g.drawString("" + time + " ms", w, h,
                     Graphics.RIGHT | Graphics.BOTTOM);
    }

    public void stop() {
        running = false;
        for(int i=0; i<players.length; ++i)
            if(players[i] != null)
                players[i].close();
    }

    public void error(String message){
        Alert alert = new Alert("Error", message, null, AlertType.ERROR);
        Displayable current = display.getCurrent();
        if(!(current instanceof Alert)){
            // This next call can't be done when current is an Alert
            display.setCurrent(alert, current);
        }
    }
}

