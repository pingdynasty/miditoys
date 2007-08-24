package com.pingdynasty.midp;

import java.io.*;
import javax.microedition.midlet.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
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
    private Image background;
    private static final int NUMBER_OF_TRACKS = 8;
    private static final int NUMBER_OF_BEATS = 8;
    private static final int SAMPLES_PER_BANK = 8;
    private static final int CLEAR_CELL = 64;
    private static final int LAST_SAMPLE_TRACK_INDEX = 6;

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
        private int bpm;
        private Player sampleplayer;
//         private Player toneplayer;
        private byte[] buffer;
        private int cellsize; // bytes to a beat (8khz * 0.5s * 2 channels * 16bits / 8bitsperbyte)

        public MixingPlayer(int bpm)
            throws IOException, MediaException{
            init(bpm, 8000, 2);
//             toneplayer = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR); 
//             toneplayer.realize(); 
        }

        public void init(int bpm, int samplerate, int channels)
            throws IOException, MediaException{
            if(sampleplayer != null)
                sampleplayer.close();
            this.bpm = bpm;
            // period = 1000 / bps == 1000 / (bpm / 60) == 60000 / bpm
            int period = 60000 / bpm;
            cellsize = (samplerate * period * channels * 2) / 1000;
            buffer = new byte[cellsize * NUMBER_OF_BEATS + 44];
            int size = cellsize * NUMBER_OF_BEATS;
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
            sampleplayer = Manager.createPlayer(new ByteArrayInputStream(buffer), "audio/x-wav");
            sampleplayer.setLoopCount(-1);
        }

        public void reset(){
            for(int i=44; i<buffer.length; ++i)
                buffer[i] = 0;
        }

        protected void mix(Sequence sequence, boolean mixin)
            throws IOException, MediaException {
            for(int i=0; i<sequence.getLength(); ++i){
                Cell cell = sequence.getCell(i);
                int index = cell.getValue();
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
        }

        public void remix()
            throws IOException, MediaException {
            for(int i=0; i<sequences.length; ++i)
                if(sequences[i].isMixed()){
                    if(sequences[i].isSampleSequence()){
                        try{
                            mix(sequences[i], true);
                        }catch(Exception exc){
                            error(exc);
                        }
                    }else if(sequences[i].isToneSequence()){
//                         ToneControl tc = (ToneControl)toneplayer.getControl("ToneControl"); 
//                         tc.setSequence(sequences[i].getToneSequence(bpm)); 
                    }
                }
            if(sampleplayer != null)
                sampleplayer.close();
            sampleplayer = Manager.createPlayer(new ByteArrayInputStream(buffer), "audio/x-wav");
            sampleplayer.setLoopCount(-1);
        }

        public void play(){
            stop();
            reset();
            try{
                remix();
            }catch(Exception exc){
                error(exc);
            }
            start();
        }

        public void start(){
            try{
                if(sampleplayer != null)
                    sampleplayer.start();
//                 if(toneplayer != null)
//                     toneplayer.start();
            }catch(MediaException exc){
                error(exc);
            }
        }

        public void stop(){
            try{
                if(sampleplayer != null)
                    sampleplayer.stop();
//                 if(toneplayer != null)
//                     toneplayer.stop();
            }catch(MediaException exc){
                error(exc);
            }
        }
    }

    private class Sequence {
        private Cell[] cells;
        private boolean mixed = true;

        public Sequence(int width, int row){
            cells = new Cell[width];
            for(int i=0; i<cells.length; ++i){
                cells[i] = new Cell(i, row, row > LAST_SAMPLE_TRACK_INDEX ? Cell.TONE_CELL_TYPE : Cell.SAMPLE_CELL_TYPE);
            }
        }

        public boolean isSampleSequence(){
            return cells[0].getCellType() == Cell.SAMPLE_CELL_TYPE;
        }

        public boolean isToneSequence(){
            return cells[0].getCellType() == Cell.TONE_CELL_TYPE;
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
        }

        public void unmix(){
            mixed = false;
        }

        public boolean isMixed(){
            return mixed;
        }

        public byte[] getToneSequence(int bpm){
            if(!isToneSequence())
                return null;
            byte tempo = (byte)(bpm / 4);
//             byte notelength = 8; // eight note
//             byte notelength = 4; // quarter note
            byte notelength = 2; // half note
            byte[] sequence = new byte[cells.length * 2 + 4];
            sequence[0] = ToneControl.VERSION;
            sequence[1] = 1;
            sequence[2] = ToneControl.TEMPO;
            sequence[3] = tempo;
            for(int i=0; i<cells.length; ++i){
                sequence[i*2+4] = (byte)cells[i].getValue();
                sequence[i*2+5] = notelength;
            }
            return sequence;
        }
    }

    private class Cursor extends Sprite {
        int col, row; // [0-7]
        public static final int MIN_ROW = 0;
        public static final int MAX_ROW = NUMBER_OF_TRACKS - 1;
        public static final int MIN_COL = 0;
        public static final int MAX_COL = NUMBER_OF_BEATS - 1;

        public Cursor(Image image){
            super(image, 20, 20);
            col = MIN_ROW;
            row = MIN_COL;
        }

        public void toggleCursor(){
            if(editmode)
                setFrame(0);
            else
                setFrame(1);
        }

        public void left(){
            if(--col < MIN_COL){
                col = MAX_COL;
                up();
            }
            setPosition(col*20, row*20);
        }

        public void right(){
            if(++col > MAX_COL){
                col = MIN_COL;
                down();
            }
            setPosition(col*20, row*20);
        }

        public void up(){
            if(--row < MIN_ROW)
                row = MAX_ROW;
            setPosition(col*20, row*20);
        }

        public void down(){
            if(++row > MAX_ROW)
                row = MIN_ROW;
            setPosition(col*20, row*20);
        }

        public int getColumn(){
            return col;
        }

        public int getRow(){
            return row;
        }

        public Sequence getSequence(){
            return sequences[row];
        }

        public void setCellValue(int index){
            sequences[row].getCell(col).setValue(index);
        }

        public void clearCell(){
            sequences[row].getCell(col).clear();
        }
    }

    public class Cell {
        private int col, row;
        private int value = -1;
        private final int type;
        public static final int SAMPLE_CELL_TYPE = 1;
        public static final int TONE_CELL_TYPE = 2;
        public static final int MIDI_CELL_TYPE = 3;

        public Cell(int col, int row, int type){
            this.col = col;
            this.row = row;
            this.type = type;
        }

        public int getValue(){
            return value;
        }

        public int getCellType(){
            return type;
        }

        public void clear(){
            value = -1;
            board.setCell(col, row, CLEAR_CELL);
        }

        public void setValue(int value){
//             assert value < samples.length;
            this.value = value;
            board.setCell(col, row, value+1);
        }

        public void play(){
            if(value == -1)
                return;
            switch(type){
            case SAMPLE_CELL_TYPE:
                if(samples[value] != null)
                    samples[value].play();
                break;
            case TONE_CELL_TYPE:
//                 try{
//                     Manager.playTone(value + 48, 400, 90);
//                 }catch(MediaException exc){
//                     error(exc);
//                 }
//                 break;
            }
        }

        public Sample getSample(){
            if(type == SAMPLE_CELL_TYPE && value != -1)
                return samples[value];
            else
                return null;
        }
    }

    public HelloMobileCanvas(Display display) 
        throws IOException, MediaException {
        super(false); // suppress key events
        background = Image.createImage("/background160x160.png");
        board = createBoard();
        samples = createSamples();
        sequences = createSequences(NUMBER_OF_BEATS, NUMBER_OF_TRACKS);
        cursor = createCursor();
        mixer = new MixingPlayer(145);
        layout = new LayerManager();
        layout.append(cursor);
        layout.append(board);

        layout.setViewWindow(0, 0, NUMBER_OF_BEATS*20, NUMBER_OF_TRACKS*20);
//         layout.setViewWindow(0, 0, getWidth(), getHeight());
        this.display = display;
    }

    public Cursor createCursor()
        throws IOException {
        Image image = Image.createImage("/cursor.png");
        Cursor cursor = new Cursor(image);
        return cursor;
    }

    public Sequence[] createSequences(int cols, int rows){
        Sequence[] sequences = new Sequence[rows];
        for(int i=0; i<sequences.length; ++i)
            sequences[i] = new Sequence(cols, i);
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
        Image image = Image.createImage("/samples.png");
        TiledLayer tiledLayer = new TiledLayer(NUMBER_OF_BEATS, NUMBER_OF_TRACKS, image, 20, 20);
        for(int i=0; i<NUMBER_OF_BEATS; ++i)
            for(int j=0; j<NUMBER_OF_TRACKS; ++j)
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

    private void play(int index){
//         if(cursor.getSequence().isToneSequence()){
//         if(cursor.getRow() > LAST_SAMPLE_TRACK_INDEX){
//             try{
//                 Manager.playTone(index + 48, 400, 90);
//             }catch(MediaException exc){
//                 error(exc);
//             }
//             cursor.setCellValue(index);
//             cursor.right(); // move one step forward
//         }else{
            if(index < samples.length && samples[index] != null){
                samples[index].play();
                cursor.setCellValue(index);
                cursor.right(); // move one step forward
            }
//         }
    }

    private void checkkey(int keycode){
        switch(keycode){
        case KEY_STAR:
            cursor.left();
            break;
        case KEY_NUM0:
            cursor.clearCell();
            cursor.right();
            break;
        case KEY_NUM1:
            play(bank * SAMPLES_PER_BANK);
            break;
        case KEY_NUM2:
            play(bank * SAMPLES_PER_BANK + 1);
            break;
        case KEY_NUM3:
            play(bank * SAMPLES_PER_BANK + 2);
            break;
        case KEY_NUM4:
            play(bank * SAMPLES_PER_BANK + 3);
            break;
        case KEY_NUM5:
            play(bank * SAMPLES_PER_BANK + 4);
            break;
        case KEY_NUM6:
            play(bank * SAMPLES_PER_BANK + 5);
            break;
        case KEY_NUM7:
            play(bank * SAMPLES_PER_BANK + 6);
            break;
        case KEY_NUM8:
            play(bank * SAMPLES_PER_BANK + 7);
            break;
        case KEY_NUM9:
            if(++bank > samples.length / SAMPLES_PER_BANK)
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
            cursor.toggleCursor();
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
                mixer.play();
//                 mixer.start();
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
        int x = (w - NUMBER_OF_BEATS*20) / 2;
        int y = (h - NUMBER_OF_TRACKS*20) / 2;
        g.drawImage(background, x, y, Graphics.TOP | Graphics.LEFT);
        layout.paint(g, x, y);
        g.setColor(0x000000);
        g.drawRect(x, y, NUMBER_OF_BEATS*20, NUMBER_OF_TRACKS*20);
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
        int y = (h - NUMBER_OF_TRACKS*20) / 2;
        for(int i=0; i<sequences.length; ++i)
            if(sequences[i].isMixed())
                g.drawString(status, w, y + i*20, Graphics.RIGHT | Graphics.TOP);
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

