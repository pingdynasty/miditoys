package com.pingdynasty.midi;

import javax.sound.midi.*;

//       ShortMessage.NOTE_OFF
//       ShortMessage.NOTE_ON
//       ShortMessage.POLY_PRESSURE
//       ShortMessage.CONTROL_CHANGE
//       ShortMessage.PROGRAM_CHANGE
//       ShortMessage.CHANNEL_PRESSURE
//       ShortMessage.PITCH_BEND

public class ReceiverPlayer extends Player {
    private Receiver receiver;
    private int channel = 0;

    public ReceiverPlayer(Receiver receiver){
        this.receiver = receiver;
    }

    public void noteon(int note)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
        receiver.send(msg, -1);
    }

    public void noteoff(int note)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.NOTE_OFF,  channel, note, 0);
        receiver.send(msg, -1);
    }

    public void bend(int degree)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
//         msg.setMessage(ShortMessage.PITCH_BEND,  channel, 0xff00 & degree, 0x00ff & degree);
        msg.setMessage(ShortMessage.PITCH_BEND,  channel, degree, degree);
        receiver.send(msg, -1);
    }

    public void modulate(int degree)
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.CONTROL_CHANGE,  channel, 1, degree);
        receiver.send(msg, -1);
    }

    public void programChange(int bank, int program)
        throws InvalidMidiDataException{
        ShortMessage sm = new ShortMessage( );
        sm.setMessage(ShortMessage.PROGRAM_CHANGE, channel, bank, program);
        receiver.send(sm, -1);
        channel = program;
    }

    public void setChannel(int channel)
        throws InvalidMidiDataException{
        this.channel = channel;
    }

    public void allNotesOff()
        throws InvalidMidiDataException{
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.CONTROL_CHANGE,  channel, 123, 0);
        receiver.send(msg, -1);
    }

    public void close(){
        receiver.close();
    }
}
