package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

public class InitMessage extends HexMessage{
	public int x, y, UUID;

	public InitMessage(int x, int y, int UUID) {
		this.x = x;
		this.y = y;
		this.UUID = UUID;
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		//This does not get sent to the server, see Handshake Process.txt
	}

	@Override
	public void ApplyToClient(Client client) {
		SwingUtilities.invokeLater(() -> client.initConnection(x, y, UUID));
	}
}
