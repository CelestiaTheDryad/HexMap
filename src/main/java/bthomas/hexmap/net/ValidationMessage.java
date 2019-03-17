package bthomas.hexmap.net;

import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

public class ValidationMessage extends HexMessage {

	public String username;
	public String password;

	public ValidationMessage(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public ValidationMessage(String username) {
		this(username, null);
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

		//reject duplicate logins
		if(server.hasConnectedUser(username)) {
			String reason = "requested duplicate username: " + username;
			Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + reason);
			server.closeListener(source, reason);
		}

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

	@Override
	public void ApplyToClient(Client client) {

	}
}
