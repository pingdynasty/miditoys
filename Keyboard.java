import javax.sound.midi.*;
import java.awt.Toolkit;
import java.awt.event.*;
import javax.swing.*;

//       ShortMessage.NOTE_OFF
//       ShortMessage.NOTE_ON
//       ShortMessage.POLY_PRESSURE
//       ShortMessage.CONTROL_CHANGE
//       ShortMessage.PROGRAM_CHANGE
//       ShortMessage.CHANNEL_PRESSURE
//       ShortMessage.PITCH_BEND

abstract class Player {
    protected int velocity;
    public void setVelocity(int velocity){
        this.velocity = velocity;
    }
    public abstract void noteon(int note)
        throws InvalidMidiDataException;
    public abstract void noteoff(int note)
        throws InvalidMidiDataException;
    public abstract void bend(int degree)
        throws InvalidMidiDataException;
//     public abstract void setInstrument(int instrument)
//         throws InvalidMidiDataException;
    public abstract void programChange(int bank, int program)
        throws InvalidMidiDataException;
}

class ReceiverPlayer extends Player {
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
        msg.setMessage(ShortMessage.PITCH_BEND,  channel, degree, degree);
//         msg.setMessage(ShortMessage.CONTROL_CHANGE,  1, degree, degree);
        receiver.send(msg, -1);
    }

//     public void setInstrument(int instrument)
//         throws InvalidMidiDataException{
//         ShortMessage sm = new ShortMessage( );
//         sm.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
//         receiver.send(sm, -1);
//     }

    public void programChange(int bank, int program)
        throws InvalidMidiDataException{
//         ShortMessage sm = new ShortMessage( );
//         sm.setMessage(ShortMessage.CONTROL_CHANGE, 0, program, bank);
//         sm.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, bank);
//         receiver.send(sm, -1);
        channel = program;
    }
}

class ChannelPlayer extends Player {
    private Synthesizer synth;
    private MidiChannel channel;

    public ChannelPlayer(Synthesizer synth){
        this.synth = synth;
        channel = synth.getChannels()[0];
    }

    public void noteon(int note)
        throws InvalidMidiDataException{
        channel.noteOn(note, velocity);
    }

    public void noteoff(int note)
        throws InvalidMidiDataException{
        channel.noteOff(note);
    }

    public void bend(int degree)
        throws InvalidMidiDataException{
        channel.setPitchBend(degree);
//         channel.controlChange(1, degree);
    }

    public void programChange(int bank, int program)
        throws InvalidMidiDataException{
        channel.programChange(bank, program);
    }

//     public void setInstrument(int instrument)
//         throws InvalidMidiDataException{
//         channel = synth.getChannels()[instrument];
//     }
}

/**
 * this program the MIDI percussion channel with a Swing window.  It monitors
 * keystrokes and mouse motion in the window and uses them to create music.
 * Keycodes between 35 and 81, inclusive, generate different percussive sounds.
 * See the VK_ constants in java.awt.event.KeyEvent, or just experiment.
 * Mouse position controls volume: move the mouse to the right of the window
 * to increase the volume.
 */
public class Keyboard extends JFrame {
    final Player player; // either play through a Receiver or a MidiChannel
    private boolean noteOn[] = new boolean[1024]; // keep track of notes that are on
    private static int bank = 0;
    private static int program = 0;

    public static void main(String[  ] args) 
        throws MidiUnavailableException, Exception {
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for(int i=0; i<devices.length; ++i){
            System.out.print("MIDI device "+i+": ");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Receiver)
                System.out.print("receiver\t");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Transmitter)
                System.out.print("transmitter\t");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Synthesizer)
                System.out.print("synthesizer\t");
            if(MidiSystem.getMidiDevice(devices[i]) instanceof Sequencer)
                System.out.print("sequencer");
            System.out.println();
            System.out.println("\t"+devices[i].getName()+" ("+
                               devices[i].getDescription()+")\t");
        }

        System.out.println("choose synthesizer or receiver device:");
        int i = System.in.read() - 48;
        System.out.println(devices[i].getName());
        System.in.read();// eat the eol
