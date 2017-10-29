import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/*
This class is the main GUI and networking code for the client side
of the RPG Hexmap program.

This program is currently meant for private use only. As such, it
has absolutely no security precautions in place to protect information.

Author: Brendan Thomas
Date: October 20, 2017
 */
public class Client implements ActionListener, MouseListener {

    //to use when handshaking with server
    private static final String version = "HEXMAP 0.1";

    private Socket service = null;
    private BufferedReader input = null;
    private PrintStream output = null;

    public boolean connected = false;
    private boolean closing = false;
    private boolean closeReceived = false;
    private boolean settingUp = false;
    private boolean setUp = false;

    private final ReentrantLock connectionLock = new ReentrantLock();

    private ArrayDeque<String> messages = new ArrayDeque<>();
    private final ReentrantLock messagesLock = new ReentrantLock();

    //Landing GUI elements
    private JFrame landingFrame;
    private JPanel landingPanel;
    private JTextField ipField;
    private JButton connectButton;
    private JTextField usernameField;
    private JLabel ipLabel;
    private JLabel usernameLabel;
    private JTextArea chatDisplay;
    private JTextField chatEnter;
    private JScrollPane displayPane;

    //Hexmap UI elements
    private Frame hexmapMainFrame;
    private Panel hexmapDisplayPanel;
    private HexMapCanvas hexCanvas;

    //event handling variables
    private Character selectedChr = null;



