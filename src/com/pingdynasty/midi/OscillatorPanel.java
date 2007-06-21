package com.pingdynasty.midi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.JFrame;

class OscillatorPanel extends JPanel {
    private float[] displayData;

    private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);

    public OscillatorPanel(int width, int height){
//         this.width = width;
        Dimension dim = new Dimension(width, height);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        displayData = new float[width];
    }

    public void setData(double[] data){
        int samplesPerPixel = data.length / displayData.length;
        float max = 0;
        float min = 0;
        for(int i=0; i<displayData.length; ++i){
            for(int j=0; j<samplesPerPixel; ++j)
                displayData[i] += data[i*samplesPerPixel+j];
//                 displayData[i] += Math.abs(data[i*samplesPerPixel+j]);
//             displayData[i] /= samplesPerPixel; // arithmetic mean
            if(displayData[i] > max)
                max = displayData[i];
            if(displayData[i] < min)
                min = displayData[i];
        }

        // normalise data to the range 0.0 to 1.0 inclusive
        for(int i=0; i<displayData.length; ++i)
            displayData[i] = (displayData[i] - min) / (max - min);

//                 scaled = ( norm * ( max - min ) ) + min
//                 norm = ( scaled - min ) / ( max - min )

        repaint();
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        g.setColor(Color.gray);
        g.setColor(DEFAULT_FOCUS_COLOR);

        int height = getHeight();
        int x = 0;
        int y = height - (int)(displayData[0] * height);
        int sx, sy;
        for(int i=1; i<displayData.length; ++i){
            sx = x;
            sy = y;
            x = i;
            y =  height - (int)(displayData[i] * height);
            g.drawLine(sx, sy, x, y);
        }
    }
}
