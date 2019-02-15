package bthomas.hexmap.server;

import bthomas.hexmap.net.HexMessage;

/*
This is a simple data structure to help manage
incoming messages for the server by keeping track
of the receiver.
 */
public class MessageData {

    public HexMessage message;
    public ConnectionHandler source;

    public MessageData(HexMessage message, ConnectionHandler source) {
        this.message = message;
        this.source = source;
    }
}
