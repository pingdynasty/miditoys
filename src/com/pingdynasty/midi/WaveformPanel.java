package com.pingdynasty.midi;

import javax.sound.sampled.*;
import java.io.File;
import java.io.ByteArrayOutputStream;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.JFrame;

public class WaveformPanel extends JPanel {
    private float[][] displayData;
    private int	width;
    private int startMark;
    private int endMark;
    private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);


	private static final float twoPower7=128.0f;
	private static final float twoPower15=32768.0f;
	private static final float twoPower23=8388608.0f;
	private static final float twoPower31=2147483648.0f;

	private static final float invTwoPower7=1/twoPower7;
	private static final float invTwoPower15=1/twoPower15;
	private static final float invTwoPower23=1/twoPower23;
	private static final float invTwoPower31=1/twoPower31;

    public WaveformPanel(int width, int height){
        this.width = width;
        Dimension dim = new Dimension(width, height);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        startMark = 0;
        endMark = 0;
        displayData = new float[2][width];
    }

    public void setData(byte[] data, AudioFormat format){
        int channels = format.getChannels();
        displayData = new float[channels][width];
        int frames = data.length / channels;
        int samplesPerPixel = frames / width / channels;
        int bytesPerSample = format.getFrameSize() / channels;
        boolean bigEndian = format.isBigEndian();
        float max = 0;
        for(int i=0; i<width; ++i){
            for(int j=0; j<samplesPerPixel; ++j){
                for(int k=0; k<channels; ++k){
                    int pos = 
                        i*samplesPerPixel*channels*bytesPerSample+
                        j*channels*bytesPerSample+
                        k*bytesPerSample;
                    switch(bytesPerSample){
                    case 1:
                        displayData[k][i] += (float)Math.abs(data[pos]);
                        break;
                    case 2:
                        displayData[k][i] += bigEndian ?
                            (float)Math.abs((data[pos]<<8) | (data[pos+1]&0xFF)) :
                            (float)Math.abs((data[pos+1]<<8) | (data[pos]&0xFF));
                        break;
                    case 3:
                        displayData[k][i] += bigEndian ?
                            (float)Math.abs((data[pos]<<16) 
                                            | ((data[pos+1]&0xFF)<<8)
                                            | (data[pos+2]&0xFF)) :
                            (float)Math.abs((data[pos+2]<<16) 
                                            | ((data[pos+1]&0xFF)<<8)
                                            | (data[pos]&0xFF));
                            break;
                    case 4:
                        displayData[k][i] += bigEndian ?
                            (float)Math.abs((data[pos]<<24) 
                                            | ((data[pos+1]&0xFF)<<16)
                                            | ((data[pos+2]&0xFF)<<8)
                                            | (data[pos+3]&0xFF)) :
                            (float)Math.abs((data[pos+3]<<24) 
                                            | ((data[pos+2]&0xFF)<<16)
                                            | ((data[pos+1]&0xFF)<<8)
                                            | (data[pos]&0xFF));
                        break;
                    default:
                        throw new IllegalArgumentException("invalid format: "+format);
                    }
                }
                for(int k=0; k<channels; ++k){
                    if(displayData[k][i] > max)
                        max = displayData[k][i];
                }
            }
        }
        for(int k=0; k<channels; ++k)
            for(int i=0; i<width; ++i)
                displayData[k][i] =  displayData[k][i] / max;
        // normalize to range 0.0 - 1.0
        repaint();
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        g.setColor(Color.gray);
        paint(g, 0, startMark);
        g.setColor(DEFAULT_FOCUS_COLOR);
        paint(g, startMark, endMark);
        g.setColor(Color.gray);
        paint(g, endMark, width);
    }

    public void paint(Graphics g, int from, int to){
        int center = getHeight() / (displayData.length * 2);
        int offset = 0;
        for(int k=0; k<displayData.length; ++k){
            for(int i=from; i<to; i++){
                int value = (int)(displayData[k][i] * center);
                g.drawLine(i, center + offset - value, i, center + offset + value);
            }
            offset += center * 2;
        }
    }

    public int getWidth(){
        return width;
    }

    public void setStartMark(int mark){
        this.startMark = mark;
        repaint();
    }

    public void setMarkLength(int mark){
        this.endMark = mark + startMark;
        if(endMark > displayData.length)
            endMark = displayData.length;
        repaint();
    }

    public void setEndMark(int mark){
        this.endMark = mark;
        repaint();
    }

    public void setMark(BeatSlicer.Slice slice){
        startMark = slice.getStart() * width / 127;
        endMark = slice.getLength() * width / 127 + startMark;
        if(endMark > width)
            endMark = width;
        repaint();
    }

    public static void main(String[] args) 
        throws Exception {

        if(args.length != 1)
            throw new Exception("usage: WaveformPanel audiofile");

        WaveformPanel panel = new WaveformPanel(500, 100);

        File file = new File(args[0]);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        if(audioInputStream == null)
            throw new IllegalArgumentException("invalid sound file "+file.getName());
        AudioFormat format = audioInputStream.getFormat();
        System.out.println("audio format: "+format);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
//         int offset = 0;
        for(int len = audioInputStream.read(buf); len > 0;
            len = audioInputStream.read(buf)){
            outputStream.write(buf, 0, len);
//             offset += len;
        }
        byte[] data = outputStream.toByteArray();
        panel.setData(data, format);

        JFrame frame = new JFrame("waveform");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500 + 20, 100 + 20);
        frame.setContentPane(panel);
        frame.setVisible(true);
    }

}
