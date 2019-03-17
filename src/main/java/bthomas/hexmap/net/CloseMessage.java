package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

/**
 * This message signifies one side's intention to close its connection with the other
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class CloseMessage extends HexMessage {

	public String reason;

	public CloseMessage(String reason) {
		this.reason = reason;
	}

	@Override
	public void ApplyToClient(Client client) {
		client.disconnect(reason);
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		server.closeListener(source, reason);
	}
}
