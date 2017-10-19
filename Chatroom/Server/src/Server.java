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
This class is the main server code for chatroom. It can handle connections
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


    public Server() {
        try {
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

            if(command.equals("stop")) {
                closeServer(0);
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

                    //send message to all clients
                    //requiring messages to bounce from client to server back to client
                    //guarantees message order is the same between all clients
                    threadHandlerLock.lock();
                    Object[] threads = listenerThreads.toArray();
                    for(int i = 0; i < threads.length; i++) {
                        ConnectionHandler thread = (ConnectionHandler) threads[i];
                        thread.addMessage(text);
                    }
                    threadHandlerLock.unlock();
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
