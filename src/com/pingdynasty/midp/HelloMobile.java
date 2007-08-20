package com.pingdynasty.midp;

import java.io.IOException;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.MediaException;

public class HelloMobile extends MIDlet implements CommandListener{

    private HelloMobileCanvas canvas;
    private static final Command exitCmd = new Command("Exit", Command.EXIT,1);

    public void startApp() {
        Display display = Display.getDisplay(this);
        if(canvas == null){
            try{
                canvas = new HelloMobileCanvas(display);
                canvas.start();
                canvas.addCommand(exitCmd);
                canvas.setCommandListener(this);
            }catch(Exception exc){
                display.setCurrent(new Alert("error", exc.toString(), null, null));
                canvas = null;
                return;
            }
        }
        Display.getDisplay(this).setCurrent(canvas);
    }

    public void pauseApp() {}

    public void destroyApp(boolean unconditional) {
        canvas.stop();
    }

    public void commandAction(Command c, Displayable d){
        if(c == exitCmd){
            destroyApp(true);
            notifyDestroyed();
        }
    }
}