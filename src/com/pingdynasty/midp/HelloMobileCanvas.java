package com.pingdynasty.midp;

import java.io.*;
import javax.microedition.midlet.*;
import javax.microedition.media.*;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

public class HelloMobileCanvas extends GameCanvas implements Runnable {

    private volatile boolean running;
    private Display display;
    private TiledLayer board;
    private LayerManager layout;
    private Sequence[] sequences;
    private Player[] players;
    private Cursor cursor;
    private boolean editmode = true;
    private boolean playmode = false;
    private MixingPlayer mixer;
    private static final int CLEAR_CELL = 64;

    private static final String[] names = new String[]{
        "Bass Pluck Hi Hat.wav",
        "Bass Bend.wav",
        "Bass Bump.wav",
        "Bass Syncopation.wav",
        "Bass.wav",
        "Snare and Bass Pluck.wav",
        "Snare and Trumpet.wav",
        "Snare.wav",
        "Trumpet.wav"};

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

    public class MixingPlayer {
        private Player player;
        private byte[] buffer = new byte[cellsize * 8 + 44];
        private static final int samplerate = 8000;
        private static final int channels = 2;
        private static final int cellsize // = 32000; // bytes to a beat (8khz * 0.5s * 2 channels * 16bits / 8bitsperbyte)
            = samplerate * channels;

        public MixingPlayer(){
            init();
        }

        public void init(){
            int size = cellsize * 8;
            buffer[0] = 0x52;
            buffer[1] = 0x49;
            buffer[2] = 0x46;
            buffer[3] = 0x46; // RIFF
            buffer[4] = (byte)((36 + size) & 0x00ff);
            buffer[5] = (byte)(((36 + size) >> 8) & 0x00ff);
            buffer[6] = (byte)(((36 + size) >> 16) & 0x00ff);
            buffer[7] = (byte)(((36 + size) >> 32) & 0x00ff);
            buffer[8] = 0x57;
            buffer[9] = 0x41;
            buffer[10] = 0x56;
            buffer[11] = 0x45; // WAVE
            buffer[12] = 0x66;
            buffer[13] = 0x6d;
            buffer[14] = 0x74;
            buffer[15] = 0x20; // fmt
            buffer[16] = 0x10;
            buffer[17] = 0x00;
            buffer[18] = 0x00;
            buffer[19] = 0x00;
            buffer[20] = 0x01;
            buffer[21] = 0x00;
            buffer[22] = 0x02;
            buffer[23] = 0x00;

            buffer[24] = 0x40;
            buffer[25] = 0x1f;
            buffer[26] = 0x00;
            buffer[27] = 0x00; // samplerate
            buffer[28] = 0x00;
            buffer[29] = 0x7d;
            buffer[30] = 0x00;
            buffer[31] = 0x00; // byterate

//             buffer[24] = (byte)(samplerate & 0x00ff);
//             buffer[25] = (byte)((samplerate >> 8) & 0x00ff);
//             buffer[26] = (byte)((samplerate >> 16) & 0x00ff);
//             buffer[27] = (byte)((samplerate >> 32) & 0x00ff); // samplerate
//             buffer[28] = (byte)((samplerate * channels * 2) & 0x00ff);
//             buffer[29] = (byte)(((samplerate * channels * 2) >> 8) & 0x00ff);
//             buffer[30] = (byte)(((samplerate * channels * 2) >> 16) & 0x00ff);
//             buffer[31] = (byte)(((samplerate * channels * 2) >> 32) & 0x00ff); // byterate

            buffer[32] = 0x04;
            buffer[33] = 0x00;
            buffer[34] = 0x10;
            buffer[35] = 0x00; // bits per sample
            // end of subchunk 1
            buffer[36] = 0x64;
            buffer[37] = 0x61;
            buffer[38] = 0x74;
            buffer[39] = 0x61;
            buffer[40] = (byte)(size & 0x00ff);
            buffer[41] = (byte)((size >> 8) & 0x00ff);
            buffer[42] = (byte)((size >> 16) & 0x00ff);
            buffer[43] = (byte)((size >> 32) & 0x00ff);
        }

        public void reset(){
            for(int i=44; i<buffer.length; ++i)
                buffer[i] = 0;
        }

