package bthomas.hexmap.commands;

import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

public abstract class HexCommand {

	//TODO: this is not secure, ideally would abstract that all subclasses have a final String key
	public abstract String getKey();

	public abstract boolean applyFromClient(Server server, ConnectionHandler client, String command);

	public abstract boolean applyFromServer(Server server, String command);
}
