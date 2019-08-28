package bthomas.hexmap.common.net;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import java.util.HashSet;

/**
 * This message is used to initialize the connection between client and server,
 * and make sure they are running compatible versions.
 *
 * Do not change this class, that will cause crashes when clients try to connect with the wrong version.
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class HandshakeMessage extends HexMessage {
	private static final long serialVersionUID = 8682067740878305603L;
	public final String version;
	private static final String versionKey = "version";

	/**
	 * Standard constructor
	 *
	 * @param version The version of the client
	 */
	public HandshakeMessage(String version) {
		this.version = version;
	}

	/**
	 * Dummy constructor to use with JSON serialization
	 */
	public HandshakeMessage()
	{
		this((String) null);
	}

	/**
	 Constructs an object from JSON representation

	 @param root
	 The JsonObject containing the data for this object.

	 @throws JsonConversionException
	 If there is not proper data stored in the JsonObject
	 */
	public HandshakeMessage(JsonObject root) throws JsonConversionException
	{
		version = JsonUtils.getString(root, versionKey);
	}

	@Override
	public HandshakeMessage fromJson(JsonObject root) throws JsonConversionException
	{
		return new HandshakeMessage(root);
	}

	@Override
	public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
	{
		root.addProperty(versionKey, version);
		super.buildJson(root, loopDetector);
	}

	@Override
	public void applyToClient(Client client) {
		Main.logger.log(HexmapLogger.INFO, "Received handshake from server.");
		client.respondToHandshake();
	}

	@Override
	public void applyToServer(Server server, ConnectionHandler source) {
		if(!version.equals(Main.version)) {
			String reason = "wrong version: " + version + ".";
			Main.logger.log(HexmapLogger.INFO, "Rejected connection for: " + reason);
			source.addMessage(new CloseMessage(reason));
		}
		source.addMessage(new HandshakeMessage(Main.version));
	}

	@Override
	public long getKey() {
		return serialVersionUID;
	}
}
