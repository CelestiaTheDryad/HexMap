package bthomas.hexmap.client;

import bthomas.hexmap.Main;
import bthomas.hexmap.common.Unit;
import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.net.ChatMessage;
import bthomas.hexmap.common.net.CloseMessage;
import bthomas.hexmap.common.net.CommandMessage;
import bthomas.hexmap.common.net.HandshakeMessage;
import bthomas.hexmap.common.net.HexMessage;
import bthomas.hexmap.common.net.InitMessage;
import bthomas.hexmap.common.net.MoveUnitMessage;
import bthomas.hexmap.common.net.NewUnitMessage;
import bthomas.hexmap.common.net.PingMessage;
import bthomas.hexmap.common.net.ValidationMessage;
import bthomas.hexmap.logging.HexmapLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 This class is the main GUI and networking code for the client side of the RPG Hexmap program.
 <p>
 This program is currently meant for private use only. As such, it has absolutely no security precautions in place to
 protect information.

 @author Brendan Thomas
 @since 2017-10-20 */
public class Client implements ActionListener, MouseListener, KeyListener
{

    private Socket service = null;
    private BufferedReader input = null;
    private PrintWriter output = null;

    public boolean connected = false;
    private boolean closeReceived = false;
    private boolean settingUp = false;
    private boolean setUp = false;
    private boolean chatStarted = false;
    public long lastPingReceived = System.currentTimeMillis();
    public long lastPingSent = 0;

    private Path infoFile = Paths.get("clientInfo.txt");

    private final ReentrantLock connectionLock = new ReentrantLock();
    private HashMap<Long, HexMessage> messages = new HashMap<>();

    //to handle main thread waiting
    private final ReentrantLock mainThreadLock = new ReentrantLock();
    private ConnectionListener toListenFrom = null;

    //if the user has clicked the "X" button
    private boolean isClosing = false;

    //Landing GUI elements
    private JFrame landingFrame;
    private JPanel landingPanel;
    private JButton connectButton;
    private JLabel ipLabel;
    private JTextField ipField;
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JTextField passwordField;
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
    private String lastMessage = "";
    private Point lastGridPositionClickedOn = null; //where the user started a click'n'drag on the hex grid

    public Client()
    {
        registerAllMessages();
    }

    public void run()
    {
        Main.scheduleTask(new ClientKeepAliveManager(this), System.currentTimeMillis() + ClientKeepAliveManager.PING_INTERVAL_MILlIS);
        //start Swing UI code and create landing GUI
        SwingUtilities.invokeLater(this::setupConnectionGUI);

        //we need to keep a handle on the main thread for logging reasons, so we store it
        //until we can use it to handle incoming messages
        while(true)
        {
            synchronized(mainThreadLock)
            {
                while(toListenFrom == null && !isClosing)
                {
                    try
                    {
                        mainThreadLock.wait();
                    }
                    catch(InterruptedException e)
                    {
                        Main.logger.log(HexmapLogger.SEVERE,
                                "Interrupted while waiting for connection: " + HexmapLogger.getStackTraceString(e));
                    }
                }
            }
            if(isClosing)
            {
                break;
            }
            toListenFrom.run();
        }
    }

    /**
     Resets the client to connect to a new server
     */
    private void reset()
    {
        service = null;
        input = null;
        output = null;
        connected = false;
        closeReceived = false;
        settingUp = false;
        setUp = false;
        chatStarted = false;
        selectedChr = null;
        lastMessage = "";

        //go back to connection landing screen
        hexmapMainFrame.dispose();
        cleanHexmapGUI();

        //redisplay landing GUI
        landingFrame.pack();
        landingFrame.setVisible(true);
    }

    /**
     Clears references to Hexmap GUI elements so they can be garbage collected
     */
    private void cleanHexmapGUI()
    {
        hexmapMainFrame = null;
        hexmapDisplayPanel = null;
        hexCanvas = null;
        chatEnter = null;
        chatArea = null;
        chatAreaScroller = null;
        disconnectButton = null;
    }