    public Client () {
        //start Swing UI code and create landing GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setupConnectionGUI();
            }
        });

        //start message handler
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleMessages();
            }
        }).start();
    }


    /*
    Method to set up the initial landing UI. This UI will
    simply get a username and server info to connect the client.
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

        chatDisplay = new JTextArea(10, 30);
        chatDisplay.setEditable(false);
        chatDisplay.setLineWrap(true);
        chatDisplay.setWrapStyleWord(true);

        GridBagConstraints displayPaneC = getGBC(0, 2, 3, 1);
        displayPane = new JScrollPane(chatDisplay);
        displayPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        landingPanel.add(displayPane, displayPaneC);

        GridBagConstraints chatEnterC = getGBC(0, 3, 3, 1);
        chatEnter = new JTextField();
        chatEnter.setPreferredSize(new Dimension(350, 24));
        chatEnter.addActionListener(this);
        landingPanel.add(chatEnter, chatEnterC);

        landingFrame.pack();
        landingFrame.setLocationRelativeTo(null);
        landingFrame.setVisible(true);
    }


    /*
    Method to setup the Hexmap GUI. This GUI will be the main focus of this program.
     */
    private void setupHexmapGUI(int x, int y) {
        settingUp = true;

        int radius = 25;
        Dimension hexSize = HexMapCanvas.getGridSize(x, y, radius);

        //create window and init
        hexmapMainFrame = new Frame();
        hexmapMainFrame.setLayout(new GridBagLayout());
        hexmapMainFrame.setTitle("Hexmap");
        hexmapMainFrame.setSize(hexSize.width + 150, hexSize.height + 50);

        //allow window to close
        hexmapMainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(0);
            }
        });

        hexmapDisplayPanel = new Panel(new GridBagLayout());
        hexmapDisplayPanel.setSize(hexSize);
        hexmapMainFrame.add(hexmapDisplayPanel, getGBC(0, 0, 1, 1));

        hexCanvas = new HexMapCanvas(x, y, radius);
        hexCanvas.addMouseListener(this);

        hexmapDisplayPanel.add(hexCanvas, getGBC(0, 0, 1, 1));

        hexmapMainFrame.setVisible(true);

        setUp = true;
        settingUp = false;


    }


    /*
    attempt to connect to a server with currently entered IP
    if not already connected.

    Thread safe.
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
                    chatDisplay.append(base + " is in invalid IP format.\n");
                    return;
                }

                //make sure we have a valid port
                if(!Pattern.matches("[0-9]+", split[1])) {
                    chatDisplay.append(base+ " is in invalid IP format.\n");
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
                chatDisplay.append(base + " is in invalid IP format.\n");
                return;
            }



            service = new Socket(ip, port);
            input = new BufferedReader(new InputStreamReader(service.getInputStream()));
            output = new PrintStream(service.getOutputStream());

            new Thread(new ConnectionListener(this, input)).start();

            System.out.println("Made connection");
            connected = true;

            //start automatic communication with server
            sendMessage("HANDSHAKE--0-" + version);
        }
        catch (IOException e1) {
            chatDisplay.append("Error connecting to " + ipField.getText() + "\n");
            cleanConnections();
        }
        finally {
            connectionLock.unlock();
        }
    }


    /*
    Callback method for server listener. Adds message to processing queue.

    Thread safe
     */
    public void receiveMessage(String message) {
        messagesLock.lock();
        messages.add(message);
        messagesLock.unlock();
    }


    /*
    Main message handling method. To be called in a separate thread from main.
    This method blocks until the client closes.
    While technically threadsafe, it is expected that there is only one running
    instance of this method.
     */
    public void handleMessages() {
        //TODO make this loop actually end or ensure that it doesn't need to
        while(!closing) {
            messagesLock.lock();
            String message = messages.poll();
            messagesLock.unlock();

            if(message != null) {
                System.out.println("Received: " + message);
                String[] parts = message.split("--");

                //messages are currently defined to have two parts
                //anything else is invalid
                if(parts.length != 2) {
                    continue;
                }

                //received new message for chatroom
                if(parts[0].equals("MESSAGE")) {
                    //handle messages
                }
                else if(parts[0].equals("HANDSHAKE")) {
                    parts = parts[1].split("-");

                    //version check was good and now receiving board
                    if(parts[0].equals("1")) {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        //with successful handshake, we can move the the hexmap GUI
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                settingUp = true;
                                //close the connection GUI
                                landingFrame.dispose();

                                //start he map GUI
                                setupHexmapGUI(x, y);
                            }
                        });
                    }
                }
                //adding a new character to the board
                else if(parts[0].equals("ADD")) {
                    waitForGUI();

                    //create character from message
                    parts = parts[1].split("-");
                    String name = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int r = Integer.parseInt(parts[3]);
                    int g = Integer.parseInt(parts[4]);
                    int b = Integer.parseInt(parts[5]);
                    Character chr = new Character(name, x, y, new Color(r, g, b));

                    //since we're changing the GUI, run in worker thread
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hexCanvas.addCharacter(chr);
                        }
                    });
                }
                else if(parts[0].equals("MOVE")) {
                    parts = parts[1].split("-");
                    String name = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int xTo = Integer.parseInt(parts[3]);
                    int yTo = Integer.parseInt(parts[4]);

                    ArrayList<Character> chrs = hexCanvas.getCharacters(x, y);
                    for(Character c: chrs) {
                        if(c.name.equals(name)) {
                            //since we're changing the GUI, run in worker thread
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    hexCanvas.moveCharacter(c, xTo, yTo);
                                }
                            });

                        }
                    }

                }
                //received disconnect notification from server
                else if(parts[0].equals("CLOSE")) {
                    closeReceived = true;
                    disconnect();
                }
            }
            else {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    //interruption is no problem
                }
            }
        }
    }

    /*
    Used to hold actions until the hexmap GUI is done being set up

    THis prevent errors when first connecting to the server
     */
    private void waitForGUI() {
        //wait for GUI to be set up if it's not
        //shouldn't take long or block infinitely
        while (!setUp && settingUp) {
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                //no problem
            }
        }

        //how the hell did this happen
        if(!setUp) {
            System.exit(1);
        }
    }


    /*
    Generic method to send messages to connected server
     */
    private void sendMessage(String message) {
        if(output != null) {
            output.println(message);
        }
    }

    /*
    callback method used by menu that selects a character
     */
    public void selectChr(String data) {
        //TODO: Character can have - in their name, which will break this. Fix.
        String[] s = data.split("-");

        if(s.length != 3) {
            System.out.println("Something broke with character selection.");
            return;
        }

        int x = Integer.parseInt(s[1]);
        int y = Integer.parseInt(s[2]);

        //find the character that was selected
        ArrayList<Character> chrs = hexCanvas.getCharacters(x, y);

        for(Character c: chrs) {
            if(c.name.equals(s[0])) {
                selectedChr = c;
                //this gets called from an actionListener, which comes from the GUI thread
                //so we can update the GUI here
                hexCanvas.setHighlighted(true, x, y);
            }
        }
    }


    /*
    Event handler method for the client GUI.
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if(source == connectButton) {
            //connecting can block, so do it in a new thread so GUI remains responsive
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startConnection();
                }
            }).start();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == hexCanvas) {
            Point location = hexCanvas.mapToLocation(e.getPoint());

            //point was not inside a grid tile, so do nothing
            if(location == null) {
                return;
            }

            System.out.println(String.format("Grid Clicked. X: %d, Y: %d", location.x, location.y));

            // the user is trying to select a character
            if(selectedChr == null) {
                ArrayList<Character> chrs = hexCanvas.getCharacters(location);

                //location has no characters, so do nothing
                if(chrs.size() == 0) {
                    return;
                }

                //there's only one character, so we know what to select
                if(chrs.size() == 1) {
                    selectedChr = chrs.get(0);
                    hexCanvas.setHighlighted(true, location);
                    return;
                }

                //there's more than one character, so we must have the user choose in a context menu
                PopupMenu menu = new PopupMenu();

                //create each menu item and add callback support
                ActionListener listener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectChr(e.getActionCommand());
                    }
                };

                for(Character c: chrs) {
                    MenuItem i = new MenuItem(c.name);
                    i.setActionCommand(String.format("%s-%d-%d", c.name, c.locX, c.locY));
                    i.addActionListener(listener);
                    menu.add(i);
                }

                //create the menu on screen
                hexmapDisplayPanel.add(menu);
                menu.show(hexmapDisplayPanel, e.getX(), e.getY());

                //this thread of execution "continues" in selectChr()
            }
            // the user is trying to move a character
            else{
                //TODO: This should start communication with the server before updating state
                hexCanvas.setHighlighted(false, selectedChr.locX, selectedChr.locY);
                sendMessage(String.format("MOVE--%s-%d-%d-%d-%d", selectedChr.name, selectedChr.locX, selectedChr.locY, location.x, location.y));
                //hexCanvas.moveCharacter(selectedChr, location);
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


    /*
    clear all connections data/configs and reset to new no matter what.
    */
    private void cleanConnections() {

        if(output != null) {
            output.close();
        }
        output = null;

        try {
            if(input != null) {
                input.close();
            }
        }
        catch (IOException e) {
            System.out.println("e1");
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
            System.out.println("e2");
        }
        finally {
            service = null;
        }
    }


    /*
    cleanly disconnect from a server if connected to one

    Thread safe
    */
    public void disconnect() {
        connectionLock.lock();
        if(!connected) {
            connectionLock.unlock();
            return;
        }

        // make sure to disconnect cleanly from server
        if(!closeReceived) {
            sendMessage("CLOSE--null");
            closeReceived = false;
        }
        connected = false;
        cleanConnections();
        connectionLock.unlock();
        System.out.println("Connection Closed");
        System.exit(0);
    }


    /*
    Helper method for organizing UI elements, many of these options
    are not used, so this method hides them to keep code clean
     */
    private GridBagConstraints getGBC(int x, int y, int xSize, int ySize) {
        return new GridBagConstraints(x, y, xSize, ySize, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    }


    public static void main(String[] args) {
        Client main = new Client();
    }
}
