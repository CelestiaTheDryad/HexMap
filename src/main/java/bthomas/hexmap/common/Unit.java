package bthomas.hexmap.common;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.json.JsonSerializable;
import bthomas.hexmap.common.util.JsonUtils;
import com.google.gson.JsonObject;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashSet;

/**
 * This class represents one unit on the Hexmap
 *
 * @author Brendan Thomas
 * @since 2017-10-28
 */
public class Unit implements Serializable, JsonSerializable
{
    private static final long serialVersionUID = 8755948938950476613L;
    private static int UIDTracker = 0;

    public String name;
    private static final String nameKey = "name";
    public int locX, locY, UID;
    private static final String xKey = "x";
    private static final String yKey = "y";
    private static final String UidKey = "uid";

    public Color color;
    private static final String colorKey = "color";

    /**
     * Standard constructor
     *
     * @param name The name given to this unit
     * @param locX This unit's X location
     * @param locY This unit's Y location
     * @param color The color of this unit's icon
     */
    public Unit(String name, int locX, int locY, Color color) {
        this.name = name;
        this.locX = locX;
        this.locY = locY;
        this.color = color;

        synchronized(Unit.class)
        {
            this.UID = UIDTracker;
            UIDTracker++;
        }
    }

    /**
     Client-Side constructor

     * @param name The name given to this unit
     * @param locX This unit's X location
     * @param locY This unit's Y location
     * @param UID The unique identifier for this unit
     * @param color The color of this unit's icon
     */
    public Unit(String name, int locX, int locY, int UID, Color color) {
        this.name = name;
        this.locX = locX;
        this.locY = locY;
        this.UID = UID;
        this.color = color;
    }

    /**
     Constructs an object from JSON representation

     @param root
     The JsonObject containing the data for this object.

     @throws JsonConversionException
     If there is not proper data stored in the JsonObject
     */
    public Unit(JsonObject root) throws JsonConversionException
    {
        name = JsonUtils.getString(root, nameKey);
        color = new Color(JsonUtils.getInt(root, colorKey), true);
        locX = JsonUtils.getInt(root, xKey);
        locY = JsonUtils.getInt(root, yKey);
        UID = JsonUtils.getInt(root, UidKey);
    }

    @Override
    public void buildJson(JsonObject root, HashSet<Object> loopDetector)
    {
        root.addProperty(nameKey, name);
        //actually gets RGBA
        root.addProperty(colorKey, color.getRGB());
        root.addProperty(xKey, locX);
        root.addProperty(yKey, locY);
        root.addProperty(UidKey, UID);
    }
}
