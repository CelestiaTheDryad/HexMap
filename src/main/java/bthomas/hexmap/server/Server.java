package bthomas.hexmap.server;

import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.commands.*;
import bthomas.hexmap.common.Unit;
import bthomas.hexmap.net.*;
import bthomas.hexmap.permissions.PermissionBase;
import bthomas.hexmap.permissions.PermissionMulti;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class contains the main server code for Hexmap. It can handle connections
 * from a theoretically infinite number of clients. It also has support for server
 * console commands.
 */
public class Server {

    public ServerSocket serverService = null;
    private boolean closing = false;
    private HashMap<String, ConnectionHandler> usernameMap = new HashMap<>();

    //server management
    private Random rand = new Random();
    private HashMap<String, HexCommand> commands = new HashMap<>();
    private PermissionMulti permissions = new PermissionMulti();
    private HashMap<String, String> passwords;

    //thread handling
    private final ReentrantLock threadHandlerLock = new ReentrantLock();
    private ArrayList<ConnectionHandler> listenerThreads = new ArrayList<>();

    //message receiving
    private ArrayDeque<MessageData> arrivalQueue = new ArrayDeque<>();
    private final ReentrantLock arrivalQueueLock = new ReentrantLock();

    //battlefield info
    private final ReentrantLock boardLock = new ReentrantLock();
    private int x = 10;
    private int y = 10;
    private HashMap<Integer, Unit> units = new HashMap<>();

    //file informations
    private Path passwordsFile = Paths.get("passwords.dat");
    public Path userPermissionsDirectory = Paths.get("permissions", "users");


    /**
     * Standard constructor, creates a server on port 7777
     */
    public Server() {
        //create directories if needed
        try {
            Files.createDirectories(userPermissionsDirectory);
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error creating permissions directories: " + HexmapLogger.getStackTraceString(e));
        }

        //load username:password map
        if(Files.exists(passwordsFile)) {
            try {
                ObjectInputStream passwordInput = new ObjectInputStream(new FileInputStream(passwordsFile.toFile()));
                passwords = (HashMap<String, String>) passwordInput.readObject();
                passwordInput.close();
            } catch (FileNotFoundException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error accessing password file: " + HexmapLogger.getStackTraceString(e));
                return;
            } catch (IOException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error reading from password file: " + HexmapLogger.getStackTraceString(e));
                return;
            } catch (ClassNotFoundException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error interpreting object from password file: " + HexmapLogger.getStackTraceString(e));
                return;
            }
        }
        else {
            passwords = new HashMap<>();
        }

        //init internet connection
        try {
            serverService = new ServerSocket(7777);
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error initializing serversocket: " + HexmapLogger.getStackTraceString(e));
            return;
        }

        new Thread(this::handleCommands).start();
        new Thread(this::handleMessages).start();

        Main.logger.log(HexmapLogger.INFO, "Server init.");
        registerAllCommands();
        registerAllPermissions();
        beginListening();
    }


    /**
     * Listen for and handle new connections
     * Runs until the server closes
     */
    private void beginListening() {
        try {
            //set timeout on ServerSocket.accept()
            serverService.setSoTimeout(250);
        }
        catch (SocketException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error setting serversocket timeout: " + HexmapLogger.getStackTraceString(e));
            return;
        }

        while (!closing) {
            try {
                //Main.logger.log("Accept");
                Socket service = serverService.accept();

                synchronized (threadHandlerLock) {

                    //race condition protection
                    if (closing) {
                        break;
                    }

                    //setup handler for new connection
                    ConnectionHandler runner = new ConnectionHandler(this, service);
                    new Thread(runner).start();

                    listenerThreads.add(runner);
                    Main.logger.log(HexmapLogger.INFO, "Made new listener. Total :" + listenerThreads.size());
                }
            }
            catch (SocketTimeoutException e) {
                //we timeout this loop every 1/4 second to check for server closes
                //perhaps there's a more elegant way to do this?
            }
            catch (IOException e) {
                if(!closing) {
                    Main.logger.log(HexmapLogger.ERROR, "error accepting connection");
                }
            }
        }
    }

    /**
     * Gets if the server has a username registered (password locked)
     *
     * @param username Yhe username to check
     * @return True if the name is locked, false if not
     */
    public boolean hasRegisteredUser(String username) {
        return passwords.containsKey(username);
    }

