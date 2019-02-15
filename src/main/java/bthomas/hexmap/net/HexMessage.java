package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import java.io.Serializable;

/***
 * Base class for HexMap networking.
 * Inherit from this class to add new communication types.
 *
 */
public abstract class HexMessage implements Serializable {
	public abstract void ApplyToServer(Server server, ConnectionHandler source);

	public abstract void ApplyToClient(Client client);
}
