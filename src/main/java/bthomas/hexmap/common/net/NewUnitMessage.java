package bthomas.hexmap.common.net;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.common.Unit;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.util.HashSet;

/**
 * This message contains directions for creating a new unit
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class NewUnitMessage extends HexMessage{
	private static final long serialVersionUID = 5062758621873907775L;
	public final Unit unit;
	private static final String unitKey = "unit";

	/**
	 * Standard constructor
	 *
	 * @param unit The unit to be created
	 */
	public NewUnitMessage(Unit unit) {
		this.unit = unit;
	}

	/**
	 * Dummy constructor to use with JSON serialization
	 */
	public NewUnitMessage()
	{
		this((Unit) null);
	}

	/**
	 Constructs an object from JSON representation

	 @param root
	 The JsonObject containing the data for this object.

	 @throws JsonConversionException
	 If there is not proper data stored in the JsonObject
	 */
	public NewUnitMessage(JsonObject root) throws JsonConversionException
	{
		JsonObject unitJson = JsonUtils.getJsonObject(root, unitKey);
		unit = unitJson == null ? null : new Unit(unitJson);
	}

	@Override
	public NewUnitMessage fromJson(JsonObject root) throws JsonConversionException
	{
		return new NewUnitMessage(root);
	}

	@Override
	public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
	{
		super.buildJson(root, loopDetector);
		root.add(unitKey, unit.toJson(loopDetector));
	}

	@Override
	public void applyToClient(Client client) {
		Main.logger.log(HexmapLogger.INFO, "Server: added unit: " + unit.name + " at: " + unit.locX + ", "
				+ unit.locY + " with color: " + unit.color.getRed() + " " + unit.color.getGreen() + " " + unit.color.getBlue() + ".");
		client.waitForGUI();
		SwingUtilities.invokeLater(() -> client.addUnit(unit));
	}

	@Override
	public long getKey() {
		return serialVersionUID;
	}

	@Override
	public void applyToServer(Server server, ConnectionHandler source) {
		//For server-client safety, clients cannot directly create new characters
		//They will use commands instead
	}
}
