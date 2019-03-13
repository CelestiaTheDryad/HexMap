package bthomas.hexmap.client;

import bthomas.hexmap.Logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.net.HexMessage;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * This class handles incoming data from the connected server
 *
 * @author Brendan Thomas
 * @since 2017-10-21
 */
public class ConnectionListener implements Runnable{

    private Client parent;
    private ObjectInputStream input;

    /**
     * The standard constructor
     *
     * @param parent The Hexmap client this listener is used for
     * @param input The data input stream to listen to
     */
    public ConnectionListener(Client parent, ObjectInputStream input) {
        this.parent = parent;
        this.input = input;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //this blocks until a something is sent to the socket
                HexMessage message = (HexMessage) input.readObject();
                if(message != null) {
                    message.ApplyToClient(parent);
                }
            }
            catch (IOException e) {
                //only a problem if parent is supposed to be connected
                if(parent.connected) {
                    Main.logger.log(HexmapLogger.SEVERE, "Error reading from input stream: " + e.toString());
                    parent.disconnect();
                }
                //something happened to the stream, close the connection
                break;
            }
            catch (ClassNotFoundException e) {
                Main.logger.log(HexmapLogger.SEVERE, "Error reading object from server: " + e.toString());
            }
        }
    }
}
