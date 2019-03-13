package bthomas.hexmap.client;

import bthomas.hexmap.common.Unit;

import java.awt.*;
import java.util.ArrayList;

/**
 * This class represents one hexagon on the map
 * Uses Polygon as a base for some math and drawing functionality
 *
 * @author Brendan Thomas
 * @since 2017-10-21
 */
public class Hexagon extends Polygon{

    private int locX;
    private int locY;
    private int radius;
    private Boolean highlighted = false;
    private ArrayList<Unit> units;

    //draw constants
    private final Color background = new Color(75,75,75);
    private final Color border = new Color(0,0,0);
    private final Color highlightBorder = new Color(190, 190, 0);

    /**
     * Standard constructor for a Hexagon
     *
     * @param x This hexagon's X location in the grid
     * @param y This hexagon's Y location in the grid
     * @param rad This hexagon's size in pixels
     * @param units The container for the units on this hexagon
     */
    public Hexagon(int x, int y, int rad, ArrayList<Unit> units) {
        //polygon variables
        npoints = 6;
        xpoints = new int[npoints];
        ypoints = new int[npoints];

        //Hexagon variables
        locX = x;
        locY = y;
        radius = rad;
        this.units = units;
        updatePoints();
    }

    /**
     * Sets the internal Polygon variables to match the information fo this Hexagon
     */
    private void updatePoints() {
        //points below calculated as if hex was snug to top-left corner
        //this is the pixel offset required by the hexagon's position
        //numbers from geometry (thanks Euclid)
        int offsetX = locX * (int) (radius * 1.5) + 3;
        int offsetY = locY * (int) (Math.sqrt(3) * radius) + 3;

        //the odd numbered columns are shifted down in a hex grid
        if(locX % 2 == 1) {
            offsetY += (int) (Math.sqrt(3) * radius) / 2;
        }

        //calculate points of Hexagon, starting with rightmost and going counterclockwise
        for(int i = 0; i < npoints; i++) {
            double angle = (double) i / 6 * (2 * Math.PI);
            int x = offsetX + radius + (int) (Math.cos(angle) * radius);
            int y = offsetY + (int) (Math.sqrt(3) * radius / 2) + (int) (Math.sin(angle) * radius);
            xpoints[i] = x;
            ypoints[i] = y;
        }
    }

    /**
     * Paints this hexagon on the given graphics objects with unit markers
     *
     * @param g The graphics object to paint on
     * @param thickness The thickness in pixels of the hexagon border
     */
    public void paint(Graphics2D g, int thickness) {
        //fill in hexagon
        g.setColor(background);
        g.fillPolygon(this);

        //draw border of hexagon
        if(highlighted) {
            g.setColor(highlightBorder);
        }
        else {
            g.setColor(border);
        }
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        g.drawPolygon(this);

        //if no units to draw, we're done
        if (units.size() == 0) {
            return;
        }

        //expects 4 or fewer units
        Point[] points = {new Point(0, 0), new Point(1, 1), new Point(1, 0), new Point(0, 1)};
        int count = 0;
        for(Unit c: units) {
            //get position relative to top left of hexagon
            int x = points[count].x * 2 * radius / 3 + 2 * radius / 3;
            int y = points[count].y * (int) (Math.sqrt(3) * radius / 2) + (int) (Math.sqrt(3) * radius / 4);

            //get absolute position
            int offsetX = locX * (int) (radius * 1.5) + 3;
            int offsetY = locY * (int) (Math.sqrt(3) * radius) + 3;
            //the odd numbered columns are shifted down in a hex grid
            if(locX % 2 == 1) {
                offsetY += (int) (Math.sqrt(3) * radius) / 2;
            }
            x += offsetX;
            y += offsetY;

            //get first three units of name
            String name;
            if(c.name.length() > 2) {
                name = c.name.substring(0, 2);
            }
            else {
                name = c.name;
            }

            //draw unit icon and nameplate
            int diameter = 2 * radius / 3;
            g.setColor(c.color);
            g.fillOval(x - diameter / 2, y - diameter / 2, diameter, diameter);

            g.setColor(Color.WHITE);
            g.drawString(name, x - diameter / 2, y + diameter / 3);

            //increment counter
            count += 1;

            //don't draw units past the fourth for now
            if(count > 3) {
                break;
            }
        }
    }

    public void setHighlighted(Boolean highlight) {
        highlighted = highlight;
    }

    public Boolean isHighlighted() {
        return highlighted;
    }
}