    /**
     Method to set up the initial landing UI. This UI will simply get a username and server info to connect the client.
     */
    private void setupConnectionGUI()
    {
        landingFrame = new JFrame("Chatroom Client");

        //allow window to close from the X button
        landingFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                super.windowClosing(e);
                close();
            }
        });

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

        GridBagConstraints passwordLabelC = getGBC(0, 2, 1, 1);
        passwordLabelC.insets = new Insets(0, 8, 0, 8);
        passwordLabel = new JLabel("Password: ");
        landingPanel.add(passwordLabel, passwordLabelC);

        GridBagConstraints passwordFieldC = getGBC(1, 2, 1, 1);
        passwordField = new JTextField();
        passwordField.setPreferredSize(new Dimension(150, 24));
        landingPanel.add(passwordField, passwordFieldC);

        connectionDisplay = new JTextArea(10, 30);
        connectionDisplay.setEditable(false);
        connectionDisplay.setLineWrap(true);
        connectionDisplay.setWrapStyleWord(true);

        GridBagConstraints displayPaneC = getGBC(0, 3, 3, 1);
        displayPane = new JScrollPane(connectionDisplay);
        displayPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        landingPanel.add(displayPane, displayPaneC);

        //load saved configuration from file
        if(Files.exists(infoFile))
        {
            try
            {
                BufferedReader input = new BufferedReader(new FileReader(infoFile.toFile()));
                ipField.setText(input.readLine());
                usernameField.setText(input.readLine());
                passwordField.setText(input.readLine());
                input.close();
            }
            catch(IOException e)
            {
                Main.logger.log(HexmapLogger.ERROR,
                        "Exception while reading from client info file: " + HexmapLogger.getStackTraceString(e));
            }
        }

        landingFrame.pack();
        //center frame on screen
        landingFrame.setLocationRelativeTo(null);
        landingFrame.setVisible(true);
    }


    /**
     Method to setup the Hexmap GUI. This GUI will be the main focus of this program.

     @param x
     The width of the map to make (received from server upon connection)
     @param y
     The height of the map to make (received from the server upon connection)
     */
    private void setupHexmapGUI(int x, int y)
    {
        settingUp = true;

        int radius = 25;
        Dimension hexSize = HexMapCanvas.getGridSize(x, y, radius);

        //create window and init
        hexmapMainFrame = new JFrame();
        hexmapMainFrame.setLayout(new GridBagLayout());
        hexmapMainFrame.setTitle("Hexmap");
        hexmapMainFrame.setSize(hexSize.width + 250, hexSize.height + 75);
        hexmapMainFrame.setResizable(false);

        //allow window to close from the X button
        hexmapMainFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                super.windowClosing(e);
                close();
            }
        });
        hexmapMainFrame.addMouseListener(this);

        hexmapDisplayPanel = new JPanel(new GridBagLayout());
        hexmapDisplayPanel.setSize(hexSize);
        hexmapMainFrame.add(hexmapDisplayPanel, getGBC(0, 0, 1, 1));

        hexCanvas = new HexMapCanvas(x, y, radius);
        hexCanvas.addMouseListener(this);
        hexCanvas.setOpaque(true);
        hexCanvas.setVisible(true);
        hexmapDisplayPanel.add(hexCanvas, getGBC(0, 0, 1, 1));

        chatArea = new JTextArea("Welcome to Hexmap!");
        chatArea.setSize(225, hexSize.height - 6);
        chatArea.setMinimumSize(new Dimension(225, hexSize.height - 6));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        chatAreaScroller = new JScrollPane(chatArea);
        chatAreaScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatAreaScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        AdaptiveScrollerListener scrollManager = new AdaptiveScrollerListener(chatAreaScroller);
        chatAreaScroller.getVerticalScrollBar().addAdjustmentListener(scrollManager);
        chatAreaScroller.addMouseWheelListener(scrollManager);
        chatArea.addCaretListener(scrollManager);

        hexmapDisplayPanel.add(chatAreaScroller, getGBC(1, 0, 1, 1, GridBagConstraints.BOTH));
        chatEnter = new JTextField(25);
        chatEnter.addActionListener(this);
        chatEnter.addKeyListener(this);
        hexmapDisplayPanel.add(chatEnter, getGBC(1, 1, 1, 1));

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        hexmapDisplayPanel.add(disconnectButton, getGBC(0, 1, 1, 1));

        //center frame on screen
        hexmapMainFrame.setLocationRelativeTo(null);
        hexmapMainFrame.pack();
        hexmapMainFrame.setVisible(true);

        hexmapMainFrame.setMinimumSize(hexmapMainFrame.getSize());
        hexmapDisplayPanel.setMaximumSize(hexmapDisplayPanel.getSize());
        hexCanvas.setMinimumSize(hexSize.getSize());
        chatArea.setMinimumSize(chatArea.getSize());
        chatAreaScroller.setMinimumSize(chatAreaScroller.getSize());
        chatEnter.setMinimumSize(chatEnter.getSize());
        disconnectButton.setMinimumSize(disconnectButton.getSize());

        setUp = true;
        settingUp = false;
    }

    private void registerAllMessages()
    {
        registerMessage(new ChatMessage());
        registerMessage(new CommandMessage());
        registerMessage(new MoveUnitMessage());
        registerMessage(new NewUnitMessage());
        registerMessage(new ValidationMessage());
        registerMessage(new HandshakeMessage());
        registerMessage(new CloseMessage());
        registerMessage(new InitMessage());
        registerMessage(new PingMessage());
    }

    /**
     Registers a new HexMessage into this server's message manager. We have to register via an instance due to the
     inheritance of the getName method

     @param message
     An instance of the message to register

     @return True if the message was successfully registered, false otherwise
     */
    private boolean registerMessage(HexMessage message)
    {
        if(messages.containsKey(message.getKey()))
        {
            return false;
        }

        messages.put(message.getKey(), message);
        return true;
    }

    public HexMessage getRegisteredMessage(long key)
    {
        return messages.get(key);
    }


    /**
     Inserts a new message into the chat box

     @param s
     The string to insert
     */
    public void chatAppend(String s)
    {
        chatArea.append("\n" + s);
    }


    /**
     Attempt to connect to a server with the currently entered IP and port
     */
    public void startConnection()
    {
        synchronized(connectionLock)
        {

            //organized this way to be thread safe
            if(connected)
            {
                Main.logger.log(HexmapLogger.DEBUG, "Connection attempted while connected");
                return;
            }

            try
            {
                String base = ipField.getText();
                Main.logger.log(HexmapLogger.INFO, "Attempting connection to: " + base);

                String ip;
                //default port to attempt
                int port = 7777;

                //parse given ip
                //ip is supposed to either be XXX.XXX.XXX.XXX or XXX.XXX.XXX.XXX:YYYYY
                if(base.contains(":"))
                {
                    String[] split = base.split(":");

                    //if this is true, the entered IP is invalid and the user should be notified
                    if(split.length != 2)
                    {
                        connectionDisplay.append(base + " is in invalid IP format.\n");
                        return;
                    }

                    //make sure we have a valid port
                    if(!Pattern.matches("[0-9]+", split[1]))
                    {
                        connectionDisplay.append(base + " is in invalid IP format.\n");
                        return;
                    }

                    port = Integer.parseInt(split[1]);

                    //the first section should be the ip address
                    ip = split[0];
                }

                //if no port is given, default to 7777
                else
                {
                    ip = base;
                }

                //make sure we have a valid ip address
                if(!Pattern.matches("([0-9]+\\.){3}[0-9]+", ip))
                {
                    connectionDisplay.append(base + " is in invalid IP format.\n");
                    return;
                }

                String username = usernameField.getText().trim();
                if(username.length() == 0)
                {
                    connectionDisplay.append("Must input a valid username");
                    return;
                }


                service = new Socket(ip, port);
                input = new BufferedReader(new InputStreamReader(service.getInputStream()));
                output = new PrintWriter(service.getOutputStream());

                Main.logger.log(HexmapLogger.INFO, "Made connection to " + ip + ":" + port);
                connected = true;

                //activate main thread to listen
                toListenFrom = new ConnectionListener(this, input);
                synchronized(mainThreadLock)
                {
                    mainThreadLock.notify();
                }

                //start automatic communication with server
                sendMessage(new HandshakeMessage(Main.version));
            }
            catch(IOException e1)
            {
                Main.logger.log(HexmapLogger.ERROR, "Error connecting to " + ipField.getText());
                connectionDisplay.append("Error connecting to " + ipField.getText() + "\n");
                cleanConnections();
            }
        }
    }

    /**
     Upon successful connection to server, delete the landing GUI and start the Hexmap GUI

     @param sizeX
     Width of the map, received from server
     @param sizeY
     Height of the map, received from the server
     */
    public void initConnection(int sizeX, int sizeY)
    {
        settingUp = true;
        //close the connection GUI
        landingFrame.dispose();

        //start the map GUI
        setupHexmapGUI(sizeX, sizeY);
    }

    /**
     Upon handshake to server, respond with username and password to log in
     */
    public void respondToHandshake()
    {
        String password = passwordField.getText().trim();
        if(password.length() == 0)
        {
            password = null;
        }
        sendMessage(new ValidationMessage(usernameField.getText().trim(), password));
    }

    /**
     Adds a Unit to the Hexmap.

     @param chr
     The character to add
     */
    public void addUnit(Unit chr)
    {
        hexCanvas.addUnit(chr);
    }


    /**
     Moves a unit from one map location to another

     @param UID
     The unique identifier for the unit to move
     @param toX
     The x location to move the unit to
     @param toY
     The y location to move the unit to
     @param fromX
     The current x location of the unit
     @param fromY
     The current y location of the unit
     */
    public void moveUnit(int UID, int toX, int toY, int fromX, int fromY)
    {
        ArrayList<Unit> chrs = hexCanvas.getUnits(fromX, fromY);
        Unit movedUnit = null;
        for(Unit u : chrs)
        {
            if(u.UID == UID)
            {
                movedUnit = u;
                break;
            }
        }

        if(movedUnit != null)
        {
            Main.logger.log(HexmapLogger.INFO, String.format("Unit: %s moved from %d, %d to %d, %d", movedUnit.name,
                    movedUnit.locX, movedUnit.locY, toX, toY));

            if(selectedChr != null && UID == selectedChr.UID)
            {
                hexCanvas.setHighlighted(false, selectedChr.locX, selectedChr.locY);
                selectedChr = null;
            }

            hexCanvas.moveUnit(movedUnit, toX, toY);
        }
    }

    /**
     Blocks until the main Hexmap GUI is built
     <p>
     Prevents errors when connecting to servers
     */
    public void waitForGUI()
    {
        //wait for GUI to be set up if it's not
        //shouldn't take long or block infinitely
        while(!setUp && settingUp)
        {
            try
            {
                Thread.sleep(50);
            }
            catch(InterruptedException e)
            {
            }
        }

        //how the hell did this happen
        if(!setUp)
        {
            Main.logger.log(HexmapLogger.SEVERE, "Error while waiting for UI setup.");
            close();
        }
    }

    /**
     Message interface to send to the server

     @param message
     The message to send
     */
    public void sendMessage(HexMessage message)
    {
        try
        {
            String out = Main.GSON.toJson(message.toJson(new HashSet<>())) + "\n";
            output.write(out);
            output.flush();
        }
        catch(JsonConversionException e)
        {
            Main.logger.log(HexmapLogger.ERROR,
                    "Error converting message for client: " + message + " error: " +
                            HexmapLogger.getStackTraceString(e));
        }
    }

    /**
     Callback method for the menu used to select units on the map.

     @param data
     String used to identify the selected unit, formatted UID-Xloc-Yloc
     */
    public void selectUnit(String data)
    {
        String[] s = data.split("-");

        if(s.length != 3)
        {
            Main.logger.log(HexmapLogger.ERROR, "Something broke with unit selection.");
            return;
        }

        int x = Integer.parseInt(s[1]);
        int y = Integer.parseInt(s[2]);

        //find the unit that was selected
        ArrayList<Unit> chrs = hexCanvas.getUnits(x, y);

        for(Unit c : chrs)
        {
            if(c.UID == Integer.parseInt(s[0]))
            {
                selectedChr = c;
                //this gets called from an actionListener, which comes from the GUI thread
                //so we can update the GUI here
                hexCanvas.setHighlighted(true, x, y);
            }
        }
    }

    /**
     Clear all connections, data, and configs and reset to new no matter what
     */
    private void cleanConnections()
    {
        toListenFrom = null;
        try
        {
            if(service != null)
            {
                service.close();
            }
        }
        catch(IOException e)
        {
            Main.logger.log(HexmapLogger.ERROR, "e3");
        }
        finally
        {
            service = null;
        }
    }

    /**
     Cleanly disconnect from a server if connected

     @param reason
     The given reason for terminating the connection
     */
    public void disconnect(String reason)
    {
        synchronized(connectionLock)
        {
            if(!connected)
            {
                return;
            }

            // make sure to disconnect cleanly from server
            if(!closeReceived)
            {
                sendMessage(new CloseMessage("client disconnect"));
            }

            connected = false;
            cleanConnections();
        }
        Main.logger.log(HexmapLogger.INFO, "Connection Closed: " + reason);

        if(!isClosing)
        {
            SwingUtilities.invokeLater(this::reset);
        }
    }

    /**
     Closes the GUI and exits the program
     */
    public void close()
    {
        Main.logger.log(HexmapLogger.INFO, "Closing client");

        //save configuration to file
        try
        {
            PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(infoFile.toFile())));
            output.println(ipField.getText());
            output.println(usernameField.getText());
            output.println(passwordField.getText());
            output.close();
        }
        catch(IOException e)
        {
            Main.logger.log(HexmapLogger.ERROR,
                    "Error saving to client info file: " + HexmapLogger.getStackTraceString(e));
        }

        try
        {
            if(hexmapMainFrame != null)
            {
                hexmapMainFrame.dispose();
            }
            landingFrame.dispose();
            synchronized(mainThreadLock)
            {
                toListenFrom = null;
                isClosing = true;
                disconnect("Client closed.");
                mainThreadLock.notify();
            }
        }
        catch(Exception e)
        {
            Main.logger.log(HexmapLogger.SEVERE,
                    "Exception encountered while closing: " + HexmapLogger.getStackTraceString(e));
            Main.logger.close();
            System.exit(Main.GENERAL_ERROR);
        }
    }

    /* ========================================================================================

    Event Handlers

    ========================================================================================= */
    //Handles button presses, text box enters
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if(source == connectButton)
        {
            //connecting can block, so do it in a new thread so GUI remains responsive
            new Thread(this::startConnection).start();
        }
        else if(source == disconnectButton)
        {
            disconnect("Pressed disconnect.");
        }
        else if(source == chatEnter)
        {
            String text = chatEnter.getText().trim();

            //do nothing on empty messages
            if(text.equals(""))
            {
                return;
            }

            lastMessage = text;

            //if the first unit is "/" treat as a command
            if(text.length() > 1 && text.charAt(0) == '/')
            {
                Main.logger.log(HexmapLogger.INFO, "Sent command: " + text);
                text = text.substring(1).trim();
                //clear empty commands
                if(text.length() > 0)
                {
                    String[] parts = text.split(" ", 2);
                    sendMessage(new CommandMessage(parts[0], parts.length == 2 ? parts[1] : null));
                }
            }
            //If not a command, send as a chat message
            //Username of sender is added server side
            else
            {
                Main.logger.log(HexmapLogger.INFO, "Sent message: " + text);
                sendMessage(new ChatMessage(text));
            }
            //clear entry bar
            chatEnter.setText("");
        }
    }


    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        //store start of click, as we want click'n'drag events that stay within a grid tile to be counted as
        //just a click
        if(e.getSource() == hexCanvas && e.getButton() == MouseEvent.BUTTON1)
        {
            Point mouseLocation = e.getPoint();
            Point gridLocation = hexCanvas.mapToLocation(mouseLocation);

            //point was not inside a grid tile, so do nothing
            if(gridLocation == null)
            {
                lastGridPositionClickedOn = null;
            }
            else
            {

                lastGridPositionClickedOn = gridLocation;
            }
        }
        else
        {
            lastGridPositionClickedOn = null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if(e.getSource() == hexCanvas && e.getButton() == MouseEvent.BUTTON1)
        {
            Point mouseLocation = e.getPoint();
            Point gridLocation = hexCanvas.mapToLocation(mouseLocation);

            //point was not inside a grid tile, so do nothing
            if(gridLocation == null)
            {
                lastGridPositionClickedOn = null;
            }
            //grid location mouse released on is same as grid location it was last pressed on
            //treat as single mouse click
            else if(gridLocation.equals(lastGridPositionClickedOn))
            {
                handleGridClick(gridLocation, mouseLocation);
            }
            else
            {
                lastGridPositionClickedOn = null;
            }
        }
        else
        {
            lastGridPositionClickedOn = null;
        }
    }

    private void handleGridClick(Point gridLocation, Point mouseLocation)
    {
//        Main.logger.log(HexmapLogger.INFO, String.format("Grid Clicked. X: %d, Y: %d", gridLocation.x,
//            gridLocation.y));

        // the user is trying to select a unit
        if(selectedChr == null)
        {
            ArrayList<Unit> chrs = hexCanvas.getUnits(gridLocation);

            //location has no characters, so do nothing
            if(chrs.size() == 0)
            {
                return;
            }

            //there's only one unit, so we know what to select
            if(chrs.size() == 1)
            {
                selectedChr = chrs.get(0);
                hexCanvas.setHighlighted(true, gridLocation);
                return;
            }

            //there's more than one unit, so we must have the user choose in a context menu
            PopupMenu menu = new PopupMenu();

            ActionListener listener = (event) -> selectUnit(event.getActionCommand());

            for(Unit c : chrs)
            {
                MenuItem i = new MenuItem(c.name);
                i.setActionCommand(String.format("%d-%d-%d", c.UID, c.locX, c.locY));
                i.addActionListener(listener);
                menu.add(i);
            }

            //create the menu on screen
            hexmapDisplayPanel.add(menu);
            menu.show(hexmapDisplayPanel, mouseLocation.x, mouseLocation.y);

            //this thread of execution "continues" in selectUnit()
        }
        // the user is trying to move a unit
        else
        {
            hexCanvas.setHighlighted(false, selectedChr.locX, selectedChr.locY);
            sendMessage(new MoveUnitMessage(selectedChr.UID, gridLocation.x, gridLocation.y, selectedChr.locX,
                    selectedChr.locY));
            selectedChr = null;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {

    }

    @Override
    public void mouseExited(MouseEvent e)
    {

    }

    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        //repeat last message on up arrow
        if(e.getKeyCode() == KeyEvent.VK_UP)
        {
            SwingUtilities.invokeLater(() -> chatEnter.setText(lastMessage));
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {

    }

   /* ========================================================================================

   Helper Methods

   ========================================================================================= */

    /*
    Generates GridBagConstraints (used to position elements in a Swing Grid layout)
     */
    private GridBagConstraints getGBC(int x, int y, int xSize, int ySize)
    {
        return new GridBagConstraints(x, y, xSize, ySize, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0);
    }

    private GridBagConstraints getGBC(int x, int y, int xSize, int ySize, int fill)
    {

        return new GridBagConstraints(x, y, xSize, ySize, 0.0, 0.0, GridBagConstraints.CENTER, fill,
                new Insets(0, 0, 0, 0), 0, 0);
    }

   /* ========================================================================================

   Getters and setters

   ========================================================================================= */
}
