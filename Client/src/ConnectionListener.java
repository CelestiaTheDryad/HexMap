import java.io.BufferedReader;
import java.io.IOException;

public class ConnectionListener implements Runnable{

    private Client parent;
    private BufferedReader input;

    public ConnectionListener(Client parent, BufferedReader input) {
        this.parent = parent;
        this.input = input;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //this blocks until a something is sent to the socket
                String text = input.readLine();

                //I don't remember why this null protection is here but I'm leaving it
                if(text != null) {
                    parent.receiveMessage(text);
                }
            }
            catch (IOException e) {
                //only a problem if parent is supposed to be connected
                if(parent.connected) {
                    System.err.println("Error reading from input stream");
                    parent.disconnect();
                }
                //something happened to the stream, close the connection
                break;
            }
        }
    }
}