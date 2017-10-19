/*
This is a simple data structure to help manage
incoming messages for the server by keeping track
of the receiver.
 */
public class MessageData {

    public String message;
    public ConnectionHandler source;

    public MessageData(String message, ConnectionHandler source) {
        this.message = message;
        this.source = source;
    }
}
