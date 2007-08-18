
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.media.*;

public class HelloMobile extends MIDlet implements CommandListener, ItemCommandListener {
    private Display mDisplay;
    private Command exitCmd;
    private Command[] cmds;
    private Form mForm;
    private Player[] players;
    private String[] names = new String[]{"start",
                                          "shot",
                                          "hit",
                                          "loose",
                                          "win"};

    public void startApp(){
        if(mForm == null){
            mForm = new Form("pingdynasty midlet!");
            exitCmd = new Command("Exit", "Exit application", Command.EXIT, 1);
            mForm.addCommand(exitCmd);
            cmds = new Command[names.length];
            players = new Player[names.length];
            for(int i=0; i<cmds.length; ++i){
                cmds[i] = new Command(names[i], Command.ITEM, 1);
                StringItem item = new StringItem(names[i], "", Item.BUTTON);
                item.setDefaultCommand(cmds[i]);
//                 item.addCommand(cmds[i]);
                item.setItemCommandListener(this);
                mForm.append(item);
                try{
                    InputStream is = getClass().getResourceAsStream(names[i]+".wav");
                    players[i] = Manager.createPlayer(is, "audio/x-wav");
                    players[i].realize();
                    players[i].prefetch();
                }catch(IOException exc){
                    mForm.append("error: "+exc);
                }catch(MediaException exc){ 
                    mForm.append("error: "+exc);
                }
                mForm.addCommand(cmds[i]);
            }
            mForm.setCommandListener(this);
            mDisplay = Display.getDisplay(this);
            mForm.append("\nplay");
            Control[] controls = players[0].getControls();
            for(int j=0; j<controls.length; ++j)
                mForm.append("control: "+controls[j]+": "+controls[j].getClass().getName());
        }
        mDisplay.setCurrent(mForm);
    }

    public void pauseApp(){
//         notifyPaused();
    }
	
    public void destroyApp(boolean unconditional){
        for(int i=0; i<players.length; ++i)
            if(players[i] != null)
                players[i].close();
//         notifyDestroyed();
    }


    public void commandAction(Command c, Item item){
        command(c);
    }
                          
    public void commandAction(Command c, Displayable s ){   
        command(c);
    }

    protected void command(Command c){
        if(c == exitCmd){
            mForm.append("\nbye bye!");
            destroyApp(true);
            notifyDestroyed();
        }else{
            for(int i=0; i<cmds.length; ++i){
                if(c == cmds[i]){
                    try{
                        if(players[i] != null)
                            players[i].start();
                        else
                            mForm.append("\nno player: "+(i+1));
                    }catch(MediaException exc){ 
                        mForm.append("error: "+exc);
                    }
                }
            }
        }
    }
}
