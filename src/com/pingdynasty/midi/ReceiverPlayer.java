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
    protected MidiDevice device;
    protected Receiver receiver;
    protected int channel = 0;
    private ShortMessage msg = new ShortMessage();

    public ReceiverPlayer(MidiDevice device)
        throws MidiUnavailableException{
        this.device = device;
        this.receiver = device.getReceiver();
    }

    public ReceiverPlayer(Receiver receiver){
        this.receiver = receiver;
    }

    public void setReceiver(Receiver receiver){
        this.receiver = receiver;
    }

    /**
     * blocking call to play note - waits for the duration of the note.
     */
    public void play(int note)
        throws InvalidMidiDataException{
        if(device == null){
            super.play(note);
            return;
        }
        long now = device.getMicrosecondPosition();
        if(now < 0){
            super.play(note);
            return;
        }
        // send note on
        msg.setMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
        receiver.send(msg, now);

        // send note off with a delay
        msg.setMessage(ShortMessage.NOTE_OFF,  channel, note, 0);
        receiver.send(msg, now + duration*1000);
    }

    public void noteon(int note)
        throws InvalidMidiDataException{
        msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
        receiver.send(msg, -1);
    }

    public void noteoff(int note)
        throws InvalidMidiDataException{
        msg.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        receiver.send(msg, -1);
    }

    /** bend - the amount of pitch change, as a nonnegative 14-bit value (8192 = no bend) */
    public void bend(int degree)
        throws InvalidMidiDataException{
        msg.setMessage(ShortMessage.PITCH_BEND, channel, degree & 0x7F, (degree >> 7) & 0x7F);
        receiver.send(msg, -1);
    }

    public void modulate(int degree)
        throws InvalidMidiDataException{
        msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, 1, degree);
        receiver.send(msg, -1);
    }

    public void controlChange(int code, int value)
        throws InvalidMidiDataException{
        msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, code, value);
        receiver.send(msg, -1);
    }

    public void programChange(int bank, int program)
        throws InvalidMidiDataException{
        ShortMessage sm = new ShortMessage( );
        sm.setMessage(ShortMessage.PROGRAM_CHANGE, channel, bank, program);
        receiver.send(sm, -1);
    }

    public void setChannel(int channel){
        this.channel = channel;
    }

    public int getChannel(){
        return channel;
    }

    public void allNotesOff()
        throws InvalidMidiDataException{
        msg.setMessage(ShortMessage.CONTROL_CHANGE,  channel, 123, 0);
        receiver.send(msg, -1);
    }

    public void close(){
        receiver.close();
    }
}
