package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

/**
 * This message contains directions for moving a unit around the Hexmap
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class MoveUnitMessage extends HexMessage {
	public int unitUID, toX, toY, fromX, fromY;

	/**
	 * Standard constructor
	 *
	 * @param unitUID The unique identifier for the moving unit
	 * @param toX The X location to move the unit to
	 * @param toY The Y location to move the unit to
	 * @param fromX The current X location of the unit
	 * @param fromY The current Y location of the unit
	 */
	public MoveUnitMessage(int unitUID, int toX, int toY, int fromX, int fromY) {
		this.unitUID = unitUID;
		this.toX = toX;
		this.toY = toY;
		this.fromX = fromX;
		this.fromY = fromY;
	}

	@Override
	public void ApplyToClient(Client client) {
		client.waitForGUI();
		SwingUtilities.invokeLater(() -> client.moveUnit(unitUID, toX, toY, fromX, fromY));
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		server.moveUnit(this);
	}
}
