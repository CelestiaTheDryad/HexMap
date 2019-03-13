package bthomas.hexmap.server;

import bthomas.hexmap.net.HexMessage;

/**
 * This is a simple data structure to help manage incoming messages
 * by keeping track of the connection they came from
 *
 * @author Brendan Thomas
 * @since 2017-10-28
 */
public class MessageData {

    public HexMessage message;
    public ConnectionHandler source;

    public MessageData(HexMessage message, ConnectionHandler source) {
        this.message = message;
        this.source = source;
    }
}
