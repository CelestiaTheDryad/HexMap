package bthomas.hexmap.net;

import bthomas.hexmap.common.Unit;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

/**
 * This message contains directions for creating a new unit
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class NewUnitMessage extends HexMessage{
	public Unit unit;

	/**
	 * Standard constructor
	 *
	 * @param unit The unit to be created
	 */
	public NewUnitMessage(Unit unit) {
		this.unit = unit;
	}

	@Override
	public void ApplyToClient(Client client) {
		client.waitForGUI();
		SwingUtilities.invokeLater(() -> client.addUnit(unit));
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		//For server-client safety, clients cannot directly create new characters
		//They will use commands instead
	}
}
