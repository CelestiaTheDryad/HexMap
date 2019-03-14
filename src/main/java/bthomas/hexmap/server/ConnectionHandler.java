package bthomas.hexmap.server;

import bthomas.hexmap.Logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.net.HexMessage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;


/*
This class handles all parts of the server's connection to one client.
It receives messages from the server to send to the client, and
from the client to send to the server.
 */

/**
 * This class handles a Hexmap server's connection to a single client
 * It receives messages from the server to send to the client,
 * and from the client to the server
 *
 * @author Brendan Thomas
 * @since 2017-10-28
 */
public class ConnectionHandler implements Runnable {

    public Server parent;
    private Socket service;

    public boolean toClose = false;
    public boolean isClosed = false;
    public String username = null;

    private ConnectionHandlerListener listener;
    private ArrayDeque<HexMessage> sendQueue = new ArrayDeque<>();
    private final ReentrantLock queueLock = new ReentrantLock();


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
            Main.logger.log(HexmapLogger.SEVERE, "Error creating listening data reader");
            isClosed = true;
            parent.closeListener(this);
            return;
        }

        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(service.getOutputStream());
        }
        catch (IOException e) {
            Main.logger.log(HexmapLogger.SEVERE, "Error creating connection data output");
            isClosed = true;
            parent.closeListener(this);
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
            Main.logger.log(HexmapLogger.ERROR, "Error sending message to client: " + e.toString());
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
