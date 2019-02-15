package bthomas.hexmap.client;

import bthomas.hexmap.common.Unit;

import java.awt.*;
import java.util.ArrayList;

/*
This class represents one hexagon in the Hexmap.
It contains the code required to draw itself.
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

    public Hexagon(int x, int y, int rad) {
        //polygon variables
        npoints = 6;
        xpoints = new int[npoints];
        ypoints = new int[npoints];

        //Hexagon variables
        locX = x;
        locY = y;
        radius = rad;
        updatePoints();
    }


    /*
    Updates the internal Polygon variables to match the given position and size of
    the hexagon.
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

    public void setUnits(ArrayList<Unit> chrs) {
        units = chrs;
    }

    /*
    Paints this hexagon an a graphics object with a given list of units
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
