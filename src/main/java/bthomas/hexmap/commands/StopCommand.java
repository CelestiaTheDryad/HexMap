package bthomas.hexmap.commands;

import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

/**
 * This command shuts down the Hexmap server
 *
 * @author Brenda Thomas
 * @since 2019-03-11
 */
public class StopCommand extends HexCommand {

	@Override
	public boolean applyFromServer(Server server, String command) {
		server.closeServer(0);
		return true;
	}

	@Override
	public boolean applyFromClient(Server server, ConnectionHandler client, String command) {
		//OP command
		return false;
	}

	@Override
	public String getKey() {
		return "stop";
	}
}
