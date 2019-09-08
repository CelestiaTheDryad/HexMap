package bthomas.hexmap.server;

import bthomas.hexmap.Main;
import bthomas.hexmap.commands.AddUnitCommand;
import bthomas.hexmap.commands.HexCommand;
import bthomas.hexmap.commands.RollCommand;
import bthomas.hexmap.commands.SetupCommand;
import bthomas.hexmap.commands.StopCommand;
import bthomas.hexmap.common.Unit;
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
import bthomas.hexmap.permissions.PermissionBase;
import bthomas.hexmap.permissions.PermissionMulti;
import bthomas.hexmap.permissions.PermissionSingle;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
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
    private HashMap<Long, HexMessage> messages = new HashMap<>();
    private PermissionMulti permissions = new PermissionMulti();
    private HashMap<String, HashSet<PermissionBase>> permissionGroups = new HashMap<>();
    private HashMap<String, String> passwords;

    //thread handling
    private final ArrayList<ConnectionHandler> listenerThreads = new ArrayList<>();

    //message receiving
    private final ArrayDeque<MessageData> arrivalQueue = new ArrayDeque<>();

    //battlefield info
    private final ReentrantLock boardLock = new ReentrantLock();
    private int x = 10;
    private int y = 10;
    private HashMap<Integer, Unit> units = new HashMap<>();

    //file informations
    private Path passwordsFile = Paths.get("passwords.dat");
    private Path groupPermissionsDirectory = Paths.get("permissions", "groups");
    public Path userPermissionsDirectory = Paths.get("permissions", "users");


    /**
     * Standard constructor, creates a server on port 7777
     */
    public Server() {
        //create directories if needed
        try {
            Files.createDirectories(userPermissionsDirectory);
            Files.createDirectories(groupPermissionsDirectory);
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

        Main.logger.log(HexmapLogger.INFO, "Server init.");
        registerAllCommands();
        registerAllPermissions();
        registerAllMessages();
        loadPermissionGroups();
    }

    public void run()
    {
        Thread consoleThread = new Thread(this::handleCommands);
        consoleThread.setDaemon(true);
        consoleThread.start();
        Thread receiveThread = new Thread(this::handleMessages);
        receiveThread.setDaemon(true);
        receiveThread.start();
        Main.scheduleTask(new ServerKeepaliveManager(this), System.currentTimeMillis() + ServerKeepaliveManager.PING_INTERVAL_MILLIS);
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

                synchronized (listenerThreads) {

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
     * Registers a new HexMessage into this server's message manager.
     * We have to register via an instance due to the inheritance of the getName method
     *
     * @param message An instance of the message to register
     * @return True if the message was successfully registered, false otherwise
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
     * Loads all group permissions from files
     */
    private void loadPermissionGroups() {
        //create default group if needed
        Path groupFile = Paths.get(groupPermissionsDirectory.toString(), "default.txt");
        if(!Files.exists(groupFile)) {
            try {
                Files.write(groupFile, new byte[0]);
            }
            catch (IOException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error creating default group permissions file: " + HexmapLogger.getStackTraceString(e));
            }
        }

        //try to load each file in the groups folder as a permissions file
        try {
            DirectoryStream<Path> groups = Files.newDirectoryStream(groupPermissionsDirectory, "*.txt");
            for(Path group : groups) {
                String fileName = group.toFile().getName();
                try {
                    HashSet<PermissionBase> newGroup = new HashSet<>();
                    BufferedReader inputPermissions = new BufferedReader(new FileReader(group.toFile()));

                    //apply each line to the group
                    String line = inputPermissions.readLine();
                    while(line != null) {
                        applyPermission(line, fileName, newGroup);
                        line = inputPermissions.readLine();
                    }
                    inputPermissions.close();

                    //add group to group list
                    permissionGroups.put(fileName, newGroup);
                }
                catch (IOException e) {
                    Main.logger.log(HexmapLogger.SEVERE, "Error reading from permission file for group: "
                            + fileName + ": " + HexmapLogger.getStackTraceString(e));
                }
            }
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error creating list of permissions group files: " + HexmapLogger.getStackTraceString(e));
        }
    }

    /**
     * Applies a single permission to a given group
     *
     * @param permission The permission to apply
     * @param groupName The name of the group (used for logging only)
     * @param group The group to apply the permission to
     * @return True if the permission was applied successfully, false otherwise
     */
    private boolean applyPermission(String permission, String groupName, HashSet<PermissionBase> group) {
        //reject invalid permissions
        if(!PermissionBase.inputPermission.matcher(permission).matches()) {
            Main.logger.log(HexmapLogger.ERROR, "Tried to load invalid permission: " + permission
                    + " to group: " + groupName);
            return false;
        }

        //get the lowest level permission manager for this permission
        PermissionMulti manager = getBasePermissionManager();
        String[] parts = permission.split("\\.");
        for(int i = 0; i < parts.length - 1; i++) {
            manager = manager.getSubManagerOrFail(parts[i]);
            //reject unregistered permissions
            if(manager == null) {
                Main.logger.log(HexmapLogger.ERROR, "Attempt to apply unregistered permission: "
                        + permission + " to group: " + groupName);
                return false;
            }
        }

        //for generic permissions, we are done here
        if(permission.contains("*")) {
            group.add(manager);
            return true;
        }

        //get the final permission single for this permission
        PermissionSingle end = manager.getPermissionOrFail(parts[parts.length - 1]);
        //reject unregistered permissions
        if(end == null) {
            Main.logger.log(HexmapLogger.ERROR, "Attempt to apply unregistered permission: "
                    + permission + " to group: " + groupName);
            return false;
        }

        group.add(end);
        return true;
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
        if(command.getName().equals("")) {
            return false;
        }

        //register this command if it's a new key
        if(commands.get(command.getName()) == null) {
            commands.put(command.getName(), command);
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
            synchronized (arrivalQueue) {
                message = arrivalQueue.poll();
            }

            if(message != null) {
                message.message.applyToServer(this, message.source);
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
        synchronized (listenerThreads)
        {
            listenerThreads.forEach(client -> client.addMessage(message));
        }
    }


    /**
     * Callback method for the server to receive messages
     * Adds to a queue so the server can process messages single-threaded
     *
     * @param message The message to process
     */
    public void receiveMessage(MessageData message) {
        synchronized (arrivalQueue) {
            arrivalQueue.add(message);
        }
    }


    /**
     * Closes a client connection
     *
     * @param listener The connection to close
     * @param reason The given reason for terminating the connection
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
        synchronized (listenerThreads) {
            listenerThreads.remove(listener);
            if (listener.username != null) {
                sendAll(new ChatMessage(listener.username + " has disconnected."));
                Main.logger.log(HexmapLogger.INFO, "Disconnected client: " + listener.username + " for: " + reason);
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
        synchronized (listenerThreads) {
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

    public HexCommand getCommand(String name) {
        return commands.get(name);
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

    public ConnectionHandler[] getClients()
    {
        synchronized(listenerThreads)
        {
            return listenerThreads.toArray(new ConnectionHandler[0]);
        }
    }

    public Dimension getSize() {
        return new Dimension(this.x, this.y);
    }

    public PermissionMulti getBasePermissionManager() {
        return permissions;
    }

    public HashSet<PermissionBase> getGroupPermissions(String groupName) {
        return permissionGroups.get(groupName);
    }
}
