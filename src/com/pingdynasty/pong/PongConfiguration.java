package com.pingdynasty.pong;

import java.awt.Point;
import java.awt.Font;

public class PongConfiguration {

    boolean doWalls = false;
    boolean doModulation = false;
    boolean doBend = false;
    boolean doLog = true;
    Point noterange = new Point(48,24); // two octaves
    int x = 15; // top left corners of playing area
    int y = 15;
    int width = 480; // width/height of playing area
    int height = 480;
    int leftgoal = x + 15;
    int rightgoal = x + width - 15;
    int bpm = 80;
    int ticksperclock = 4;
    int clocksperbeat = 48;
    int clockoffset = clocksperbeat / 2;
    int clock = clockoffset;
    int maxVerticalSpeed = 18;
    int computerRacketFudge = 15;
    int computerRacketSkill = 8;

    Font font = new Font("Arial", Font.BOLD, 18);
}
