package bthomas.hexmap.common;

import java.awt.*;
import java.io.Serializable;

/**
 * This class represents one unit on the Hexmap
 *
 * @author Brendan Thomas
 * @since 2017-10-28
 */
public class Unit implements Serializable {
    private static int UIDTracker = 0;

    public String name;
    public int locX, locY, UID;
    public Color color;

    /**
     * Standard constructor
     *
     * @param name The name given to this unit
     * @param x This unit's X location
     * @param y This unit's Y location
     * @param color The color of this unit's icon
     */
    public Unit(String name, int x, int y, Color color) {
        this.name = name;
        locX = x;
        locY = y;
        this.color = color;

        this.UID = UIDTracker;
        UIDTracker += 1;
    }
}
