import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;


public class Server {

    public ServerSocket serverService = null;
    private boolean closing = false;
    private boolean closed = false;

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

                System.out.println("Got lock");

                //race condition protection
                if(closing) {
                    threadHandlerLock.unlock();
                    break;
                }

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

    public void handleMessages() {
        while(!closing) {
            arrivalQueueLock.lock();
            MessageData message = arrivalQueue.poll();
            arrivalQueueLock.unlock();
            if(message != null) {
                String text = message.message;
                ConnectionHandler source = message.source;
                String[] parts = text.split("--");

                if(parts.length != 2) {
                    continue;
                }

                if(parts[0].equals("CLOSE")) {
                    closeListener(source);
                }
                else if(parts[0].equals("MESSAGE")) {
                    System.out.println(parts[1]);
                    threadHandlerLock.lock();
                    Object[] threads = listenerThreads.toArray();
                    for(int i = 0; i < threads.length; i++) {
                        ConnectionHandler thread = (ConnectionHandler) threads[i];
                        thread.addMessage(text);
                    }
                    threadHandlerLock.unlock();
                }

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

    }

    public void receiveMessage(MessageData message) {
        System.out.println("DEBUG: message received");
        arrivalQueueLock.lock();
        arrivalQueue.add(message);
        arrivalQueueLock.unlock();
    }

    public void closeListener(ConnectionHandler listener) {
        if(!listener.isClosed) {
            listener.toClose = true;


            while(!listener.isClosed) {
                //wait
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
}