    /**
     * Locks a new username behind a given password
     *
     * @param username The username to lock
     * @param password The password to lock with
     * @return True if the username was locked, false otherwise
     */
    public boolean registerNewUser(String username, String password) {
        if(hasRegisteredUser(username)) {
            return false;
        }

        passwords.put(username, password);
        return true;
    }

    /**
     * Checks to see if a given username:password combination is valid on this server
     *
     * @param username The username to check
     * @param password The password to check
     * @return True if the combination is valid, false if not
     */
    public boolean validateUser(String username, String password) {
        if(password == null) {
            return false;
        }
        return passwords.get(username).equals(password);
    }

    /**
     * Registers all "vanilla" permissions for Hexmap
     */
    private void registerAllPermissions() {
        registerPermission("hexmap.commands.stop");
        registerPermission("hexmap.commands.setup");
        registerPermission("hexmap.commands.roll");
        registerPermission("hexmap.commands.addunit");
        registerPermission("hexmap.actions.chat");
        registerPermission("hexmap.actions.moveunit");
    }

    /**
     * Registers a new permission into this server's permission manager
     *
     * @param permission The permission to register, eg "op" or "hexmap.commands.roll"
     * @return True if the permission was registered successfully, false otherwise
     */
    public boolean registerPermission(String permission) {
        //reject invalid permissions
        if(!PermissionBase.genericPermissionPattern.matcher(permission).matches()) {
            return false;
        }

        String[] parts = permission.split("\\.");

        PermissionMulti manager = permissions;
        //all but the last level is a permission submanager
        for(int i = 0; i < parts.length - 1; i++) {
            manager = manager.getSubMultiOrCreate(parts[i]);
        }

        return manager.registerPermission(parts[parts.length - 1]);
    }


    /**
     * Registers all "vanilla" commands for Hexmap
     */
    private void registerAllCommands() {
        registerCommand(new RollCommand());
        registerCommand(new StopCommand());
        registerCommand(new SetupCommand());
        registerCommand(new AddUnitCommand());
    }


    /**
     * Registers a given command object
     *
     * @param command The command to register
     * @return True if the command was registered successfully, false if otherwise
     */
    public boolean registerCommand(HexCommand command) {
        //reject default keys
        if(command.getKey().equals("")) {
            return false;
        }

        //register this command if it's a new key
        if(commands.get(command.getKey()) == null) {
            commands.put(command.getKey(), command);
            return true;
        }

        //reject duplicate keys
        else {
            return false;
        }
    }


    /**
     * Handles commands typed into the server console
     * Runs infinitely until the server closes
     *
     * The blocking on keyboard.nextline() might cause some problems,
     * but none have been noticed so far
     */
    private void handleCommands() {
        Scanner keyboard = new Scanner(System.in);
        while(!closing) {
            String command = keyboard.nextLine();

            //do nothing on empty messages
            if(command.equals("")) {
                continue;
            }

            if(command.charAt(0) == '/') {
                command = command.substring(1);
            }
            String[] parts = command.split(" ", 2);

            HexCommand serverCommand = commands.get(parts[0]);
            if(serverCommand != null) {
                //use null to represent a command with no extra parts
                serverCommand.applyFromServer(this, parts.length == 2 ? parts[1] : null);
            }
            else {
                Main.logger.log(HexmapLogger.INFO, "Unknown command: " + parts[0]);
            }
        }
    }


