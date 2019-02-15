package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

public class MoveUnitMessage extends HexMessage {
	public int unitUID, toX, toY, fromX, fromY;

	public MoveUnitMessage(int unitUID, int toX, int toY, int fromX, int fromY) {
		this.unitUID = unitUID;
		this.toX = toX;
		this.toY = toY;
		this.fromX = fromX;
		this.fromY = fromY;
	}

	@Override
	public void ApplyToClient(Client client) {
		client.moveUnit(unitUID, toX, toY, fromX, fromY);
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		server.moveUnit(this);
	}
}
