package com.pingdynasty.midp;

import java.io.*;
import javax.microedition.midlet.*;
import javax.microedition.media.*;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

public class HelloMobileCanvas extends GameCanvas implements Runnable {

    private boolean running;
    private Display display;
    private TiledLayer board;
    private LayerManager layout;
    private Sequence[] sequences;
    private Sample[] samples;
    private Cursor cursor;
    private int bank = 0;
    private boolean editmode = true;
    private boolean playmode = false;
    private MixingPlayer mixer;
    private static final int CLEAR_CELL = 64;

    private static final String[] names = new String[]{
        "amen_one.wav",
        "amen_two.wav",
        "amen_three.wav",
        "amen_four.wav",
        "amen_five.wav",
        "amen_six.wav",
        "amen_seven.wav",
        "amen_eight.wav",
        "amen_nine.wav",
        "Bass Pluck Hi Hat.wav",
        "Bass Bend.wav",
        "Bass.wav",
        "Bass Bump.wav",
        "Bass Syncopation.wav",
        "Snare and Bass Pluck.wav",
        "Snare.wav",
        "Snare and Trumpet.wav",
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

    public class Sample {
        private String name;
        private Player player;
//         private byte[] buffer;

        public Sample(String name)
            throws IOException, MediaException {
            this.name = name;
            InputStream is = getInputStream();
            if(name.toLowerCase().endsWith(".wav"))
                player = Manager.createPlayer(is, "audio/x-wav");
//             else if(names[i].endsWith(".mp3"))
//                 player = Manager.createPlayer(is, "audio/x-mp3");
            else
                throw new IOException("unsupported file format: "+name);
            player.prefetch();
        }

//         public byte[] getBuffer(){
//             return buffer;
//         }

        public InputStream getInputStream()
            throws IOException {
            return getClass().getResourceAsStream(name);
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

        public void close(){
            if(player != null)
                player.close();
        }
    }

    public class MixingPlayer {
        private Player player;
        private byte[] buffer;
        private int cellsize; // bytes to a beat (8khz * 0.5s * 2 channels * 16bits / 8bitsperbyte)

        public MixingPlayer(int bpm){
            init(bpm, 8000, 2);
        }

        public void init(int bpm, int samplerate, int channels){
            // period = 1000 / bps == 1000 / (bpm / 60) == 60000 / bpm
            int period = 60000 / bpm;
            cellsize = (samplerate * period * channels * 2) / 1000;
            buffer = new byte[cellsize * 8 + 44];
            int size = cellsize * 8;
            // write wav riff header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            buffer[0] = 0x52; // R
            buffer[1] = 0x49; // I
            buffer[2] = 0x46; // F
            buffer[3] = 0x46; // F
            buffer[4] = (byte)((36 + size) & 0x00ff);
            buffer[5] = (byte)(((36 + size) >> 8) & 0x00ff);
            buffer[6] = (byte)(((36 + size) >> 16) & 0x00ff);
            buffer[7] = (byte)(((36 + size) >> 32) & 0x00ff);
            buffer[8] = 0x57;  // W
            buffer[9] = 0x41;  // A
            buffer[10] = 0x56; // V
            buffer[11] = 0x45; // E
            buffer[12] = 0x66; // f
            buffer[13] = 0x6d; // m
            buffer[14] = 0x74; // t
            buffer[15] = 0x20; // [space]
            buffer[16] = 0x10;
            buffer[17] = 0x00;
            buffer[18] = 0x00;
            buffer[19] = 0x00; // subchunk size = 16 (4 bytes)
            buffer[20] = 0x01;
            buffer[21] = 0x00; // AudioFormat = 1 [PCM] (2 bytes)
            buffer[22] = 0x02;
            buffer[23] = 0x00; // Number of channels = 2 (2 bytes)

            buffer[24] = 0x40;
            buffer[25] = 0x1f;
            buffer[26] = 0x00;
            buffer[27] = 0x00; // samplerate (4 bytes)
            buffer[28] = 0x00;
            buffer[29] = 0x7d;
            buffer[30] = 0x00;
            buffer[31] = 0x00; // byterate (4 bytes)

//             buffer[24] = (byte)(samplerate & 0x00ff);
//             buffer[25] = (byte)((samplerate >> 8) & 0x00ff);
//             buffer[26] = (byte)((samplerate >> 16) & 0x00ff);
//             buffer[27] = (byte)((samplerate >> 32) & 0x00ff); // samplerate
//             buffer[28] = (byte)((samplerate * channels * 2) & 0x00ff);
//             buffer[29] = (byte)(((samplerate * channels * 2) >> 8) & 0x00ff);
//             buffer[30] = (byte)(((samplerate * channels * 2) >> 16) & 0x00ff);
//             buffer[31] = (byte)(((samplerate * channels * 2) >> 32) & 0x00ff); // byterate

            buffer[32] = 0x04;
            buffer[33] = 0x00; // block align = 4 (2 bytes)
            buffer[34] = 0x10;
            buffer[35] = 0x00; // bits per sample = 16 (2 bytes)
            // end of subchunk 1
            buffer[36] = 0x64; // d
            buffer[37] = 0x61; // a
            buffer[38] = 0x74; // t
            buffer[39] = 0x61; // a
            buffer[40] = (byte)(size & 0x00ff);
            buffer[41] = (byte)((size >> 8) & 0x00ff);
            buffer[42] = (byte)((size >> 16) & 0x00ff);
            buffer[43] = (byte)((size >> 32) & 0x00ff);
        }

        public void reset(){
            for(int i=44; i<buffer.length; ++i)
                buffer[i] = 0;
        }

        public void mix(Sequence sequence, boolean mixin)
            throws IOException, MediaException {
            if(player != null)
                player.stop();
            player = null;
            for(int i=0; i<sequence.getLength(); ++i){
                Cell cell = sequence.getCell(i);
                int index = cell.getSampleIndex();
                if(index > -1){
                    InputStream is = getClass().getResourceAsStream(names[index]);
                    is.skip(44); // size of header
                    int val = is.read();
                    int offset = i * cellsize + 44;
                    while(val != -1 && offset < buffer.length){
                        if(mixin)
                            buffer[offset++] += val;
                        else
                            buffer[offset++] -= val;
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

    private class Sequence { // implements Runnable {
//         private final int row;
        private Cell[] cells;
//         private int period = 600;
//         private boolean running;
//         private boolean playing;
        private Cell lastCell;
        private boolean mixed = false;

        public Sequence(int width, int row){
//             this.row = row;
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

        public void mix(){
            mixed = true;
            try{
                mixer.mix(this, true);
            }catch(Exception exc){
                error(exc);
            }
        }

        public void unmix(){
            mixed = false;
            try{
                mixer.mix(this, false);
            }catch(Exception exc){
                error(exc);
            }
        }

        public boolean isMixed(){
            return mixed;
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

        public void setSample(int index){
            sequences[row].getCell(col).setSample(index);
        }

        public void clearSample(){
            sequences[row].getCell(col).clearSample();
        }
    }

    public class Cell {
        private int col, row;
        private Sample sample;

        public Cell(int col, int row){
            this.col = col;
            this.row = row;
        }

        public Sample getSample(){
            return sample;
        }

        public int getSampleIndex(){
            for(int i=0; i<samples.length; ++i)
                if(sample == samples[i])
                    return i;
            return -1;
        }

        public void clearSample(){
            sample = null;
            board.setCell(col, row, CLEAR_CELL);
        }

        public void setSample(int index){
//             assert index < samples.length;
            sample = samples[index];
            board.setCell(col, row, index+1);
        }

        public void play(){
            if(sample != null)
                sample.play();
        }

        public void stop(){
            if(sample != null)
                sample.stop();
        }
    }

    public HelloMobileCanvas(Display display) 
        throws IOException, MediaException {
        super(false); // suppress key events
        board = createBoard();
        samples = createSamples();
        sequences = createSequences(8, 8);
        cursor = createCursor();
        mixer = new MixingPlayer(145);
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

    public Sample[] createSamples(){
        Sample[] samples = new Sample[names.length];
        for(int i=0; i<samples.length; ++i){
            try{
                samples[i] = new Sample(names[i]);
            }catch(IOException exc){
                error(exc);
            }catch(MediaException exc){
                error(exc);
            }
        }
        return samples;
    }

    private TiledLayer createBoard() 
        throws IOException {
        // 176 * 220
        Image image = Image.createImage("/shapes.png");
        TiledLayer tiledLayer = new TiledLayer(8, 8, image, 20, 20);
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
  
    public void run() {
        Graphics g = getGraphics();
        int timeStep = 80;
        int lastRenderTime = 0;
        while(running){
            long start = System.currentTimeMillis();
            render(g);
            renderStatus(g);
//             renderTime(g, lastRenderTime);
            flushGraphics();
            long end = System.currentTimeMillis();
            lastRenderTime = (int)(end - start);
            if(lastRenderTime < timeStep){
                try{ 
                    Thread.sleep(timeStep - lastRenderTime); 
                }catch(InterruptedException ie) { 
                    stop(); 
                }
            }
        }
    }
  
    // set and play the sample at the current cursor position
    private void setSample(int index){
        if(index > -1 && index < samples.length && samples[index] != null){
            samples[index].play();
            cursor.setSample(index);
            cursor.right(); // move one step forward
        }
    }

    private void checkkey(int keycode){
        switch(keycode){
        case KEY_STAR:
            cursor.left();
            break;
        case KEY_NUM0:
            cursor.clearSample();
            cursor.right();
            break;
        case KEY_NUM1:
            setSample(bank * 8);
            break;
        case KEY_NUM2:
            setSample(bank * 8 + 1);
            break;
        case KEY_NUM3:
            setSample(bank * 8 + 2);
            break;
        case KEY_NUM4:
            setSample(bank * 8 + 3);
            break;
        case KEY_NUM5:
            setSample(bank * 8 + 4);
            break;
        case KEY_NUM6:
            setSample(bank * 8 + 5);
            break;
        case KEY_NUM7:
            setSample(bank * 8 + 6);
            break;
        case KEY_NUM8:
            setSample(bank * 8 + 7);
            break;
        case KEY_NUM9:
            if(++bank > samples.length / 8)
                bank = 0;
            break;
//         default:
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
            mixSequence();
            break;
        }
    }

    protected void keyPressed(int keycode){
        if(keycode == KEY_POUND){
            editmode = !editmode;
            if(editmode && playmode)
                togglePlay();
        }
        if(editmode){
            checkkey(keycode);
        }else{
            if(keycode == KEY_STAR)
                togglePlay();
            else
                checkaction(getGameAction(keycode));
        }
        //             if((getKeyStates() & (LEFT_PRESSED | RIGHT_PRESSED | UP_PRESSED | DOWN_PRESSED | FIRE_PRESSED)) == 0)
        //                 checkkey(keycode);
    }

    private void togglePlay(){
        playmode = !playmode;
        if(playmode){
            try{
                mixer.start();
            }catch(Exception exc){
                error(exc);
            }
        }else{
            mixer.stop();
        }
    }

    private void mixSequence(){
        if(playmode)
            togglePlay();
        Sequence sequence = sequences[cursor.getRow()];
        if(sequence.isMixed())
            sequence.unmix();
        else
            sequence.mix();
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
        String status;
        if(editmode)
            status = "edit "+(bank+1);
        else
            status = "play"+(playmode ? " x" : " o");
        Font font = g.getFont();
        int sw = font.stringWidth(status) + 2;
        int sh = font.getHeight();
        g.setColor(0xffffff);
        g.fillRect(w - sw, h - sh, sw, sh);
        g.setColor(0x000000);
        g.drawRect(w - sw, h - sh, sw, sh);
        g.drawString(status, w, h, Graphics.RIGHT | Graphics.BOTTOM);
        // draw stars to the right side of mixed in sequences
        status = "*";
//         sw = font.stringWidth(status) + 2;
        int y = (h - 160) / 2;
        for(int i=0; i<sequences.length; ++i)
            if(sequences[i].isMixed())
                g.drawString(status, w, y + i*20, Graphics.RIGHT | Graphics.TOP);
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
        for(int i=0; i<samples.length; ++i)
            if(samples[i] != null)
                samples[i].close();
//         for(int i=0; i<sequences.length; ++i)
//             sequences[i].finish();
    }

    public void error(Exception exc){
//         exc.printStackTrace();
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

