package com.pingdynasty.phosphoros;
//
// LiveCam.java
// LiveCam
//
// Created by Jochen Broz on 19.02.05.
// Copyright (c) 2005 Jochen Broz. All rights reserved.
//

import java.util.*;


// for the OpenGL stuff
// import net.java.games.jogl.*;



// For accessing the sequence grabber
import quicktime.*;
import quicktime.std.sg.*;
import quicktime.std.*;
import quicktime.qd.*;
import quicktime.util.*;
import quicktime.io.*;
import quicktime.std.image.*;



// For creation of awt images
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;


public class LiveCam {

    int frameCount = 0;

    // Data concerning the sequence grabber, its gWorld and its image size
    SequenceGrabber sg;
    QDRect cameraImageSize;
    QDGraphics gWorld;


    // Data concerning building awt images from cameras gWorld
    public int[] pixelData;
    BufferedImage image;


    /**
     * flag, indicating that all capture and display tasks should continue or not
     */
    boolean cameraActive = true;

    /**
     * Creates the LiveCam object.
     */
    public LiveCam() throws Exception{
        initSequenceGrabber();
        initBufferedImage();
    }

    /**
     * Initializes the SequenceGrabber. Gets it's source video bounds, creates a gWorld with that size.
     * Configures the video channel for grabbing, previewing and playing during recording.
     */
    private void initSequenceGrabber() throws Exception{
        sg = new SequenceGrabber();
        SGVideoChannel vc = new SGVideoChannel(sg);
        cameraImageSize = vc.getSrcVideoBounds();

        gWorld =new QDGraphics(cameraImageSize);
        sg.setGWorld(gWorld, null);

        vc.setBounds(cameraImageSize);
        vc.setUsage(quicktime.std.StdQTConstants.seqGrabRecord |
                    quicktime.std.StdQTConstants.seqGrabPreview |
                    quicktime.std.StdQTConstants.seqGrabPlayDuringRecord);
        vc.setFrameRate(0);
        vc.setCompressorType( quicktime.std.StdQTConstants.kComponentVideoCodecType);
    }


