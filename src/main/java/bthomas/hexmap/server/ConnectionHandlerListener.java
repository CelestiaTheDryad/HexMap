package bthomas.hexmap.server;

import bthomas.hexmap.Logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.net.HexMessage;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * This class handles incoming data from a client connection
 *
 * @author Brendan Thomas
 * @since 2017-10-28
 */
public class ConnectionHandlerListener implements Runnable{

    private ConnectionHandler parent;
    private ObjectInputStream input;

    public boolean stopped = false;

    /**
     *
     * @param parent The main handler for this connection
     * @param input THe input stream to read from
     */
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
                    Main.logger.log(HexmapLogger.SEVERE, "Error reading from input stream");
                    parent.parent.closeListener(parent);
                }
                //something happened to the stream, close the connection
                break;
            }
            catch (ClassNotFoundException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error reading object from client: " + e.toString());
            }
        }
    }
}
