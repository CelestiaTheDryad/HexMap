
public class MessageData {

    public String message;
    public ConnectionHandler source;

    public MessageData(String message, ConnectionHandler source) {
        this.message = message;
        this.source = source;
    }
}
