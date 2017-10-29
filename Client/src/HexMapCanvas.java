import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/*
This class handles the drawing and rendering of the hexagonal map.
It will also contain code for mapping mouse clicks to hex locations.

Author: Brendan Thomas
Date: October 21, 2017
 */
public class HexMapCanvas extends Canvas{

    private int sizeX;
    private int sizeY;
    private int hexSize;
    private ArrayList<Character>[][] characters;

    private Hexagon[][] hexgrid;
    private final Color background = new Color(125,125,125);

    public HexMapCanvas(int numCol, int numRow, int size) {
        sizeX = numCol;
        sizeY = numRow;
        hexSize = size;
        setBackground(background);

        setSize(getGridSize(numRow, numCol, size));
        makeHexes();
    }


    /*
    Constructs the grid of hexagons used to draw the map
     */
    private void makeHexes() {
        //init grid of hexagons
        hexgrid = new Hexagon[sizeX][sizeY];
        //java says I shouldn't do this, I don't know why and
        //don't care because it seems perfectly logical to me
        characters = (ArrayList<Character>[][]) new ArrayList[sizeX][sizeY];

        //make the hexes
        for(int x = 0; x < sizeX; x++) {
            for(int y = 0; y < sizeY; y++) {
                hexgrid[x][y] = new Hexagon(x, y, hexSize);
                characters[x][y] = new ArrayList<>();
            }
        }
    }


    /*
    Adds a new character to the map and redraws the map
    This assumes the character will fir in the grid
     */
    public void addCharacter(Character chr) {
        characters[chr.locX][chr.locY].add(chr);
        repaint();
    }

    /*
    Moves a character from its current location to a new one
     */
    public void moveCharacter(Character chr, Point to) {
        characters[chr.locX][chr.locY].remove(chr);
        characters[to.x][to.y].add(chr);
        chr.locX = to.x;
        chr.locY = to.y;
        repaint();
    }

    /*
    Gets the list of characters at a given location
     */
    public ArrayList<Character> getCharacters(int x, int y) {
        return characters[x][y];
    }


    /*
    This method draws the hex grid with all components inside it.
     */
    @Override
    public void paint (Graphics g) {
        Graphics2D g1 = (Graphics2D) g;
        for(int x = 0; x < sizeX; x++) {
            for(int y = 0; y < sizeY; y++) {
                hexgrid[x][y].paint(g1, 3, characters[x][y]);
            }
        }

    }


    /*
    Given a pixel location, maps it to a given hex
    Null means the point could not be mapped in this grid.
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
    Returns a dimension with the size of a given hex grid for sizing purposes
     */
    public static Dimension getGridSize(int rows, int cols, int radius) {
        int width = (int) (cols * radius * 1.5 + radius * .5) + 6;
        int height = (int) (rows * Math.sqrt(3) * radius + Math.sqrt(3) / 2 * radius) + 6;
        return new Dimension(width, height);
    }
}
