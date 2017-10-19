import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler implements Runnable {

    public Server parent;
    private Socket service = null;

    public boolean toClose = false;
    public boolean isClosed = false;
    public Condition notify= null;

    private ConnectionHandlerListener listener;
    private ArrayDeque<String> sendQueue = new ArrayDeque<>();
    private final ReentrantLock queueLock = new ReentrantLock();


    public ConnectionHandler(Server parent, Socket service) {
        this.parent = parent;
        this.service = service;
    }

    public void run() {
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(service.getInputStream()));
        }
        catch (IOException e) {
            System.err.println("Error creating listening data reader");
            isClosed = true;
            return;
        }

        PrintStream output = null;
        try {
            output = new PrintStream(service.getOutputStream());
        }
        catch (IOException e) {
            System.err.println("Error creating connection data output");
            isClosed = true;
            return;
        }

        listener = new ConnectionHandlerListener(this, input);
        new Thread(listener).start();

        while(!toClose) {
            //send a message to the client if one is in the queue
            if(sendQueue.size() > 0) {
                queueLock.lock();
                String message = sendQueue.poll();
                queueLock.unlock();
                output.println(message);
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

    public void addMessage(String message) {
        queueLock.lock();
        sendQueue.add(message);
        queueLock.unlock();
    }
}
