package com.pingdynasty.midi;

import javax.sound.midi.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

public class MidiSyncDevice implements MidiDevice {

    private int bpm;

    private static final MidiDevice.Info info = new MidiSyncDeviceInfo();
    static class MidiSyncDeviceInfo extends MidiDevice.Info {
        MidiSyncDeviceInfo(){
            super("Internal MIDI Sync", "pingdynasty", 
                  "Internal MIDI Sync Thread", "v1.0");
        }
    }
    private List devices;

    public int getBPM(){
        return bpm;
    }

    public void setBPM(int bpm){
        if(bpm < 1)
            throw new IllegalArgumentException("BPM must be greater than 0");
        this.bpm = bpm;
        for(int i=0, n=devices.size(); i<n; ++i){
            MidiSync sync = (MidiSync)devices.get(i);
            sync.setBPM(bpm);
        }
    }

    public void close(){}

    public MidiDevice.Info getDeviceInfo(){
        return info;
    }

    public int getMaxReceivers(){
        return 0;
    }

    public int getMaxTransmitters(){
        return -1;
    }

    public long getMicrosecondPosition(){
        return -1;
    }

    public Receiver getReceiver()
        throws MidiUnavailableException{
        throw new MidiUnavailableException();
    }

    public Transmitter getTransmitter(){
        MidiSync sync = new MidiSync(bpm);
        devices.add(sync);
        return sync;
    }

    public List getReceivers(){
        return Collections.EMPTY_LIST;
    }

    public List getTransmitters(){
        List result = new ArrayList();
//         for(int i=0, int n=devices.size(); i<n; ++i){
        Iterator it = devices.iterator();
        while(it.hasNext()){
            MidiSync sync = (MidiSync)it.next();
            if(sync.isOpen())
                result.add(sync);
            else
                it.remove();
        }
        return result;
    }

    public boolean isOpen(){
        return true;
    }

    public void open(){
    }
}