        public void mix(Sequence sequence)
            throws IOException, MediaException {
            for(int i=0; i<sequence.getLength(); ++i){
                Cell cell = sequence.getCell(i);
                int index = cell.getSampleIndex();
                if(index > -1){
                    InputStream is = getClass().getResourceAsStream(names[index]);
                    is.skip(44); // size of header
                    int val = is.read();
                    int offset = i * cellsize + 44;
                    while(val != -1 && offset < buffer.length){
                        buffer[offset++] += val;
                        val = is.read();
                    }
                }
            }
            player = Manager.createPlayer(new ByteArrayInputStream(buffer), "audio/x-wav");
            player.setLoopCount(-1);
        }

        public void start(){
            try{
                if(player != null)
                    player.start();
            }catch(MediaException exc){
                error(exc);
            }
        }

        public void stop(){
            try{
                if(player != null)
                    player.stop();
            }catch(MediaException exc){
                error(exc);
            }
        }
    }

    private class Sequence implements Runnable {
        private final int row;
        private Cell[] cells;
        private int period = 600;
        private boolean running;
        private boolean playing;
        private Cell lastCell;

        public Sequence(int width, int row){
            this.row = row;
            cells = new Cell[width];
            for(int i=0; i<cells.length; ++i){
                cells[i] = new Cell(i, row);
            }
        }

        public int getLength(){
            return cells.length;
        }

        public Cell getCell(int col){
//             assert col < cells.length;
            return cells[col];
        }

        public void start(){
            running = true;
            Thread thread = new Thread(this);
            thread.start();
        }

        public void finish(){
            running = false;
        }

        public void toggle(){
            playing = !playing;
        }

        public void run(){
            while(running){
                if(playing){
                    for(int i=0; i<cells.length && running && playing; ++i){
                        if(lastCell != null)
                            lastCell.stop();
                        cells[i].play();
                        lastCell = cells[i];
                        try{ 
                            Thread.sleep(period); 
                        }catch(InterruptedException ie){}
                    }
//                     playing = false;
                }else{
                    try{ 
                        Thread.sleep(500);
                    }catch(InterruptedException ie){}
                }
            }
        }
    }

    private class Cursor extends Sprite {
        int col, row; // [0-7]
        public static final int MIN = 0;
        public static final int MAX = 7;

        public Cursor(Image image){
            super(image);
            col = MIN;
            row = MIN;
        }

        public void left(){
            if(--col < MIN)
                col = MAX;
            setPosition(col*20, row*20);
        }

        public void right(){
            if(++col > MAX)
                col = MIN;
            setPosition(col*20, row*20);
        }

        public void up(){
            if(--row < MIN)
                row = MAX;
            setPosition(col*20, row*20);
        }

        public void down(){
            if(++row > MAX)
                row = MIN;
            setPosition(col*20, row*20);
        }

        public int getColumn(){
            return col;
        }

        public int getRow(){
            return row;
        }

        public void toggle(){
            sequences[row].toggle();
        }

        public void setSample(int index){
            sequences[row].getCell(col).setSample(index);
        }

        public void clearSample(){
            sequences[row].getCell(col).clearSample();
        }
    }

    public class Cell {
        private int col, row;
        private Player player;

        public Cell(int col, int row){
            this.col = col;
            this.row = row;
        }

        public Player getPlayer(){
            return player;
        }

        public int getSampleIndex(){
            for(int i=0; i<players.length; ++i)
                if(player == players[i])
                    return i;
            return -1;
        }

        public void clearSample(){
            player = null;
            board.setCell(col, row, CLEAR_CELL);
        }

        public void setSample(int index){
//             assert index < players.length;
            player = players[index];
            board.setCell(col, row, index+1);
        }

        public void play(){
            try{
                if(player != null)
                    player.start();
            }catch(MediaException exc){
                error(exc);
            }
        }

        public void stop(){
            try{
                if(player != null)
                    player.stop();
            }catch(MediaException exc){
                error(exc);
            }
        }
    }

    public HelloMobileCanvas(Display display) 
        throws IOException, MediaException {
        super(false); // suppress key events
        board = createBoard();
        players = createPlayers();
        sequences = createSequences(8, 8);
        cursor = createCursor();
        mixer = new MixingPlayer();
        layout = new LayerManager();
        layout.append(cursor);
        layout.append(board);
        layout.setViewWindow(0, 0, 160, 160);
//         layout.setViewWindow(0, 0, getWidth(), getHeight());
        this.display = display;
    }

