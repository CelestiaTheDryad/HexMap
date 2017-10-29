import java.io.BufferedReader;
import java.io.IOException;

/*
This class handles messages coming in from the client and sends them to the server for processing.
 */
public class ConnectionHandlerListener implements Runnable{

    private ConnectionHandler parent;
    private BufferedReader input;

    public boolean stopped = false;

    public ConnectionHandlerListener(ConnectionHandler parent, BufferedReader input) {
        this.parent = parent;
        this.input = input;
    }

    public void run() {
        while (true) {
            try {
                String text = input.readLine();
                if(text != null) {
                    //send message to server with source
                    parent.parent.receiveMessage(new MessageData(text, parent));
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
        }
    }
}
