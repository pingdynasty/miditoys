package com.pingdynasty.midi.bcontrol;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import com.pingdynasty.midi.ResourceLocator;

public class BControlButton extends JButton {

    private static Icon ICON =
        ResourceLocator.getIcon("bcontrol/switch_off_36x25.png");
    private static Icon SELECTED_ICON =
        ResourceLocator.getIcon("bcontrol/switch_on_36x25.png");
    private static Icon PRESSED_ICON =
        ResourceLocator.getIcon("bcontrol/switch_sel_36x25.png");

    private final static Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);

    private final static Dimension MIN_SIZE = new Dimension(36, 25);
    private final static Dimension PREF_SIZE = new Dimension(40, 30);
    
//     private final static RenderingHints AALIAS = 
// 	new RenderingHints(RenderingHints.KEY_ANTIALIASING, 
// 			   RenderingHints.VALUE_ANTIALIAS_ON);
    
    public BControlButton() {
	setPreferredSize(PREF_SIZE);
        setMinimumSize(MIN_SIZE);
        if(ICON != null)
            setIcon(ICON);
        if(SELECTED_ICON != null)
            setSelectedIcon(SELECTED_ICON);
        if(PRESSED_ICON != null)
            setPressedIcon(PRESSED_ICON);
    }

    // paint the button
    public void paint(Graphics g) {
	if(g instanceof Graphics2D) {
	    Graphics2D g2d = (Graphics2D)g;
	    g2d.setBackground(getParent().getBackground());
// 	    g2d.addRenderingHints(AALIAS);
        }

        // center position
        int x = getWidth() / 2 - 18;
	int y = getHeight() / 2 - 12;

// 	if(hasFocus())
// 	    g.setColor(DEFAULT_FOCUS_COLOR);
// 	else
// 	    g.setColor(Color.white);

        // clear the space
        g.clearRect(x, y, 36, 25);

        if(model.isPressed())
            this.getPressedIcon().paintIcon(this, g, x, y);
        else if(model.isSelected())
            this.getSelectedIcon().paintIcon(this, g, x, y);
        else
            this.getIcon().paintIcon(this, g, x, y);
    }
}
