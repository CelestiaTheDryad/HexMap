package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

/**
 * This message contains all the information a new client will need to setup the Hexmap
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class InitMessage extends HexMessage{
	public int x, y, UUID;

	/**
	 * Standard constructor
	 *
	 * @param width The number of columns in the map
	 * @param height The number of rows in the map
	 * @param UUID The unique identifier assigned to this client
	 */
	public InitMessage(int width, int height, int UUID) {
		this.x = width;
		this.y = height;
		this.UUID = UUID;
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		//This does not get sent to the server, see Handshake Process.txt
	}

	@Override
	public void ApplyToClient(Client client) {
		//use the swing worker thread since the GUI will be changing
		SwingUtilities.invokeLater(() -> client.initConnection(x, y, UUID));
	}
}
