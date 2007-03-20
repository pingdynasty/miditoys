package com.pingdynasty.midi;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class KeyboardMapper {
    private int[] mappings = new int[1024];
    private int octave = 4;

    public KeyboardMapper(){
        for(int i=0; i<mappings.length; i++)
            mappings[i] = -1;
    }

    public KeyboardMapper(Properties props){
        this();
        map(props);
    }

    public void map(Properties props){
        for(Enumeration e = props.propertyNames(); e.hasMoreElements();){
            String key = (String)e.nextElement();
            int note = Integer.parseInt(props.getProperty(key));
            int keycode = KeyStroke.getKeyStroke(key).getKeyCode();
            mappings[keycode] = note;
        }
    }

    public void changeOctaveUp(){
        ++octave;
    }

    public void changeOctaveDown(){
        --octave;
    }

    public int getNote(int key){
        if(mappings[key] == -1)
            return -1;
        int note = mappings[key] + (octave * 12);
        System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+"): "+note);
        return note;
    }
}
