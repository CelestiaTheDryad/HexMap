package bthomas.hexmap.common;

import java.awt.*;
import java.io.Serializable;

/*
This class is a simple data container class to represent one
unit on the Hexmap.
 */
public class Unit implements Serializable {
    private static int UIDTracker = 0;

    public String name;
    public int locX, locY, UID;
    public Color color;

    public Unit(String name, int x, int y, Color color) {
        this.name = name;
        locX = x;
        locY = y;
        this.color = color;

        this.UID = UIDTracker;
        UIDTracker += 1;
    }
}