//         System.out.println("choose instrument: 0-9");
//         instrument = System.in.read() - 48;

        MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
        device.open();
        Player player;
        if(device instanceof Synthesizer){
            Synthesizer synthesizer = (Synthesizer)device;
            player = new ChannelPlayer(synthesizer);
//             frame = new Keyboard(player);
            
//             // select channel
//             Receiver receiver = device.getReceiver();
//             ShortMessage sm = new ShortMessage( );
//             sm.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
//             receiver.send(sm, -1);
//             frame = new Keyboard(new ReceiverPlayer(receiver));

//             MidiChannel channel = synthesizer.getChannels()[instrument];
//             frame = new Keyboard(new ChannelPlayer(channel));
        }else if(device instanceof Receiver){
            Receiver receiver = (Receiver)device;
            player = new ReceiverPlayer(receiver);
//             ShortMessage sm = new ShortMessage( );
//             sm.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
//             receiver.send(sm, -1);
//             frame = new Keyboard(new ReceiverPlayer(receiver));
        }else{
            throw new Exception("invalid MIDI device: "+devices[i].getName());
        }
        player.programChange(bank, program);
        JFrame frame = new Keyboard(player);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(255, 255);
        frame.setVisible(true);
    }

    public Keyboard(Player play) 
        throws Exception {
        super("Keyboard");
        player = play;

        addKeyListener(new KeyAdapter( ) {
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode( );
                    try{
                        if(key == KeyEvent.VK_UP){
                            player.programChange(bank, ++program);
                            System.out.println("program "+program);
                        }else if(key == KeyEvent.VK_DOWN){
                            player.programChange(bank, --program);
                            System.out.println("program "+program);
                        }else if(key == KeyEvent.VK_RIGHT){
                            player.programChange(++bank, program);
                            System.out.println("bank "+bank);
                        }else if(key == KeyEvent.VK_LEFT){
                            player.programChange(--bank, program);
                            System.out.println("bank "+bank);
                        }else if (key >= 35 && key <= 81 && !noteOn[key]) {
                            noteOn[key] = true;
                            player.noteon(key);
                        }
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
                public void keyReleased(KeyEvent e) {
                    int key = e.getKeyCode( );
                    KeyEvent nextPress = (KeyEvent) Toolkit.getDefaultToolkit().
                        getSystemEventQueue().
                        peekEvent(KeyEvent.KEY_PRESSED);
                    if ((nextPress == null) ||
                        (nextPress.getWhen() != e.getWhen()) ||
                        (nextPress.getKeyCode() != e.getKeyCode())) {
                        if (key >= 35 && key <= 81) {
                            noteOn[key] = false;
                            try{
                                player.noteoff(key);
                            }catch(Exception exc){
                                exc.printStackTrace();
                            }
                        }
                    }
                }
            });
        addMouseMotionListener(new MouseMotionAdapter( ) {
                public void mouseMoved(MouseEvent e) {
                    int velocity = e.getX()/2;
                    System.out.println("velocity "+velocity);
                    player.setVelocity(velocity);
                    int bend = e.getY()/2;
                    System.out.println("bend "+bend);
                    try{
                        player.bend(bend);
                    }catch(Exception exc){
                        exc.printStackTrace();
                    }
                }
            });
    }

//     public Keyboard(Synthesizer synth) {
//         super("Keyboard");

//         // Channel 10 is the GeneralMidi percussion channel.  In Java code, we
//         // number channels from 0 and use channel 9 instead.
//         channel = synth.getChannels()[9];

//         addKeyListener(new KeyAdapter( ) {
//                 public void keyPressed(KeyEvent e) {
//                     int key = e.getKeyCode( );
//                     if(key >= 35 && key <= 81 && !noteOn[key]) {
//                         noteOn[key] = true;
//                         channel.noteOn(key, velocity);
//                     }
//                 }
//                 public void keyReleased(KeyEvent e) {
//                     KeyEvent nextPress = (KeyEvent) Toolkit.getDefaultToolkit().
//                         getSystemEventQueue().
//                         peekEvent(KeyEvent.KEY_PRESSED);
//                     if ((nextPress != null) &&
//                         (nextPress.getWhen() == e.getWhen()) &&
//                         (nextPress.getKeyCode() == e.getKeyCode()))
//                         return; // keyboard repeat
//                     int key = e.getKeyCode( );
//                     noteOn[key] = false;
//                     if (key >= 35 && key <= 81) channel.noteOff(key);
//                 }
//             });

//         addMouseMotionListener(new MouseMotionAdapter( ) {
//                 public void mouseMoved(MouseEvent e) {
//                     velocity = e.getX( );
//                 }
//             });
//     }
}