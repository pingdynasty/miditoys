package com.pingdynasty.midi;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Locale;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class KeyboardMapper {
    private int[] mappings = new int[1024]; // maps keycodes to MIDI notes
    private int octave = 4;

    public KeyboardMapper(){
        for(int i=0; i<mappings.length; i++)
            mappings[i] = -1;
    }

    public KeyboardMapper(Locale locale){
        this();
        ResourceBundle bundle = 
            ResourceBundle.getBundle("com.pingdynasty.midi.KeyboardMapper", locale);
        map(bundle);
    }

    public KeyboardMapper(Properties props){
        this();
        map(props);
    }

    public void map(ResourceBundle bundle){
        for(Enumeration e = bundle.getKeys(); e.hasMoreElements();){
            String key = (String)e.nextElement();
            int note = NoteParser.getMidiNote(bundle.getString(key));
            int keycode = KeyStroke.getKeyStroke(key).getKeyCode();
            mappings[keycode] = note;
        }
    }

    public void map(Properties props){
        for(Enumeration e = props.propertyNames(); e.hasMoreElements();){
            String key = (String)e.nextElement();
            int note = NoteParser.getMidiNote(props.getProperty(key));
            int keycode = KeyStroke.getKeyStroke(key).getKeyCode();
            mappings[keycode] = note;
        }
    }

    public void changeOctaveUp(){
        if(octave < 9)
            ++octave;
    }

    public void changeOctaveDown(){
        if(octave > -1)
            --octave;
    }

    public int getOctave(){
        return octave;
    }

    public int getNote(int key){
        if(mappings[key] == -1)
            return -1;
        int note = mappings[key] + (octave * 12);
//         System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+"): "+note);
        return note;
    }
}
