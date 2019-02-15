package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

public class CloseMessage extends HexMessage {
	@Override
	public void ApplyToClient(Client client) {
		client.disconnect();
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		server.closeListener(source);
	}
}
