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
 * This message contains all the information a new client will need to setup the Hexmap
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class InitMessage extends HexMessage{
	private static final long serialVersionUID = 8433404537519147519L;
	private final int x, y;
	private static final String xKey = "x";
	private static final String yKey = "y";

	/**
	 * Standard constructor
	 *
	 * @param width The number of columns in the map
	 * @param height The number of rows in the map
	 */
	public InitMessage(int width, int height) {
		this.x = width;
		this.y = height;
	}

	/**
	 * Dummy constructor to use with JSON serialization
	 */
	public InitMessage()
	{
		this(-1, -1);
	}

	/**
	 Constructs an object from JSON representation

	 @param root
	 The JsonObject containing the data for this object.

	 @throws JsonConversionException
	 If there is not proper data stored in the JsonObject
	 */
	public InitMessage(JsonObject root) throws JsonConversionException
	{
		x = JsonUtils.getInt(root, xKey);
		y = JsonUtils.getInt(root, yKey);
	}

	@Override
	public InitMessage fromJson(JsonObject root) throws JsonConversionException
	{
		return new InitMessage(root);
	}

	@Override
	public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
	{
		root.addProperty(xKey, x);
		root.addProperty(yKey, y);
		super.buildJson(root, loopDetector);
	}

	@Override
	public void applyToClient(Client client) {
		//use the swing worker thread since the GUI will be changing
		SwingUtilities.invokeLater(() -> client.initConnection(x, y));
	}

	@Override
	public void applyToServer(Server server, ConnectionHandler source) {
		//This does not get sent to the server, see Handshake Process.txt
	}

	@Override
	public long getKey() {
		return serialVersionUID;
	}
}
