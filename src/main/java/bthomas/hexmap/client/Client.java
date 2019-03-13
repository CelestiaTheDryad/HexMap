package bthomas.hexmap.client;

import bthomas.hexmap.Main;
import bthomas.hexmap.common.Unit;
import bthomas.hexmap.net.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * This class is the main GUI and networking code for the client side
 * of the RPG Hexmap program.
 *
 * This program is currently meant for private use only. As such, it
 * has absolutely no security precautions in place to protect information.
 *
 * @author Brendan Thomas
 * @since 2017-10-20
 */
public class Client implements ActionListener, MouseListener, KeyListener {

    private Socket service = null;
    private ObjectInputStream input = null;
    private ObjectOutputStream output = null;

    public boolean connected = false;
    private boolean closing = false;
    private boolean closeReceived = false;
    private boolean settingUp = false;
    private boolean setUp = false;
    private boolean chatStarted = false;
    //TODO: more robust UUID system
    //First, determine what I'd even want from UUIDs, currently not even used
    private int UUID = -1;

    private final ReentrantLock connectionLock = new ReentrantLock();

    //Landing GUI elements
    private JFrame landingFrame;
    private JPanel landingPanel;
    private JTextField ipField;
    private JButton connectButton;
    private JTextField usernameField;
    private JLabel ipLabel;
    private JLabel usernameLabel;
    private JTextArea connectionDisplay;
    private JScrollPane displayPane;

    //Hexmap UI elements
    private JFrame hexmapMainFrame;
    private JPanel hexmapDisplayPanel;
    private HexMapCanvas hexCanvas;
    private JTextField chatEnter;
    private JTextArea chatArea;
    private JScrollPane chatAreaScroller;
    private JButton disconnectButton;

    //event handling variables
    private Unit selectedChr = null;
    private String username;
    private String lastMessage = "";

    public Client () {
        //start Swing UI code and create landing GUI
        SwingUtilities.invokeLater(this::setupConnectionGUI);
    }


    /**
     * Method to set up the initial landing UI. This UI will
     * simply get a username and server info to connect the client.
     */
    private void setupConnectionGUI() {
        landingFrame = new JFrame("Chatroom Client");
        landingFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        landingPanel = new JPanel(new GridBagLayout());
        landingPanel.setBorder(new TitledBorder(new EtchedBorder(), "Test Chatroom"));
        landingFrame.add(landingPanel);

        GridBagConstraints ipLabelC = getGBC(0, 0, 1, 1);
        ipLabelC.insets = new Insets(0, 8, 0, 8);
        ipLabel = new JLabel("Server IP:");
        landingPanel.add(ipLabel, ipLabelC);

        GridBagConstraints ipFieldC = getGBC(1, 0, 1, 1);
        ipField = new JTextField();
        ipField.setPreferredSize(new Dimension(150, 24));
        landingPanel.add(ipField, ipFieldC);

        GridBagConstraints connectButtonC = getGBC(2, 0, 1, 1);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        landingPanel.add(connectButton, connectButtonC);

        GridBagConstraints usernameLabelC = getGBC(0, 1, 1, 1);
        usernameLabelC.insets = new Insets(0, 8, 0, 8);
        usernameLabel = new JLabel("Username:");
        landingPanel.add(usernameLabel, usernameLabelC);

        GridBagConstraints usernameFieldC = getGBC(1, 1, 1, 1);
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(150, 24));
        landingPanel.add(usernameField, usernameFieldC);

        connectionDisplay = new JTextArea(10, 30);
        connectionDisplay.setEditable(false);
        connectionDisplay.setLineWrap(true);
        connectionDisplay.setWrapStyleWord(true);

        GridBagConstraints displayPaneC = getGBC(0, 2, 3, 1);
        displayPane = new JScrollPane(connectionDisplay);
        displayPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        landingPanel.add(displayPane, displayPaneC);

