package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Locale;

public class MidiKeyboardListener implements KeyListener {
    private Receiver receiver;
    private KeyboardMapper keyboard;
    private boolean noteOn[] = new boolean[127]; // keep track of notes that are on
    private ShortMessage msg; // reusable data item
    private int channel = 0; // Midi channel 1
    private int velocity = 80;

    public MidiKeyboardListener(Receiver receiver, KeyboardMapper keyboard){
        msg = new ShortMessage();
        this.receiver = receiver;
        this.keyboard = keyboard;
    }

    public MidiKeyboardListener(){
        msg = new ShortMessage();
        keyboard = new KeyboardMapper(Locale.getDefault());
        keyboard.setOctave(3);
    }

    public void setReceiver(Receiver receiver){
        this.receiver = receiver;
    }

    public int getChannel(){
        return channel;
    }

    public void setChannel(int channel){
        this.channel = channel;
    }

    public int getVelocity(){
        return velocity;
    }

    public void setVelocity(int velocity){
        this.velocity = velocity;
    }

    public void keyTyped(KeyEvent e){}

    public void keyPressed(KeyEvent e){
        int key = e.getKeyCode();
        try{
            if(key == KeyEvent.VK_UP){
                keyboard.changeOctaveUp();
            }else if(key == KeyEvent.VK_DOWN){
                keyboard.changeOctaveDown();
            }else{
                int note = keyboard.getNote(key);
                if(note >= 0 && !noteOn[note]){
                    try{
                        noteOn[note] = true;
                        noteon(note);
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
            }
        }catch(Exception exc){
            exc.printStackTrace();
        }
    }

    public void keyReleased(KeyEvent e){
        KeyEvent nextPress = (KeyEvent)Toolkit.getDefaultToolkit().
            getSystemEventQueue().
            peekEvent(KeyEvent.KEY_PRESSED);
        if((nextPress == null) ||
            (nextPress.getWhen() != e.getWhen()) ||
            (nextPress.getKeyCode() != e.getKeyCode())) {
            int note = keyboard.getNote(e.getKeyCode());
            if(note >= 0 && noteOn[note]){
                try{
                    noteOn[note] = false;
                    noteoff(note);
                }catch(Exception exc){
                    exc.printStackTrace();
                }
            }
        }
    }

    protected void noteon(int note)
        throws InvalidMidiDataException{
//         System.out.println("note on: "+note);
        msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
        receiver.send(msg, -1);
    }

    protected void noteoff(int note)
        throws InvalidMidiDataException{
//         System.out.println("note on: "+note);
        msg.setMessage(ShortMessage.NOTE_OFF, channel, note, velocity);
        receiver.send(msg, -1);
    }
}