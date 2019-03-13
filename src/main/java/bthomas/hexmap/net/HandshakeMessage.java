package bthomas.hexmap.net;

import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

/**
 * This message is used to initialize the connection between client and server,
 * and make sure they are running compatible versions
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class HandshakeMessage extends HexMessage {
	public String version;
	public String username;

	/**
	 * Standard constructor
	 *
	 * @param version The version of the client
	 * @param username The username requested by the client
	 */
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
