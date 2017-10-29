import java.awt.*;

/*
This class is a simple data container class to represent one
character on the Hexmap.
 */
public class Character {
    public String name;
    public int locX;
    public int locY;
    public Color color;

    public Character(String nm, int x, int y, Color cl) {
        name = nm;
        locX = x;
        locY = y;
        color = cl;
    }
}
