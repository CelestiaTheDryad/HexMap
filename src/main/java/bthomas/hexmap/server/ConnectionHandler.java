package bthomas.hexmap.server;

import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.net.HexMessage;
import bthomas.hexmap.permissions.PermissionBase;
import bthomas.hexmap.permissions.PermissionMulti;
import bthomas.hexmap.permissions.PermissionSingle;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;


/*
This class handles all parts of the server's connection to one client.
It receives messages from the server to send to the client, and
from the client to send to the server.
 */

/**
 * This class handles a Hexmap server's connection to a single client
 * It receives messages from the server to send to the client,
 * and from the client to the server.
 *
 * This class also represents one user of the Hexmap server, and handles
 * owned units, permissions, and other user-specific items
 *
 * @author Brendan Thomas
 * @since 2017-10-28
 */
public class ConnectionHandler implements Runnable {

    //connection handler items
    public Server parent;
    private Socket service;

    public boolean toClose = false;
    public boolean isClosed = false;

    private ConnectionHandlerListener listener;
    private ArrayDeque<HexMessage> sendQueue = new ArrayDeque<>();
    private final ReentrantLock queueLock = new ReentrantLock();

    //user items
    public String username = null;

    //an instance of PermissionMulti in this set indicates a permission like "hexmap.commands.*"
    //an instance of PermissionSingle in this set indicates a permission like "hexmap.commands.roll"
    private HashSet<PermissionBase> permissions = new HashSet<>();


    /**
     * Standard constructor
     *
     * @param parent The server this connection belongs to and should report to
     * @param service The actual data connection to use
     */
    public ConnectionHandler(Server parent, Socket service) {
        this.parent = parent;
        this.service = service;
    }

    /**
     * Load the permissions for this user from its permissions file
     */
    public void setupPermissions() {
        Path userFile = Paths.get(parent.userPermissionsDirectory.toString(), username + ".txt");
        if(Files.exists(userFile)) {
            try {
                BufferedReader inputPermisisons = new BufferedReader(new FileReader(userFile.toFile()));
                String line = inputPermisisons.readLine();
                while(line != null) {
                    applyPermission(line);
                    line = inputPermisisons.readLine();
                }
                inputPermisisons.close();
            }
            catch (FileNotFoundException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Could not access permission file for:  "
                        + username + ": " + HexmapLogger.getStackTraceString(e));
            }
            catch (IOException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error reading from permission file for: "
                        + username + ": " + HexmapLogger.getStackTraceString(e));
            }
        }
        else {
            try {
                Files.write(userFile, new byte[0]);
            }
            catch (IOException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error creating new permission file for: "
                        + username + ": " + HexmapLogger.getStackTraceString(e));
            }
        }
    }

    /**
     * Add a single permission to this user
     *
     * @param permission The permission to add
     * @return True if the permission was successfully added, false otherwise
     */
    private boolean applyPermission(String permission) {
        //reject invalid permissions
        if(!PermissionBase.inputPermission.matcher(permission).matches()) {
            Main.logger.log(HexmapLogger.ERROR, "Tried to load invalid permission: " + permission
                    + " to user: " + username);
            return false;
        }

        //get the lowest level permission manager for this permission
        PermissionMulti manager = parent.getBasePermissionManager();
        String[] parts = permission.split("\\.");
        for(int i = 0; i < parts.length - 1; i++) {
            manager = manager.getSubManagerOrFail(parts[i]);
            //reject unregistered permissions
            if(manager == null) {
                Main.logger.log(HexmapLogger.ERROR, "Attempt to apply unregistered permission: "
                        + permission + " to user: " + username);
                return false;
            }
        }

        //for generic permissions, we are done here
        if(permission.contains("*")) {
            permissions.add(manager);
            return true;
        }

        //get the final permission single for this permission
        PermissionSingle end = manager.getPermissionOrFail(parts[parts.length - 1]);
        //reject unregistered permissions
        if(end == null) {
            Main.logger.log(HexmapLogger.ERROR, "Attempt to apply unregistered permission: "
                    + permission + " to user: " + username);
            return false;
        }

        permissions.add(end);
        return true;
    }

    /**
     * Checks to see if the user has a given permission.
     *
     * @param permission The permission to check the user for
     * @return True if the user has that permission, false if they do not or the permission is invalid
     */
    public boolean hasPermission(String permission) {
        //reject invalid permissions
        if(!PermissionBase.genericPermissionPattern.matcher(permission).matches()) {
            Main.logger.log(HexmapLogger.ERROR, "Tried to check invalid permission: " + permission
                    + " to user: " + username);
            return false;
        }

        //get the lowest level permission manager for this permission
        PermissionMulti manager = parent.getBasePermissionManager();
        String[] parts = permission.split("\\.");
        for(int i = 0; i < parts.length - 1; i++) {
            //check if user contains a generic permission containing the requested permission
            if(permissions.contains(manager)) {
                return true;
            }

            manager = manager.getSubManagerOrFail(parts[i]);
            //reject unregistered permissions
            if(manager == null) {
                Main.logger.log(HexmapLogger.ERROR, "Attempt to check unregistered permission: "
                        + permission + " to user: " + username);
                return false;
            }
        }

        //get the final permission single for this permission
        PermissionSingle end = manager.getPermissionOrFail(parts[parts.length - 1]);
        //reject unregistered permissions
        if(end == null) {
            Main.logger.log(HexmapLogger.ERROR, "Attempt to check unregistered permission: "
                    + permission + " to user: " + username);
            return false;
        }

        return permissions.contains(end);
    }



    public void run() {
        //set up connection stuff
        //if there's an error, mark as broken

        //There's apparently some wizardry with these streams that doesn't exist with text streams
        //The output stream must be created first on one side, and the input stream first on the other
        //The server makes the input stream first
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(service.getInputStream());
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error creating connection data reader: " + HexmapLogger.getStackTraceString(e));
            isClosed = true;
            parent.closeListener(this, "Error creating connection data reader");
            return;
        }

        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(service.getOutputStream());
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error creating connection data output: " + HexmapLogger.getStackTraceString(e));
            isClosed = true;
            parent.closeListener(this,"Error creating connection data reader");
            return;
        }

        //start listener
        listener = new ConnectionHandlerListener(this, input);
        new Thread(listener).start();

        //send messages to client until handler is closed
        while(!toClose) {
            //send a message to the client if one is in the queue
            if(sendQueue.size() > 0) {
                HexMessage message;
                synchronized (queueLock) {
                    message = sendQueue.poll();
                }
                sendMessage(output, message);
            }
            else {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    //no problem
                }
            }
        }

        listener.stopped = true;

        try {
            output.close();
            input.close();
            service.close();
        }
        catch (IOException e) {
            //no problem cleanly closing only preferred
        }

        isClosed = true;


    }

    /**
     * Sends a message to the client
     *
     * @param output The connection to send the message on
     * @param message The message to send
     */
    private void sendMessage(ObjectOutputStream output, HexMessage message) {
        try {
            output.writeObject(message);
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.ERROR, "Error sending message to client: " + HexmapLogger.getStackTraceString(e));
        }
    }

    /**
     * Adds a message to be sent to this connection
     *
     * @param message The message to add
     */
    public void addMessage(HexMessage message) {
        synchronized (queueLock) {
            sendQueue.add(message);
        }
    }
}
