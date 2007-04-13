package com.pingdynasty.midi.bcontrol;

import java.util.List;
import java.nio.ByteBuffer;
import javax.sound.midi.*;

public class BCRSysexMessage extends SysexMessage {

    int index;

    public BCRSysexMessage(int index){
        super();
        this.index = index;
    }

    protected static final void putInteger(ByteBuffer buf, int num){    
//     In order to avoid an accidental 'F7' byte inside a sysex message,
//     Behringer has decided the high bit on any byte cannot be used.  This means
//     we have to do a special kind of counting when using 2 bytes to store the
//     packet count index.  Here's how they count:
//     00 00 -> 00 7F  (    0 to   127)
//     01 00 -> 01 7F  (  128 to   255)
//     ...
//     7F 00 -> 7F 7F  (16256 to 16383)
        if(num < 0 || num > 16383)
            throw new IllegalArgumentException("integer out of range: "+num);
        byte msb = (byte)((num & 0xff80) >>> 7);
        byte lsb = (byte)(num & 0x007f);
        buf.put(msb).put(lsb);
        String str = Integer.toString(num, 2);
    }

    public void setMessage(String message)
        throws InvalidMidiDataException {
        ByteBuffer buf = ByteBuffer.allocate(message.length() + 8);
        byte[] data = new byte[]{
            0x00, 0x20, 0x32, // Behringer manufacturer's code
            0x00, 0x15, 0x20, // header, space
        };
        buf.put(data);
        putInteger(buf, index);
        for(int i=0; i<message.length(); ++i)
            buf.put((byte)message.charAt(i));
        data = buf.array();
        super.setMessage(SysexMessage.SYSTEM_EXCLUSIVE, data, data.length);
//             (byte)0xf0, // status byte (System Exclusive Message)
//         return new SysexMessage(data);
//         sysex.setMessage(SysexMessage.SYSTEM_EXCLUSIVE, data, data.length);
    }

    public String print(){
        byte[] data = super.getData();
        StringBuffer buf = new StringBuffer();
        for(int i=0; i<data.length; ++i)
            buf.append(Integer.toHexString(data[i])).append(' ');
        return buf.toString();
    }

    public String dump(){
        byte[] data = super.getData();
        StringBuffer buf = new StringBuffer();
        for(int i=0; i<data.length; ++i)
            buf.append((char)data[i]);
        return buf.toString();
    }

    public static BCRSysexMessage createMessage(int index, String data)
        throws Exception {
        BCRSysexMessage sysex = new BCRSysexMessage(index);
        sysex.setMessage(data);
        return sysex;
    }

    public static void createMessage(List messages, String data)
        throws InvalidMidiDataException {
        BCRSysexMessage sysex = new BCRSysexMessage(messages.size());
        sysex.setMessage(data);
        messages.add(sysex);
    }

    public static final void main(String[] args)
        throws Exception {
        String data = args.length > 0 ? args[0] :
            "  .easypar CC 1 1 0 127 absolute";
        BCRSysexMessage msg = new BCRSysexMessage(16253);
        msg.setMessage(data);
        System.out.println(msg.dump());
        System.err.println(msg.print());
//         ByteBuffer buf = ByteBuffer.allocate(10000);
//         for(int i=0; i<200; i+=5)
//             putInteger(buf, i);
//         for(int i=16256; i<16383; i+=5)
//             putInteger(buf, i);
//         System.out.println("hex "+Integer.toHexString(0x7f00)+" "+Integer.toHexString(0x007f));
//         System.out.println("bin "+Integer.toBinaryString(0x7f00)+" "+Integer.toBinaryString(0x007f));
//         System.out.println("bin "+Integer.toBinaryString(0xff80)+" "+Integer.toBinaryString(0x007f));
    }
}