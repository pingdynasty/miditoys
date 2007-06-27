package com.pingdynasty.midi;

import java.awt.*;
import javax.swing.JPanel;
import javax.swing.JFrame;

public class OscillatorPanel extends JPanel {
    private float[] displayData;
    private double scale;

    private final static Color CLIP_LIMIT_COLOR = new Color(0x800000);
    private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);
    // Set the antialiasing to get the right look!
    private final static RenderingHints AALIAS = 
	new RenderingHints(RenderingHints.KEY_ANTIALIASING, 
			   RenderingHints.VALUE_ANTIALIAS_ON);

    public OscillatorPanel(int width){
        displayData = new float[width];
        setScaleFactor(63);
    }

    public int getScaleFactor(){
        return (int)(scale / 2.0d * 127.0d);
    }

    public void setScaleFactor(int scale){
        assert scale > 0;
        assert scale < 128;
        this.scale = scale * 2.0d / 127.0d;
//         this.scale = scale / 127.0d;
    }

    public void setData(double[] data){
        int samplesPerPixel = data.length > displayData.length ?
            data.length / displayData.length : 1;
        for(int i=0; i<displayData.length; ++i){
            for(int j=0; j<samplesPerPixel && i*samplesPerPixel+j < data.length; ++j)
                displayData[i] += data[i*samplesPerPixel+j];
            displayData[i] /= (float)samplesPerPixel; // arithmetic mean
        }
        repaint();
    }

    public void setAndScaleData(double[] data){
        if(data.length == displayData.length){
            for(int i=0; i<displayData.length; ++i)
                displayData[i] = (float)(scale * data[i]);
        }else if(data.length > displayData.length){
            int samplesPerPixel = data.length / displayData.length;
//             for(int i=0; i<displayData.length; ++i){
//                 for(int j=0; j<samplesPerPixel; ++j)
//                     displayData[i] += (float)(scale * data[i*samplesPerPixel+j]);
//                 displayData[i] /= (float)samplesPerPixel; // arithmetic mean
//             }
            for(int i=0; i<displayData.length; ++i)
                displayData[i] = (float)(scale * data[i*samplesPerPixel]);
        }else{
            // displayData.length > data.length
            int pixelsPerSample = displayData.length / data.length;
            for(int i=0; i<data.length; ++i)
                for(int j=0; j<pixelsPerSample; ++j)
                    displayData[i*pixelsPerSample+j] = (float)(scale * data[i]);
        }
        repaint();
    }

    public void setAndNormalizeData(double[] data){
        int samplesPerPixel = data.length > displayData.length ?
            data.length / displayData.length : 1;
        float max = 0;
        float min = 0;
        for(int i=0; i<displayData.length; ++i){
            for(int j=0; j<samplesPerPixel && j < data.length - i*samplesPerPixel; ++j)
                displayData[i] += data[i*samplesPerPixel+j];
//                 displayData[i] += Math.abs(data[i*samplesPerPixel+j]);
            if(displayData[i] > max)
                max = displayData[i];
            if(displayData[i] < min)
                min = displayData[i];
        }
        System.out.println("max/min "+max+"/"+min);
        // normalise data to the range 0.0 to 1.0 inclusive
//         for(int i=0; i<displayData.length; ++i)
//             displayData[i] = (displayData[i] - min) / (max - min);

//                 scaled = ( norm * ( max - min ) ) + min
//                 norm = ( scaled - min ) / ( max - min )

        repaint();
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

// 	if(g instanceof Graphics2D) {
// 	    Graphics2D g2d = (Graphics2D)g;
// 	    g2d.setBackground(getParent().getBackground());
// 	    g2d.addRenderingHints(AALIAS);
// 	}

        int height = getHeight();
        int offset = (getWidth() - displayData.length) / 2;

        // draw frame
        g.setColor(Color.gray);
        g.drawRoundRect(offset, 1, displayData.length, height-2, 10, 10);

        height -= 2;

        // draw clipping limit line
        g.setColor(CLIP_LIMIT_COLOR);
        int clip = (int)(height - (0.9f * height));
        g.drawLine(offset, clip, displayData.length+offset, clip);
        g.setColor(DEFAULT_FOCUS_COLOR);

        int x = offset;
        int y = height - (int)(displayData[0] * height) + 1;
        int sx, sy;
        for(int i=1; i<displayData.length; ++i){
            sx = x;
            sy = y;
            x = i+offset;
            y =  height - (int)(displayData[i] * height) + 1;
            g.drawLine(sx, sy, x, y);
        }
    }
}
