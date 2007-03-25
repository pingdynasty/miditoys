package com.pingdynasty.pong;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component.Identifier;

public class JInputController extends RacketController {

    private static final float steplength = 4.0f;
    private static final float max_threshold = 0.01f;
    private static final float min_threshold = -0.01f;
    private Controller controller;
    private Component component;
//     private Axis axis;
    private boolean running = true;

    public JInputController(Racket racket){
        super(racket);
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
    }

    public void move(){
        if(controller.poll()){ // update the controller from the hardware
            float value = component.getPollData();
            if(value < min_threshold || value > max_threshold){
                int delta = (int)(steplength * value);
                if(delta != 0)
                    move(delta);
            }
        }
    }

    // test controller availability
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

//         float prevData = 0;
//         while(gamePadContr.poll()){
//             float data = buttonComponent.getPollData();
//             if(data != prevData)
//                 System.out.println(data);
//         }
    }
}