        landingFrame.pack();
        landingFrame.setLocationRelativeTo(null);
        landingFrame.setVisible(true);
    }


    /**
     * Method to setup the Hexmap GUI. This GUI will be the main focus of this program.
     *
     * @param x The width of the map to make (received from server upon connection)
     * @param y The height of the map to make (received from the server upon connection)
     */
    private void setupHexmapGUI(int x, int y) {
        settingUp = true;

        int radius = 25;
        Dimension hexSize = HexMapCanvas.getGridSize(x, y, radius);

        //create window and init
        hexmapMainFrame = new JFrame();
        hexmapMainFrame.setLayout(new GridBagLayout());
        hexmapMainFrame.setTitle("Hexmap");
        hexmapMainFrame.setSize(hexSize.width + 250, hexSize.height + 75);
        hexmapMainFrame.setResizable(false);

        //allow window to close
        hexmapMainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(0);
            }
        });

        hexmapDisplayPanel = new JPanel(new GridBagLayout());
        hexmapDisplayPanel.setSize(hexSize);
        hexmapMainFrame.add(hexmapDisplayPanel, getGBC(0, 0, 1, 1));

        hexCanvas = new HexMapCanvas(x, y, radius);
        hexCanvas.addMouseListener(this);
        hexCanvas.setOpaque(true);
        hexCanvas.setVisible(true);
        hexmapDisplayPanel.add(hexCanvas, getGBC(0, 0, 1, 1));

        chatArea = new JTextArea("");
        chatArea.setSize(225, hexSize.height - 6);
        chatArea.setMinimumSize(new Dimension(225, hexSize.height - 6));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        //chatArea.setEnabled(false);
        //chatArea.setBackground(new Color(210, 210, 210));
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        chatAreaScroller = new JScrollPane(chatArea);
        chatAreaScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatAreaScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        hexmapDisplayPanel.add(chatAreaScroller, getGBC(1, 0, 1, 1, GridBagConstraints.BOTH));
        chatEnter = new JTextField(25);
        chatEnter.addActionListener(this);
        chatEnter.addKeyListener(this);
        hexmapDisplayPanel.add(chatEnter, getGBC(1, 1, 1, 1));

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        hexmapDisplayPanel.add(disconnectButton, getGBC(0, 1, 1, 1));

        hexmapMainFrame.pack();
        hexmapMainFrame.setVisible(true);

        setUp = true;
        settingUp = false;
    }


    /**
     * Inserts a new message into the chat box
     *
     * @param s The string to insert
     */
    public void chatAppend(String s) {
        if(!chatStarted) {
            chatStarted = true;
            chatArea.append(s);
        }
        else {
            chatArea.append("\n" + s);
        }
    }


    /**
     * Attempt to connect to a server with the currently entered IP and port
     */
    public void startConnection() {
        connectionLock.lock();

        //organized this way to be thread safe
        if(connected) {
            System.out.println("Connection attempted while connected");
            connectionLock.unlock();
            return;
        }

        try {
            System.out.println("attempting connection");

            String base = ipField.getText();
            String ip;
            int port = 7777;

            //parse given ip
            //ip is supposed to either be XXX.XXX.XXX.XXX or XXX.XXX.XXX.XXX:YYYYY
            if(base.contains(":")) {
                String[] split = base.split(":");

                //if this is true, the entered IP is invalid and the user should be notified
                if(split.length != 2) {
                    connectionDisplay.append(base + " is in invalid IP format.\n");
                    return;
                }

                //make sure we have a valid port
                if(!Pattern.matches("[0-9]+", split[1])) {
                    connectionDisplay.append(base+ " is in invalid IP format.\n");
                    return;
                }

                port = Integer.parseInt(split[1]);

                //the first section should be the ip address
                ip = split[0];
            }

            //if no port is given, default to 7777
            else {
                ip = base;
            }

            //make sure we have a valid ip address
            if(!Pattern.matches("([0-9]+\\.){3}[0-9]+", ip)) {
                connectionDisplay.append(base + " is in invalid IP format.\n");
                return;
            }


            service = new Socket(ip, port);

            //There's apparently some wizardry with these streams that doesn't exist with text streams
            //The output stream must be created first on one side, and the input stream first on the other
            //The server makes the input stream first
            output = new ObjectOutputStream(service.getOutputStream());
            input = new ObjectInputStream(service.getInputStream());

            new Thread(new ConnectionListener(this, input)).start();

            System.out.println("Made connection");
            connected = true;
            username = usernameField.getText();

            //start automatic communication with server
            sendMessage(new HandshakeMessage(Main.version, username));
        }
        catch (IOException e1) {
            connectionDisplay.append("Error connecting to " + ipField.getText() + "\n");
            cleanConnections();
        }
        finally {
            connectionLock.unlock();
        }
    }

    /**
     * Upon successful connection to server, delete the landing GUI and start the Hexmap GUI
     *
     * @param sizeX Width of the map, received from server
     * @param sizeY Height of the map, received from the server
     * @param UUID unique identifier assigned tot eh client from the server
     */
    public void initConnection(int sizeX, int sizeY, int UUID) {
        this.UUID = UUID;
        settingUp = true;
        //close the connection GUI
        landingFrame.dispose();

        //start the map GUI
        setupHexmapGUI(sizeX, sizeY);
    }

    /**
     * Adds a Unit to the Hexmap.
     *
     * @param chr The character to add
     */
    public void addUnit(Unit chr) {
        hexCanvas.addUnit(chr);
    }


    /**
     * Moves a unit from one map location to another
     *
     * @param UID  The unique identifier for the unit to move
     * @param toX The x location to move the unit to
     * @param toY The y location to move the unit to
     * @param fromX The current x location of the unit
     * @param fromY The current y location of the unit
     */
    public void moveUnit(int UID, int toX, int toY, int fromX, int fromY) {
        ArrayList<Unit> chrs = hexCanvas.getUnits(fromX, fromY);
        for(Unit c: chrs) {
            if(c.UID == UID) {
                hexCanvas.moveUnit(c, toX, toY);
            }
        }
    }

    /**
     * Blocks until the main Hexmap GUI is built
     *
     * Prevents errors when connecting to servers
     */
    public void waitForGUI() {
        //wait for GUI to be set up if it's not
        //shouldn't take long or block infinitely
        while (!setUp && settingUp) {
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
            }
        }

        //how the hell did this happen
        if(!setUp) {
            System.out.println("Error while waiting for UI setup.");
            System.exit(1);
        }
    }

    /**
     * Message interface to send to the server
     *
     * @param message The message to send
     */
    private void sendMessage(HexMessage message) {
        try {
            output.writeObject(message);
        }
        catch (IOException e) {
            System.out.println("Error sending message to server.");
            e.printStackTrace();
        }
    }

    /**
     * Callback method for the menu used to select units on the map.
     *
     * @param data String used to identify the selected unit, formatted UID-Xloc-Yloc
     */
    public void selectUnit(String data) {
        String[] s = data.split("-");

        if(s.length != 3) {
            System.out.println("Something broke with unit selection.");
            return;
        }

        int x = Integer.parseInt(s[1]);
        int y = Integer.parseInt(s[2]);

        //find the unit that was selected
        ArrayList<Unit> chrs = hexCanvas.getUnits(x, y);

        for(Unit c: chrs) {
            if(c.UID == Integer.parseInt(s[0])) {
                selectedChr = c;
                //this gets called from an actionListener, which comes from the GUI thread
                //so we can update the GUI here
                hexCanvas.setHighlighted(true, x, y);
            }
        }
    }

    /**
     * Clear all connections, data, and configs and reset to new no matter what
     */
    private void cleanConnections() {
        //TODO: research proper handling for these errors
        try {
            if(output != null) {
                output.close();
            }
        }
        catch (IOException e) {
            System.out.println("e1");
        }
        finally {
            output = null;
        }

        try {
            if(input != null) {
                input.close();
            }
        }
        catch (IOException e) {
            System.out.println("e2");
        }
        finally {
            input = null;
        }

        try {
            if(service != null) {
                service.close();
            }
        }
        catch (IOException e) {
            System.out.println("e3");
        }
        finally {
            service = null;
        }
    }

    /**
     * Cleanly disconnect from a server if connected
     */
    public void disconnect() {
        connectionLock.lock();
        if(!connected) {
            connectionLock.unlock();
            return;
        }

        // make sure to disconnect cleanly from server
        if(!closeReceived) {
            sendMessage(new CloseMessage());
        }

        connected = false;
        cleanConnections();
        connectionLock.unlock();
        System.out.println("Connection Closed");
        //TODO: reset to connection screen
        System.exit(0);
    }

   /* ========================================================================================

   Event Handlers

   ========================================================================================= */
   //Handles button presses, text box enters
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if(source == connectButton) {
            //connecting can block, so do it in a new thread so GUI remains responsive
            new Thread(this::startConnection).start();
        }
        else if(source == disconnectButton) {
            disconnect();
        }
        else if(source == chatEnter) {
            String text = chatEnter.getText().trim();

            //do nothing on empty messages
            if(text.equals("")) {
                return;
            }

            lastMessage = text;

            //if the first unit is "/" treat as a command
            if(text.length() > 1 && text.charAt(0) == '/') {
                String[] parts = text.substring(1).split(" ", 2);
                sendMessage(new CommandMessage(parts[0], parts.length == 2 ? parts[1] : null));
            }
            //If not a command, send as a chat message
            else {
                sendMessage(new ChatMessage(username + ": " + text));
            }
            //clear entry bar
            chatEnter.setText("");
        }
    }


    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == hexCanvas) {
            Point location = hexCanvas.mapToLocation(e.getPoint());

            //point was not inside a grid tile, so do nothing
            if(location == null) {
                return;
            }

            //System.out.println(String.format("Grid Clicked. X: %d, Y: %d", location.x, location.y));

            // the user is trying to select a unit
            if(selectedChr == null) {
                ArrayList<Unit> chrs = hexCanvas.getUnits(location);

                //location has no characters, so do nothing
                if(chrs.size() == 0) {
                    return;
                }

                //there's only one unit, so we know what to select
                if(chrs.size() == 1) {
                    selectedChr = chrs.get(0);
                    hexCanvas.setHighlighted(true, location);
                    return;
                }

                //there's more than one unit, so we must have the user choose in a context menu
                PopupMenu menu = new PopupMenu();

                ActionListener listener = (event) -> selectUnit(event.getActionCommand());

                for(Unit c: chrs) {
                    MenuItem i = new MenuItem(c.name);
                    i.setActionCommand(String.format("%d-%d-%d", c.UID, c.locX, c.locY));
                    i.addActionListener(listener);
                    menu.add(i);
                }

                //create the menu on screen
                hexmapDisplayPanel.add(menu);
                menu.show(hexmapDisplayPanel, e.getX(), e.getY());

                //this thread of execution "continues" in selectUnit()
            }
            // the user is trying to move a unit
            else{
                hexCanvas.setHighlighted(false, selectedChr.locX, selectedChr.locY);
                sendMessage(new MoveUnitMessage(selectedChr.UID, location.x, location.y, selectedChr.locX, selectedChr.locY));
                selectedChr = null;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        //repeat last message on up arrow
        if(e.getKeyCode() == KeyEvent.VK_UP) {
            SwingUtilities.invokeLater(() -> chatEnter.setText(lastMessage));
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

   /* ========================================================================================

   Helper Methods

   ========================================================================================= */

    /*
    Generates GridBagConstraints (used to position elements in a Swing Grid layout)
     */
    private GridBagConstraints getGBC(int x, int y, int xSize, int ySize) {
        return new GridBagConstraints(x, y, xSize, ySize, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    }

    private GridBagConstraints getGBC(int x, int y, int xSize, int ySize, int fill) {

        return new GridBagConstraints(x, y, xSize, ySize, 0.0, 0.0, GridBagConstraints.CENTER, fill, new Insets(0, 0, 0, 0), 0, 0);
    }


    public static void main(String[] args) {
        Client main = new Client();
    }
}
