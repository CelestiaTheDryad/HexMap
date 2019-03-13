package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import java.io.Serializable;

/***
 * Base class for HexMap networking.
 * Inherit from this class to add new communication types.
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public abstract class HexMessage implements Serializable {

	/**
	 * Apply this message to a server
	 * @param server The server to apply to
	 * @param source The connection that this message came from
	 */
	public abstract void ApplyToServer(Server server, ConnectionHandler source);

	/**
	 * Apply this message to a client
	 *
	 * @param client The client to apply to
	 */
	public abstract void ApplyToClient(Client client);
}
