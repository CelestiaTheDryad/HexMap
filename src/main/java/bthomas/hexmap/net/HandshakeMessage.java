package bthomas.hexmap.net;

import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

/**
 * This message is used to initialize the connection between client and server,
 * and make sure they are running compatible versions.
 *
 * Do not change this class, that will cause crashes when clients try to connect with the wrong version.
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class HandshakeMessage extends HexMessage {
	public String version;

	/**
	 * Standard constructor
	 *
	 * @param version The version of the client
	 */
	public HandshakeMessage(String version) {
		this.version = version;
	}

	@Override
	public void ApplyToClient(Client client) {
		Main.logger.log(HexmapLogger.INFO, "Received handshake from server.");
		client.respondToHandshake();
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		if(!version.equals(Main.version)) {
			String reason = "wrong version: " + version + ".";
			Main.logger.log(HexmapLogger.INFO, "Rejected connection for: " + reason);
			source.addMessage(new CloseMessage(reason));
		}
		source.addMessage(new HandshakeMessage(Main.version));
	}
}
