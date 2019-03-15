package bthomas.hexmap.net;

import bthomas.hexmap.logging.HexmapLogger;
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
	public String password;

	/**
	 * Standard constructor
	 *
	 * @param version The version of the client
	 * @param username The username requested by the client
	 */
	public HandshakeMessage(String version, String username) {
		this.version = version;
		this.username = username;
		this.password = null;
	}

	/**
	 * Constructor with password
	 *
	 * @param version The version of the client
	 * @param username The username requested by the client
	 * @param password The password given by the client
	 */
	public HandshakeMessage(String version, String username, String password) {
		this.version = version;
		this.username = username;
		this.password = password;
	}

	@Override
	public void ApplyToClient(Client client) {
		//This does not get applied to the client. See Handshake Process.txt
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		username = username.trim();

		//reject blank usernames
		if(username.length() == 0) {
			String reason = "invalid username";
			Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + reason);
			server.closeListener(source, reason);
		}

		if(version.equals(Main.version) && !server.hasConnectedUser(username)) {
			//validate username/password
			if(!server.hasRegisteredUser(username)) {
				if(password != null) {
					server.registerNewUser(username, password);
				}
				server.initConnection(source, username);
			}
			else if(server.validateUser(username, password)) {
				server.initConnection(source, username);
			}
			else {
				String reason = "invalid password";
				Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + username + " " + reason);
				server.closeListener(source, reason);
			}
		}
		else {
			String reason;
			if(!version.equals(Main.version)) {
				reason = "incorrect version: " + version;
			}
			else if(server.hasConnectedUser(username)) {
				reason = "requested duplicate username: " + username;
			}
			else {
				reason = "unknown reason";
			}
			Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + reason);
			server.closeListener(source, reason);
		}
	}
}
