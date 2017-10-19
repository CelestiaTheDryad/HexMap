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

public class Client implements ActionListener{

    private Socket service = null;
    private BufferedReader input = null;
    private PrintStream output = null;
    private boolean closing = false;

    public boolean connected = false;
    private boolean closeReceived = false;

    private final ReentrantLock connectionLock = new ReentrantLock();

    private ArrayDeque<String> messages = new ArrayDeque<>();
    private final ReentrantLock messagesLock = new ReentrantLock();

    private JFrame mainFrame;
    private JPanel mainPanel;

    private JTextField ipField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JTextField usernameField;
    private JLabel ipLabel;
    private JLabel usernameLabel;
    private JTextArea chatDisplay;
    private JTextField chatEnter;
    private JScrollPane displayPane;

    public Client() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleMessages();
            }
        }).start();
    }

    public void setupUI() {
        mainFrame = new JFrame("Chatroom Client");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new TitledBorder(new EtchedBorder(), "Test Chatroom"));
        mainFrame.add(mainPanel);

        GridBagConstraints ipLabelC = getGBC(0, 0, 1, 1);
        ipLabelC.insets = new Insets(0, 8, 0, 8);
        ipLabel = new JLabel("Server IP:");
        mainPanel.add(ipLabel, ipLabelC);

        GridBagConstraints ipFieldC = getGBC(1, 0, 1, 1);
        ipField = new JTextField();
        ipField.setPreferredSize(new Dimension(150, 24));
        mainPanel.add(ipField, ipFieldC);

        GridBagConstraints connectButtonC = getGBC(2, 0, 1, 1);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        mainPanel.add(connectButton, connectButtonC);

        GridBagConstraints usernameLabelC = getGBC(0, 1, 1, 1);
        usernameLabelC.insets = new Insets(0, 8, 0, 8);
        usernameLabel = new JLabel("Username:");
        mainPanel.add(usernameLabel, usernameLabelC);

        GridBagConstraints disconnectButtonC = getGBC(2, 1, 1, 1);
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        mainPanel.add(disconnectButton, disconnectButtonC);

        GridBagConstraints usernameFieldC = getGBC(1, 1, 1, 1);
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(150, 24));
        mainPanel.add(usernameField, usernameFieldC);

        chatDisplay = new JTextArea(10, 30);
        chatDisplay.setEditable(false);
        chatDisplay.setLineWrap(true);
        chatDisplay.setWrapStyleWord(true);

        GridBagConstraints displayPaneC = getGBC(0, 2, 3, 1);
        displayPane = new JScrollPane(chatDisplay);
        displayPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(displayPane, displayPaneC);

        GridBagConstraints chatEnterC = getGBC(0, 3, 3, 1);
        chatEnter = new JTextField();
        chatEnter.setPreferredSize(new Dimension(350, 24));
        chatEnter.addActionListener(this);
        mainPanel.add(chatEnter, chatEnterC);

        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }


    /*
    Helper method for organizing UI elements, many of these options
    are not used, so this method hides them to keep code clean
     */
    private GridBagConstraints getGBC(int x, int y, int xSize, int ySize) {
        return new GridBagConstraints(x, y, xSize, ySize, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    }


    /*
    Event handler method for chatroom, handles all events from UI elements
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if(source == chatEnter) {
            System.out.println("Text Enter Action");
            String name = usernameField.getText();
            String text = chatEnter.getText();

            //ensure proper information before sending message to server
            if(text.equals("")) {
                return;
            }

            if (name.equals("")) {
                chatDisplay.append("Please enter a username before chatting.\n");
                return;
            }

            sendMessage("MESSAGE--"+ name + ": " + text);
            chatEnter.setText("");

        }
        else if(source == connectButton) {
            System.out.println("Connect button Action");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    startConnection();
                }
            }).start();
        }
        else if(source == disconnectButton) {
            System.out.println("Disconnect Button Action");
            disconnect();
        }
    }


    /*
    Callback method for server listener. Adds message to processing queue.
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
                String[] parts = message.split("--");
                System.out.println("Received: " + message);

                //messages are currently defined to have two parts
                //anything else is invalid
                if(parts.length != 2) {
                    continue;
                }

                //received new message for chatroom
                if(parts[0].equals("MESSAGE")) {
                    //TODO make this use invokelater() so it doesn't break the UI
                    //append message to text bar
                    //keep text scrolled to bottom if at bottom
                    //else keep looking at current location
                    //TODO this currently doesn't work properly
                    JScrollBar bar = displayPane.getVerticalScrollBar();
                    chatDisplay.append(parts[1] + "\n");

                    try {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e) {
                        //no prob
                    }

                    chatDisplay.revalidate();
                    displayPane.revalidate();
                    bar.revalidate();
                    bar.setValue(bar.getMaximum() - bar.getVisibleAmount());
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
    Generic method to send messages to connected server
     */
    private void sendMessage(String message) {
        if(output != null) {
            output.println(message);
        }
    }


    /*
    attempt to connect to a server with currently entered IP
    if not already connected
    */
    public void startConnection() {
        System.out.println("Lock signal");
        connectionLock.lock();

        if(connected) {
            System.out.println("Connection attempted while connected");
            connectionLock.unlock();
            return;
        }

        try {
            System.out.println("attempting connection");
            //TODO allow custom ports
            service = new Socket(ipField.getText(), 7777);
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

    public static void main(String[] args) {
        Client main = new Client();

        //have to update UI on swing worker thread or else problems
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                main.setupUI();
            }
        });

        /* Alternative ways to handle the thread. IDK why people would ever use this syntax
        SwingUtilities.invokeLater(
                () -> main.setupUI()
        );

        SwingUtilities.invokeLater(main::setupUI);
        */
    }
}
