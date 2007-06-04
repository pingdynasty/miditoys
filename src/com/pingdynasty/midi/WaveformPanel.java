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
    private float[] displayData;
    private int	width;
    private int startMark;
    private int endMark;

    private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);

    public WaveformPanel(int width, int height){
        this.width = width;
        Dimension dim = new Dimension(width, height);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        displayData = new float[width];
        startMark = 0;
        endMark = width;
//         setForeground(Color.GREEN);
//         setBackground(Color.BLACK);
    }

    public void setData(byte[] data, AudioFormat format){
        int channels = format.getChannels();
        assert channels == 2;
        int frames = data.length / channels;
        int samplesPerPixel = frames / width;
        float max = 0;
        for(int i=0; i<width; ++i){
            for(int j=0; j<samplesPerPixel; ++j)
//                 displayData[i] = (float)(Math.abs(data[i * samplesPerPixel + j]) / 65536.0f);
                displayData[i] += (float)(Math.abs(data[i * samplesPerPixel + j]));
//             displayData[i] /= samplesPerPixel;
            if(displayData[i] > max)
                max = displayData[i];
        }
//         for(int i=0; i<width; ++i)
//             displayData[i] = (displayData[i] / max ) * dim.height; // normalize values
        for(int i=0; i<width; ++i)
            displayData[i] /=  max; // normalize to range 0.0 - 1.0
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        int height = getHeight();
	g.setColor(Color.gray);
        for(int i=0; i<startMark; i++){
            int value = (int) (displayData[i] * height);
//             int y1 = (nHeight - 2 * value) / 2;
//             int y2 = y1 + 2 * value;
//             g.drawLine(i, y1, i, y2);
            g.drawLine(i, height - value, i, value);
        }
	g.setColor(Color.black);
        for(int i=startMark; i<endMark; i++){
            int value = (int) (displayData[i] * height);
            g.drawLine(i, height - value, i, value);
        }
	g.setColor(Color.gray);
        for(int i=endMark; i<displayData.length; i++){
            int value = (int) (displayData[i] * height);
            g.drawLine(i, height - value, i, value);
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