    public Cursor createCursor()
        throws IOException {
        Image image = Image.createImage("/shape.png");
        Cursor cursor = new Cursor(image);
        return cursor;
    }

    public Sequence[] createSequences(int cols, int rows){
        Sequence[] sequences = new Sequence[rows];
        for(int i=0; i<sequences.length; ++i){
            sequences[i] = new Sequence(cols, i);
//             sequences[i].start(); // create and start thread
        }
        return sequences;
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
        TiledLayer tiledLayer = new TiledLayer(8, 8, image, 20, 20);
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
        for(int i=0; i<8; ++i)
            for(int j=0; j<8; ++j)
                tiledLayer.setCell(i, j, CLEAR_CELL);
        return tiledLayer;
    }
  
    public void start(){
        running = true;
        Thread thread = new Thread(this);
        thread.start();
    }
  
    public void play(int index){
//         if(index > -1 && index < players.length){
            try{
                players[index].start();
                cursor.setSample(index);
                cursor.right(); // move one step forward
            }catch(MediaException exc){ 
                error(exc);
            }
//         }
    }

    public void run() {
        Graphics g = getGraphics();
        int timeStep = 80;
        int lastRenderTime = 0;
        while(running){
            long start = System.currentTimeMillis();
            tick();
            render(g);
            renderStatus(g);
//             renderTime(g, lastRenderTime);
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
        input();
    }

    private void input() {
//         int keyStates = getKeyStates();
//         if((keyStates & LEFT_PRESSED) != 0) 
//             cursor.left();
//         else if((keyStates & RIGHT_PRESSED) != 0) 
//             cursor.right();
//         else if((keyStates & UP_PRESSED) != 0)
//             cursor.up();
//         else if((keyStates & DOWN_PRESSED) != 0)
//             cursor.down();
//         else if((keyStates & FIRE_PRESSED) != 0)
//             cursor.toggle();
    }

    private void checkkey(int keycode){
        switch(keycode){
        case KEY_NUM0:
            cursor.clearSample();
            cursor.right();
            break;
//         case KEY_STAR:
//             playmode = !playmode;
//             if(playmode)
//                 mixer.start();
//             else
//                 mixer.stop();
//             break;
//         case KEY_POUND:
//             editmode = !editmode;
//             break;
        default:
            for(int i=1; i<keys.length; ++i)
                if(keycode == keys[i]){
                    int index = i-1;
                    if(index > -1 && index < players.length){
                        play(index);
                    }
                }
        }
    }

    private void checkaction(int action){
        switch(action){
        case UP:
            cursor.up();
            break;
        case DOWN:
            cursor.down();
            break;
        case LEFT:
            cursor.left();
            break;
        case RIGHT:
            cursor.right();
            break;
        case FIRE:
            cursor.toggle();
            break;
        }
    }

    protected void keyPressed(int keycode){
        if(keycode == KEY_POUND){
            editmode = !editmode;
        }else if(keycode == KEY_STAR){
            playmode = !playmode;
            if(playmode){
                try{
                    mixer.mix(sequences[cursor.getRow()]);
                    mixer.start();
                }catch(Exception exc){
                    error(exc);
                }
            }else{
                mixer.stop();
                mixer.reset();
            }
        }
        if(editmode){
            checkkey(keycode);
        }else{
            checkaction(getGameAction(keycode));
//             if((getKeyStates() & (LEFT_PRESSED | RIGHT_PRESSED | UP_PRESSED | DOWN_PRESSED | FIRE_PRESSED)) == 0)
//                 checkkey(keycode);
        }
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
  
    private void renderStatus(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        String status = (editmode ? "edit" : "play") + (playmode ? " x" : " o");
        Font font = g.getFont();
        int sw = font.stringWidth(status) + 2;
        int sh = font.getHeight();
        g.setColor(0xffffff);
        g.fillRect(w - sw, h - sh, sw, sh);
        g.setColor(0x000000);
        g.drawRect(w - sw, h - sh, sw, sh);
        g.drawString(status, w, h, Graphics.RIGHT | Graphics.BOTTOM);
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
        for(int i=0; i<sequences.length; ++i)
            sequences[i].finish();
    }

    public void error(Exception exc){
        error(exc.toString());
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