    /**
     * Handles messages received from clients
     * Runs infinitely until the server closes
     */
    public void handleMessages() {
        while(!closing) {
            MessageData message;
            synchronized (arrivalQueueLock) {
                message = arrivalQueue.poll();
            }

            if(message != null) {
                message.message.ApplyToServer(this, message.source);
            }
            //if no message in queue, wait a bit
            else {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    //no problem
                }
            }
        }

    }


    /**
     * Checks to see if the server has a connected user with a given name
     *
     * @param username The name to check for
     * @return True if there is a connected player with that name, false if not.
     */
    public boolean hasConnectedUser(String username) {
        return usernameMap.containsKey(username);
    }


    /**
     * Adds a given unit to the board
     * Assumes the unit is valid
     *
     * @param u The unit to add
     */
    public void addUnit(Unit u) {
        synchronized (boardLock) {
            units.put(u.UID, u);
        }
    }


    /**
     * Moves a unit based on a message from a client
     * Assumes the move is valid
     * In case of clients trying to simultaneously move units, operates on first-come, first-serve
     *
     * @param message The move message to apply
     */
    public void moveUnit(MoveUnitMessage message) {
        synchronized (boardLock) {
            Unit u = units.get(message.unitUID);
            if (u != null) {
                Main.logger.log(HexmapLogger.INFO, String.format("Unit: %s moved from %d, %d to %d, %d", u.name, u.locX,
                        u.locY, message.toX, message.toY));
                u.locX = message.toX;
                u.locY = message.toY;
                sendAll(message);
            }
        }
    }


    /**
     * Sends setup instructions to a newly connected client
     *
     * @param source The connection handler to set up
     * @param username The username of the new client
     */
    public void initConnection(ConnectionHandler source, String username) {
        Main.logger.log(HexmapLogger.INFO, "Accepted new connection with name: " + username);
        source.username = username;
        usernameMap.put(username, source);
        source.setupPermissions();
        source.addMessage(new InitMessage(x, y));
        sendAll(new ChatMessage(username + " has joined."));
        //give client the map info
        synchronized (boardLock) {
            for (Unit u : units.values()) {
                source.addMessage(new NewUnitMessage(u));
            }
        }
    }


    /**
     * Sends a message to all connected clients
     *
     * @param message The message to send
     */
    public void sendAll(HexMessage message) {
        //requiring messages to bounce from client to server back to client
        //guarantees message order is the same between all clients
        synchronized (threadHandlerLock) {
            Object[] threads = listenerThreads.toArray();
            for (int i = 0; i < threads.length; i++) {
                ConnectionHandler thread = (ConnectionHandler) threads[i];
                thread.addMessage(message);
            }
        }
    }


    /**
     * Callback method for the server to receive messages
     * Adds to a queue so the server can process messages single-threaded
     *
     * @param message The message to process
     */
    public void receiveMessage(MessageData message) {
        synchronized (arrivalQueueLock) {
            arrivalQueue.add(message);
        }
    }


    /**
     * Closes a client connection
     *
     * @param listener The connection to close
     */
    public void closeListener(ConnectionHandler listener, String reason) {
        //prevent multiple closes
        if(listener.isClosed || listener.toClose) {
            return;
        }

        listener.addMessage(new CloseMessage(reason));

        if(listener.username != null) {
            usernameMap.remove(listener.username);
        }


        //nicely close handler if it's running
        listener.toClose = true;


        while(!listener.isClosed) {
            //wait a bit
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                Main.logger.log(HexmapLogger.ERROR, "Error waiting for listener to close: " + HexmapLogger.getStackTraceString(e));
                //should probably deal with this
            }
        }
        synchronized (threadHandlerLock) {
            listenerThreads.remove(listener);
            if (listener.username != null) {
                sendAll(new ChatMessage(listener.username + " has disconnected."));
                Main.logger.log(HexmapLogger.INFO, "Disconnected client: " + listener.username + " for " + reason);
            }
        }
    }


    /**
     * Nicely close the server and exit with a given status
     *
     */
    public void closeServer() {
        closing = true;

        //output passwords
        try {
            ObjectOutputStream passwordOutput = new ObjectOutputStream(new FileOutputStream(passwordsFile.toFile()));
            passwordOutput.writeObject(passwords);
            passwordOutput.close();
        }
        catch (FileNotFoundException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error opening passwords file to save passwords: " + HexmapLogger.getStackTraceString(e));
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error Opening passwords file to save passwords: " + HexmapLogger.getStackTraceString(e));
        }

        Main.logger.log(HexmapLogger.INFO, "closing server, goodbye");

        //safely close all listeners
        synchronized (threadHandlerLock) {
            while (listenerThreads.size() > 0) {
                closeListener(listenerThreads.get(0), "server closing");
            }
        }

        try {
            serverService.close();
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, HexmapLogger.getStackTraceString(e));
        }

    }

   /* ========================================================================================

   Getters and setters

   ========================================================================================= */

    public HexCommand getCommand(String key) {
        return commands.get(key);
    }

    public Random getRandom() {
        return rand;
    }

    public void setSize(int x, int y) {
        synchronized (boardLock) {
            this.x = x;
            this.y = y;
        }
    }

    public Dimension getSize() {
        return new Dimension(this.x, this.y);
    }

    public PermissionMulti getBasePermissionManager() {
        return permissions;
    }
}
