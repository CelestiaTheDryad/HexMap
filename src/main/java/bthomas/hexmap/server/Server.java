package bthomas.hexmap.server;

import bthomas.hexmap.Main;
import bthomas.hexmap.common.Unit;
import bthomas.hexmap.net.HexMessage;
import bthomas.hexmap.net.InitMessage;
import bthomas.hexmap.net.MoveUnitMessage;
import bthomas.hexmap.net.NewUnitMessage;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

/*
This class is the main server code for Hexmap. It can handle connections
from a theoretically infinite number of clients. It also has support for server
console commands.

Author: Brendan Thomas
 */
public class Server {

    public ServerSocket serverService = null;
    private boolean closing = false;

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


    public Server() {
        try {
            //TODO: allow custom ports
            serverService = new ServerSocket(7777);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        new Thread(this::handleCommands).start();
        new Thread(this::handleMessages).start();

        System.out.println("Server init.");
        beginListening();
    }


    /*
    Method to find and handle clients attempting to connect to the server.
    This method runs infinitely until the server closes.
     */
    private void beginListening() {
        try {
            //set timeout on ServerSocket.accept()
            serverService.setSoTimeout(250);
        }
        catch (SocketException e) {
            System.err.println("ServerSocket error");
            return;
        }

        while (!closing) {
            try {
                //System.out.println("Accept");
                Socket service = serverService.accept();
                System.out.println("Accepted");

                threadHandlerLock.lock();

                //race condition protection
                if(closing) {
                    threadHandlerLock.unlock();
                    break;
                }

                //setup handler for new connection
                ConnectionHandler runner = new ConnectionHandler(this, service);
                new Thread(runner).start();

                listenerThreads.add(runner);
                System.out.println("Made new listener. Total :" + Integer.toString(listenerThreads.size()));
                threadHandlerLock.unlock();
            }
            catch (SocketTimeoutException e) {
                //we timeout this loop every 1/4 second to check for server closes
                //perhaps there's a more elegant way to do this?
            }
            catch (IOException e) {
                if(!closing) {
                    System.err.println("Error accepting service");
                }
            }
        }
    }


    /*
    Method to handle console commands. This should be run on its own thread.
    This method runs infinitely until the server closes.

    The blocking on keyboard.nextline() might cause problems,
    but none have been noticed so far.
     */
    public void handleCommands() {
        Scanner keyboard = new Scanner(System.in);
        while(!closing) {
            String command = keyboard.nextLine();
            String[] parts = command.split(" ");

            if(parts[0].equals("stop")) {
                closeServer(0);
            }
            // make the board to a given size
            else if(parts[0].equals("setup")) {
                try {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    boardLock.lock();
                    this.x = x;
                    this.y = y;
                    boardLock.unlock();
                }
                catch (Exception e) {
                    System.out.println("Invalid command \"" + command + "\"" );
                }
            }
            //add a new unit
            else if(parts[0].equals("add")) {
                try {
                    String name = parts[1];
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int r = Integer.parseInt(parts[4]);
                    int g = Integer.parseInt(parts[5]);
                    int b = Integer.parseInt(parts[6]);

                    Unit u = new Unit(name, x, y, new Color(r, g, b));
                    boardLock.lock();
                    units.put(u.UID, u);
                    boardLock.unlock();

                    sendAll(new NewUnitMessage(u));
                }
                catch (Exception e) {
                    System.out.println("Invalid command \"" + command + "\"" );
                }
            }
            else {
                System.out.println("Unknown command \"" + command + "\"" );
            }
        }
    }


    /*
    Method to handle incoming messages. This should be run on its own thread.
    This method runs infinitely until the server closes.
     */
    public void handleMessages() {
        while(!closing) {
            arrivalQueueLock.lock();
            MessageData message = arrivalQueue.poll();
            arrivalQueueLock.unlock();

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

    public void moveUnit(MoveUnitMessage message) {
        boardLock.lock();
        Unit u = units.get(message.unitUID);
        if(u != null) {
            u.locX = message.toX;
            u.locY = message.toY;
            sendAll(message);
        }
        boardLock.unlock();
    }

    public void initConnection(ConnectionHandler source) {
        //give client the map info
        boardLock.lock();
        source.addMessage(new InitMessage(x, y, -1));
        for(Unit u: units.values()) {
            source.addMessage(new NewUnitMessage(u));
        }
        boardLock.unlock();
    }

    /*
    Sends a given message to every connected client
     */
    public void sendAll(HexMessage message) {
        //requiring messages to bounce from client to server back to client
        //guarantees message order is the same between all clients
        threadHandlerLock.lock();
        Object[] threads = listenerThreads.toArray();
        for(int i = 0; i < threads.length; i++) {
            ConnectionHandler thread = (ConnectionHandler) threads[i];
            thread.addMessage(message);
        }
        threadHandlerLock.unlock();
    }


    /*
    Callback method for server to receive messages from connection handlers.
     */
    public void receiveMessage(MessageData message) {
        //System.out.println("DEBUG: message received");
        arrivalQueueLock.lock();
        arrivalQueue.add(message);
        arrivalQueueLock.unlock();
    }


    /*
    Closes a given connection handler from both normal and already errored states.
     */
    public void closeListener(ConnectionHandler listener) {
        //nicely close handler if it's running
        if(!listener.isClosed) {
            listener.toClose = true;


            while(!listener.isClosed) {
                //wait a bit
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    //TODO: figure out what to do with this
                    //Not sure what the various causes are, can be called manually
                    //can also be caused from outside (from OS?) in which case I think program should shut down immediately
                }
            }
        }
        threadHandlerLock.lock();
        listenerThreads.remove(listener);
        System.out.println("DEBUG: Server listener closed. Total now: " + listenerThreads.size());
        threadHandlerLock.unlock();
    }


    /*
    Nicely closes the server and exits with a given status.
     */
    private void closeServer(int retStat) {
        closing = true;

        System.out.println("DEBUG: closing server");

        //safely close all listeners
        threadHandlerLock.lock();
        while(listenerThreads.size() > 0) {
            closeListener(listenerThreads.get(0));
        }
        threadHandlerLock.unlock();

        try {
            serverService.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(99);
        }

        System.exit(retStat);

    }


    public static void main(String[] args) {
        Server server = new Server();
    }
}
