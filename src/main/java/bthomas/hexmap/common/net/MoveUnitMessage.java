package bthomas.hexmap.common.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.util.HashSet;

/**
 * This message contains directions for moving a unit around the Hexmap
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class MoveUnitMessage extends HexMessage {
	private static final long serialVersionUID = -7316619218970310328L;
	public final int unitUID, toX, toY, fromX, fromY;
	private static final String uidKey = "uid";
	private static final String toXKey = "toX";
	private static final String toYKey = "toY";
	private static final String fromXKey = "fromX";
	private static final String fromYKey = "fromY";

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

	/**
	 * Dummy constructor to use with JSON serialization
	 */
	public MoveUnitMessage()
	{
		this(-1, -1, -1, -1, -1);
	}

	/**
	 Constructs an object from JSON representation

	 @param root
	 The JsonObject containing the data for this object.

	 @throws JsonConversionException
	 If there is not proper data stored in the JsonObject
	 */
	public MoveUnitMessage(JsonObject root) throws JsonConversionException
	{
		unitUID = JsonUtils.getInt(root, uidKey);
		toY = JsonUtils.getInt(root, toYKey);
		toX = JsonUtils.getInt(root, toXKey);
		fromX = JsonUtils.getInt(root, fromXKey);
		fromY = JsonUtils.getInt(root, fromYKey);
	}

	@Override
	public MoveUnitMessage fromJson(JsonObject root) throws JsonConversionException
	{
		return new MoveUnitMessage(root);
	}

	@Override
	public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
	{
		root.addProperty(uidKey, unitUID);
		root.addProperty(toXKey, toX);
		root.addProperty(toYKey, toY);
		root.addProperty(fromXKey, fromX);
		root.addProperty(fromYKey, fromY);
		super.buildJson(root, loopDetector);
	}

	@Override
	public void applyToClient(Client client) {
		client.waitForGUI();
		SwingUtilities.invokeLater(() -> client.moveUnit(unitUID, toX, toY, fromX, fromY));
	}

	@Override
	public void applyToServer(Server server, ConnectionHandler source) {
		server.moveUnit(this);
	}

	@Override
	public long getKey() {
		return serialVersionUID;
	}
}
