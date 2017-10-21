import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayDeque;
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
public class Client implements ActionListener {

    private Socket service = null;
    private BufferedReader input = null;
    private PrintStream output = null;

    public boolean connected = false;
    private boolean closeReceived = false;

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



    public Client () {
        //start Swing UI code and create landing GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setupConnectionGUI();
            }
        });
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
    private void setupHexmapGUI() {

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
    Generic method to send messages to connected server
     */
    private void sendMessage(String message) {
        if(output != null) {
            output.println(message);
        }
    }


    /*
    Event handler method for the client GUI.
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if(source == connectButton) {
            //connecting can block, so do it in a new thread
            //so GUI remains responsive
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startConnection();
                }
            }).start();

            /*
            TODO: This code doesn't belong here but I haven't set up the section for it

            //is only true if a connection was successfully made
            if(connected){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setupHexmapGUI();
                    }
                });
            }
            */
        }
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
