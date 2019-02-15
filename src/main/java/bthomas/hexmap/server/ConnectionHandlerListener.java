package bthomas.hexmap.server;

import bthomas.hexmap.net.HexMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;

/*
This class handles messages coming in from the client and sends them to the server for processing.
 */
public class ConnectionHandlerListener implements Runnable{

    private ConnectionHandler parent;
    private ObjectInputStream input;

    public boolean stopped = false;

    public ConnectionHandlerListener(ConnectionHandler parent, ObjectInputStream input) {
        this.parent = parent;
        this.input = input;
    }

    public void run() {
        while (true) {
            try {
                HexMessage message = (HexMessage) input.readObject();
                if(message != null) {
                    //send message to server with source
                    parent.parent.receiveMessage(new MessageData(message, parent));
                }
            }
            catch (IOException e) {
                //not actually an error if socket was supposed to be closed
                if(!stopped) {
                    System.err.println("Error reading from input stream");
                    parent.parent.closeListener(parent);
                }
                //something happened to the stream, close the connection
                break;
            }
            catch (ClassNotFoundException e) {
                System.out.println("Error reading message from client.");
                e.printStackTrace();
            }
        }
    }
}
