package bthomas.hexmap.client;

import bthomas.hexmap.common.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/*
This class handles the drawing and rendering of the hexagonal map.
It will also contain code for mapping mouse clicks to hex locations.

Author: Brendan Thomas
Date: October 21, 2017
 */

/**
 * This class handles the drawing and rendering of the hexagonal map.
 * It also contains the code for mapping mouse clicks to hex locations.
 *
 * @author Brendan Thomas
 * @since 2017-10-21
 */
public class HexMapCanvas extends JPanel{

    private int sizeX;
    private int sizeY;
    private int hexSize;
    //TODO: make this a HashMap using the unit's UID
    private ArrayList<Unit>[][] units;

    private Hexagon[][] hexgrid;
    private final Color background = new Color(125,125,125);

    /**
     * Main constructor for the canvas
     *
     * @param numCol The number of columns in the map
     * @param numRow The number of rows in the map
     * @param size The size in pixels of each hexagon
     */
    public HexMapCanvas(int numCol, int numRow, int size) {
        sizeX = numCol;
        sizeY = numRow;
        hexSize = size;
        setBackground(background);

        //used to set a default size in swing for this canvas
        setSize(getGridSize(numRow, numCol, size));

        //setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        makeHexes();
    }

    @Override
    public Dimension getPreferredSize() {
        return getGridSize(sizeX, sizeY, hexSize);
    }


    /**
     * Constructs the grid of hexagons used to draw the map
     */
    private void makeHexes() {
        //init grid of hexagons
        hexgrid = new Hexagon[sizeX][sizeY];

        //you cant do new ArrayList<Unit>[][] because java can't check the types of ArrayLists during runtime
        //so you could assign an element of this array to be ArrayList<Integer> if you wanted without an error
        //an unchecked cast gives us the desired functionality, and is safe as long as no one does anything stupid like that
        units = (ArrayList<Unit>[][]) new ArrayList[sizeX][sizeY];

        //make the hexes
        for(int x = 0; x < sizeX; x++) {
            for(int y = 0; y < sizeY; y++) {
                units[x][y] = new ArrayList<>();
                hexgrid[x][y] = new Hexagon(x, y, hexSize, units[x][y]);
            }
        }
    }


    /**
     * Adds a new unit to the map and redraws the map
     * This method assumes the unit will fit in the GUI
     *
     * @param chr The unit to add
     */
    public void addUnit(Unit chr) {
        //TODO: add protection against having more than 4 units in a spot
        units[chr.locX][chr.locY].add(chr);
        repaint();
    }

    /*
    Moves a unit from its current location to a new one

    overloaded
     */

    /**
     * Moves a unit from its current location to a new one
     *
     * @param chr The unit to move
     * @param x X position to move to
     * @param y Y position to move to
     */
    public void moveUnit(Unit chr, int x, int y) {
        //TODO: add protection against having more than 4 units in a spot
        units[chr.locX][chr.locY].remove(chr);
        units[x][y].add(chr);
        chr.locX = x;
        chr.locY = y;
        repaint();
    }

    /**
     * Gets the list of units at a location
     *
     * @param x The X location to get units from
     * @param y The Y location to get units from
     * @return The list of units at that location
     */
    public ArrayList<Unit> getUnits(int x, int y) {
        return units[x][y];
    }

    /**
     * Gets the list of units at a location
     *
     * @param p The point to get units from
     * @return The list of units at that location
     */
    public ArrayList<Unit> getUnits(Point p) {
        return getUnits(p.x, p.y);
    }


    /**
     * Paints the canvas and all sub elements
     *
     * @param g The Graphics2D object to draw on
     */
    @Override
    public void paintComponent (Graphics g) {
        super.paintComponent(g);
        //some hexagons need to specifically overwrite their neighbors afterwards
        ArrayList<Hexagon> drawLater = new ArrayList<>();

        //Don't know why this is safe but internet examples always have it
        Graphics2D g1 = (Graphics2D) g;
        for(int x = 0; x < sizeX; x++) {
            for(int y = 0; y < sizeY; y++) {
                if(hexgrid[x][y].isHighlighted()) {
                    drawLater.add(hexgrid[x][y]);
                }
                else {
                    hexgrid[x][y].paint(g1, 3);
                }
            }
        }

        //paint special hexagons last so they overwrite the others
        for(Hexagon h: drawLater) {
            h.paint(g1, 3);
        }

    }


    /**
     * Maps a pixel location to a Hexagon location on the grid
     *
     * @param p The pixel location
     * @return The hexagon location the pixel location maps to, or null if it is not on a hexagon
     */
    public Point mapToLocation(Point p) {
        for(int col = 0; col < sizeX; col++) {
            for(int row = 0; row < sizeY; row++ ) {
                if(hexgrid[col][row].contains(p)) {
                    return new Point(col, row);
                }
            }
        }

        return null;
    }

    /*
    sets a given tile of the hex grid to be highlighted
    assumes point is in the grid

    overloaded
     */

    /**
     * Sets a given grid location to be highlighted or not
     *
     * @param highlight Whether the grid element should be highlighted or not
     * @param x The X location of the grid element to change
     * @param y The Y location of the grid element to change
     */
    public void setHighlighted(Boolean highlight, int x, int y) {
        hexgrid[x][y].setHighlighted(highlight);
        repaint();
    }

    /**
     * Sets a given grid location to be highlighted or not
     *
     * @param highlight Whether the grid element should be highlighted or not
     * @param p The location of the grid element to change
     */
    public void setHighlighted(Boolean highlight, Point p) {
        setHighlighted(highlight, p.x, p.y);
    }

    /**
     * Gets the appropriate size for the Hex canvas to be fully displayed
     *
     * @param rows The number of rows in the grid
     * @param cols The number of columns in the grid
     * @param radius The width in pixels of a grid element
     * @return The size of the map
     */
    public static Dimension getGridSize(int rows, int cols, int radius) {
        int width = (int) (cols * radius * 1.5 + radius * .5) + 6;
        int height = (int) (rows * Math.sqrt(3) * radius + Math.sqrt(3) / 2 * radius) + 6;
        return new Dimension(width, height);
    }
}
