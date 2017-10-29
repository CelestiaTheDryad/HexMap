import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

/*
This class is the main server code for Hexmap. It can handle connections
from a theoretically infinite number of clients. It also has support for server
console commands.

Author: Brendan Thomas
 */
public class Server {

    // to use when handshaking with clients
    private static final String version = "HEXMAP 0.1";

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
    private ArrayList<Character> characters = new ArrayList<>();


    public Server() {
        try {
            //TODO: allow custom ports
            serverService = new ServerSocket(7777);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleMessages();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleCommands();
            }
        }).start();

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
                //no problem, just loop again
            }
            catch (IOException e) {
                System.err.println("Error accepting service");
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
            //add a new character
            else if(parts[0].equals("add")) {
                try {
                    String name = parts[1];
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int r = Integer.parseInt(parts[4]);
                    int g = Integer.parseInt(parts[5]);
                    int b = Integer.parseInt(parts[6]);

                    Character c = new Character(name, x, y, new Color(r, g, b));
                    boardLock.lock();
                    characters.add(c);
                    boardLock.unlock();

                    sendAll(String.format("ADD--%s-%d-%d-%d-%d-%d", name, x, y, r, g, b));
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
                String text = message.message;
                ConnectionHandler source = message.source;
                String[] parts = text.split("--");

                //message are defined to have 2 parts. Anything else is invalid.
                if(parts.length != 2) {
                    continue;
                }

                if(parts[0].equals("CLOSE")) {
                    closeListener(source);
                }

                else if(parts[0].equals("MESSAGE")) {
                    System.out.println(parts[1]);
                    sendAll(text);
                }

                else if(parts[0].equals("MOVE")) {
                    parts = parts[1].split("-");
                    String name = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int xTo = Integer.parseInt(parts[3]);
                    int yTo = Integer.parseInt(parts[4]);

                    //update internal data
                    boardLock.lock();
                    for(Character c: characters) {
                        if(c.name.equals(name) && c.locX == x && c.locY == y) {
                            c.locX = xTo;
                            c.locY = yTo;
                            break;
                        }
                    }
                    boardLock.unlock();

                    //send move to all clients
                    sendAll(text);
                }

                else if (parts[0].equals("HANDSHAKE")) {
                    //handshake message are delimited by "-"
                    parts = parts[1].split("-");

                    //initial handshake
                    if(parts[0].equals("0")) {
                        //make sure client-server version is the same
                        if (parts[1].equals(version)) {
                            //give client the map info
                            boardLock.lock();
                            source.addMessage(String.format("HANDSHAKE--1-%d-%d", x, y));
                            for(Character c: characters) {
                                //name - x - y - color RGB
                                source.addMessage(String.format("ADD--%s-%d-%d-%d-%d-%d", c.name, c.locX, c.locY, c.color.getRed(), c.color.getBlue(), c.color.getGreen()));
                            }
                            boardLock.unlock();
                        }
                        else {
                            //client wrong version, tell it to go away
                            source.addMessage("CLOSE--NULL");
                            try {
                                Thread.sleep(50);
                            }
                            catch (InterruptedException e) {
                                //no problem
                            }
                            closeListener(source);
                        }
                    }
                }

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

    /*
    Sends a given message to every connected client
     */
    private void sendAll(String message) {
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
        System.out.println("DEBUG: message received");
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
                    //no problem here
                }
            }
        }
        threadHandlerLock.lock();
        listenerThreads.remove(listener);
        System.out.println("DEBUG: Server listener closed. Total now: " + Integer.toString(listenerThreads.size()));
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
