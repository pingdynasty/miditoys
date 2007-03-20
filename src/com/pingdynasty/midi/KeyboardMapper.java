package com.pingdynasty.midi;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.event.KeyEvent;

public class KeyboardMapper {
    private Map mappings = new HashMap();
    private int octave = 4;

    public KeyboardMapper(){
    }

    public KeyboardMapper(Properties props){
        map(props);
    }

    public void map(Properties props){
        for(Enumeration e = props.propertyNames(); e.hasMoreElements();){
            String key = (String)e.nextElement();
            String note = props.getProperty(key);
            map(key, Integer.parseInt(note));
        }
    }

    public void map(String keytext, int note){
        mappings.put(keytext, new Integer(note));
    }

    public void changeOctaveUp(){
        ++octave;
    }

    public void changeOctaveDown(){
        --octave;
    }

    public int getNote(int key){
        Integer i = (Integer)mappings.get(KeyEvent.getKeyText(key));
        if(i == null)
            return -1;
        int note = i.intValue() + (octave * 12);
        System.out.println("key "+KeyEvent.getKeyText(key)+" ("+key+"): "+note);
        return note;
    }
}
