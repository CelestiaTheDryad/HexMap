package bthomas.hexmap.net;

import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

public class HandshakeMessage extends HexMessage {
	public String version;
	public String username;

	public HandshakeMessage(String version, String username) {
		this.version = version;
		this.username = username;
	}

	@Override
	public void ApplyToClient(Client client) {
		//This does not get applied to the client. See Handshake Process.txt
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		if(version.equals(Main.version) && !server.hasConnectedUser(username)) {
			source.username = username;

			server.initConnection(source, username);
		}
		else {
			source.addMessage(new CloseMessage());
		}
	}
}