    /**
     * This initializes the buffered image. First if determines the size of the data.
     * Finds the number of ints per row, sets up the int array, where we can put the
     * pixel data in. A DataBuffer is defined, using that int array. Based on this DataBuffer
     * the BufferedImage is defined.
     * The BufferedImage definition is not needed when the OpenGL view is used. There we only need
     * the int-array pixelData. But I do not expect that defining the Buffered image has negative influence
     * on OpenGLs performance.
     */
    private void initBufferedImage() throws Exception{

        int size = gWorld.getPixMap().getPixelData().getSize();
        int intsPerRow = gWorld.getPixMap().getPixelData().getRowBytes()/4;

        size = intsPerRow*cameraImageSize.getHeight();
        pixelData = new int[size];
        DataBuffer db = new DataBufferInt(pixelData, size);

        ColorModel colorModel = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);
        int[] masks= {0x00ff0000, 0x0000ff00, 0x000000ff};
        WritableRaster raster = Raster.createPackedRaster(db, cameraImageSize.getWidth(), cameraImageSize.getHeight(), intsPerRow, masks, null);
        image = new BufferedImage(colorModel, raster, false, null);

    }


    /**
     * This is a bit tricky. We do not start Previewing, but recording. By setting the output to a
     * dummy file (which will never be created (hope so)) with the
     * quicktime.std.StdQTConstants.seqGrabDontMakeMovie flag set. This seems to be equivalent to
     * preview mode with the advantage, that it refreshes correctly.
     */
    private void startPreviewing() throws Exception{
        QTFile movieFile = new QTFile(new java.io.File("NoFile"));
        sg.setDataOutput( null, quicktime.std.StdQTConstants.seqGrabDontMakeMovie);
        sg.prepare(true, true);
        sg.startRecord();

        // setting up a thread, to idle the sequence grabber
        Runnable idleCamera = new Runnable(){
                int taskingDelay = 25;
                public void run(){
                    try{
                        while(cameraActive){
                            Thread.sleep(taskingDelay);
                            synchronized(sg){
                                sg.idleMore();
                                sg.update(null);
                            }
                        }
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            };
        (new Thread(idleCamera)).start();
    }


    /**
     * This creates a Panel, which displays the buffered image using awt. A Thread is started, copying
     * the pixel data from the sequence grabbers gWorld to the data buffer of the BufferedImage. Then
     * the image is repainted.
     */
    public Component buildSwingCameraView(){
        /**
         * Used for the threadding image update
         */
        final Component ret = new Component(){
                public void paint(Graphics g){
                    super.paint(g);
                    g.drawImage(image, 0, 0, this);
                    frameCount++;
                };
            };

        Runnable imageUpdate = new Runnable(){
                int taskingDelay = 10;
                public void run(){
                    try{
                        while(cameraActive){
                            synchronized(sg){
                                gWorld.getPixMap().getPixelData().copyToArray(0, pixelData, 0, pixelData.length);
                                ret.repaint();
                            }
                            Thread.sleep(taskingDelay);
                        }
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            };
        (new Thread(imageUpdate)).start();
        return ret;
    }


    /**
     * This creates a Panel, which displays the camera image using OpenGL
     */
//     public Component buildOpenGLCameraView(){
//         GLEventListener jogler = new GLEventListener(){
//                 final int WIDTH = gWorld.getPixMap().getPixelData().getRowBytes()/4;
//                 final int HEIGHT = cameraImageSize.getHeight();

//                 public void init(GLDrawable drawable){
//                     GL gl = drawable.getGL();
//                     gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//                     gl.glShadeModel(GL.GL_FLAT);
//                 }

//                 public void display(GLDrawable drawable){
//                     GL gl = drawable.getGL();
//                     GLU glu = drawable.getGLU();
//                     gl.glClear(GL.GL_COLOR_BUFFER_BIT| GL.GL_DEPTH_BUFFER_BIT);
//                     gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
//                     gWorld.getPixMap().getPixelData().copyToArray(0, pixelData, 0, WIDTH*HEIGHT);
//                     gl.glDrawPixels(WIDTH, HEIGHT, gl.GL_BGRA, gl.GL_UNSIGNED_INT_8_8_8_8_REV, pixelData);
//                     frameCount++;
//                 }

//                 public void reshape(GLDrawable drawable, int i, int x, int width, int height){
//                     GL gl = drawable.getGL();
//                     GLU glu = drawable.getGLU();
//                     gl.glViewport(0, 0, WIDTH, HEIGHT);
//                     gl.glMatrixMode(GL.GL_PROJECTION);
//                     gl.glLoadIdentity();
//                     glu.gluOrtho2D(0.0, (double) WIDTH, 0.0, (double) HEIGHT);
//                     gl.glMatrixMode(GL.GL_MODELVIEW);
//                     gl.glLoadIdentity();
//                 }

//                 public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged){}
//             };

//         GLCapabilities caps = new GLCapabilities();
//         GLCanvas canvas = GLDrawableFactory.getFactory().createGLCanvas(caps);
//         canvas.addGLEventListener(jogler);
//         Animator animator = new Animator(canvas);
//         animator.start();
//         return canvas;
//     }

    public QDRect getImageSize(){
        return cameraImageSize;
    }

    public int getFrameCount(){
        return frameCount;
    }


    public static void main (String args[]) {
        try{
            QTSession.open();

            LiveCam myCam = new LiveCam();
            JFrame cameraFrame = new JFrame("LiveCam");
            myCam.startPreviewing();

            // Switch between the two lines to select OpenGL or awt display:
            Component imagePanel = myCam.buildSwingCameraView();
//             Component imagePanel = myCam.buildOpenGLCameraView();

            cameraFrame.add(imagePanel);
            cameraFrame.setBounds(100, 100, myCam.getImageSize().getWidth(), myCam.getImageSize().getHeight());
            cameraFrame.setSize(800, 600);
            cameraFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            cameraFrame.show();

            // Do some frame counting
            System.out.println("Counting frames:");
            Thread.sleep(5000);
            int fc = myCam.getFrameCount();
            Thread.sleep(10000);
            System.out.println("Frame rate:"+0.1*(myCam.getFrameCount()-fc));

        }catch(Exception ex){
            ex.printStackTrace();
            QTSession.close();
        }

    }
}
