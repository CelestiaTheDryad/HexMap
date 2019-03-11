package bthomas.hexmap.server;

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
public class ConnectionHandler implements Runnable {

    public Server parent;
    private Socket service;

    public boolean toClose = false;
    public boolean isClosed = false;
    public String username;

    private ConnectionHandlerListener listener;
    private ArrayDeque<HexMessage> sendQueue = new ArrayDeque<>();
    private final ReentrantLock queueLock = new ReentrantLock();


    public ConnectionHandler(Server parent, Socket service) {
        this.parent = parent;
        this.service = service;
    }



    public void run() {
        //set up connection stuff
        //if there's an error, mark as broken
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(service.getInputStream());
        }
        catch (IOException e) {
            System.err.println("Error creating listening data reader");
            isClosed = true;
            parent.closeListener(this);
            return;
        }

        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(service.getOutputStream());
        }
        catch (IOException e) {
            System.err.println("Error creating connection data output");
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
                queueLock.lock();
                HexMessage message = sendQueue.poll();
                queueLock.unlock();
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

    public void sendMessage(ObjectOutputStream output, HexMessage message) {
        try {
            output.writeObject(message);
        }
        catch (IOException e) {
            System.out.println("Error sending message to client.");
            e.printStackTrace();
        }
    }

    public void addMessage(HexMessage message) {
        queueLock.lock();
        sendQueue.add(message);
        queueLock.unlock();
    }
}
