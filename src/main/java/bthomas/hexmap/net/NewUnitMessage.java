package bthomas.hexmap.net;

import bthomas.hexmap.common.Unit;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

public class NewUnitMessage extends HexMessage{
	public Unit unit;

	public NewUnitMessage(Unit unit) {
		this.unit = unit;
	}

	@Override
	public void ApplyToClient(Client client) {
		client.waitForGUI();
		SwingUtilities.invokeLater(() -> client.addCharacter(unit));
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		//For server-client safety, clients cannot directly create new characters
	}
}
