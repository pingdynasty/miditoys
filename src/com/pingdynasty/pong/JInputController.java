package com.pingdynasty.pong;

// import java.awt.Component;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component.Identifier;

public class JInputController extends RacketController implements Runnable {

    private static final float steplength = 4.0f;
    private static final float max_threshold = 0.01f;
    private static final float min_threshold = -0.01f;
    private java.awt.Component awt;
    private Thread poller;
    private Controller controller;
    private Component component;
//     private Axis axis;
    private boolean running = true;

    public JInputController(Racket racket, java.awt.Component awt){
        super(racket);
        this.awt = awt;

        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();
        // retrieve the available controllers
        Controller[] controllers = ce.getControllers();
        for(int i=0; i<controllers.length; ++i){
            if(controllers[i].getType() == Controller.Type.GAMEPAD ||
               controllers[i].getType() == Controller.Type.STICK){
                controller = controllers[i];
                break;
            }
        }
        Component[] components = controller.getComponents();
        for(int i=0; i<components.length; ++i){
            if(components[i].getName().equalsIgnoreCase("y")){
                component = components[i];
                break;
            }
        }

        poller = new Thread(this);
        poller.setDaemon(true);
        poller.start();
    }

    public static void main(String args[])
        throws Exception {
        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();

        // retrieve the available controllers
        Controller[] controllers = ce.getControllers();

        for(int i=0; i<controllers.length; ++i)
            System.out.println("Controller: " + controllers[i].getName() + 
                               " type: " + controllers[i].getType());
		
        //fetch gamepad controller
        Controller gamePadContr = null;
        for(int i=0; i<controllers.length; ++i){
            if(controllers[i].getType() == Controller.Type.GAMEPAD ||
               controllers[i].getType() == Controller.Type.STICK) {
                gamePadContr = controllers[i];
                break;
            }
        }
		
        //none found
        if(gamePadContr == null){
            throw new NullPointerException("No gamepad found");
        }

        Component[] components = gamePadContr.getComponents();
        for(int i=0; i< components.length; ++i){
            System.out.println("Component: " + components[i].getName() + " id: " + components[i].getIdentifier().getName());
        }
        

        Component buttonComponent = gamePadContr.getComponent(Identifier.Axis.POV);

        JInputController c = new JInputController(null, null);
        Thread.sleep(100000);
		
//         float prevData = 0;
//         while(gamePadContr.poll()){
//             float data = buttonComponent.getPollData();
//             if(data != prevData)
//                 System.out.println(data);
//         }
    }

// ControllerEnvironment ce =
// ControllerEnvironment.getDefaultEnvironment();
// ************************************

// Then you can retrieve the available controllers from the environment (such as gamepad, mouse)
// ************************************
// Controller[] controllers = ce.getControllers();
// ************************************

// You can make a little test to show every controller he finds with the following code
// ************************************
// for(Controller c : controllers){
// System.out.println("Name: " + c.getName() + " type: " + c.getType());
// }
// ************************************

// To get the gamepad out of the controllers array, use the following code
// The only thing it does is traverse through the controllers array, and see if any of them is a gamepad. And if he has found one, he will quit the loop
// ********************************
// Controller gamePadContr = null;
// for(Controller c : controllers){
// if(c.getType() == Controller.Type.GAMEPAD) {
// gamePadContr = c;
// break;
// }
// }

    public void run(){
        while(running){
            if(controller.poll()){ // update the controller from the hardware
                float value = component.getPollData();
                if(value < min_threshold || value > max_threshold){
                    int delta = (int)(steplength * value);
                    if(delta != 0){
                        System.out.println("move "+delta);
                        move(delta);
                        awt.repaint();
                    } 
                    try{
                        Thread.sleep(5);
                    }catch(InterruptedException exc){}
                }else{
                    try{
                        Thread.sleep(20);
                    }catch(InterruptedException exc){}
                }
//             float y = controller.getAxisValue(1); // assuming second axis is the right control
            }else{
                try{
                    Thread.sleep(20);
                }catch(InterruptedException exc){}
            }
        }
    }

    public void destroy(){
        running = false;
    }
}
