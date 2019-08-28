package bthomas.hexmap.common.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import java.util.HashSet;

/**
 * This message signifies one side's intention to close its connection with the other
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class CloseMessage extends HexMessage {

	private static final long serialVersionUID = -1449713013647322515L;

	public final String reason;
	private static final String reasonKey = "reason";

	public CloseMessage(String reason) {
		this.reason = reason;
	}

	/**
	 * Dummy constructor to use with JSON serialization
	 */
	public CloseMessage()
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
	public CloseMessage(JsonObject root) throws JsonConversionException
	{
		reason = JsonUtils.getString(root, reasonKey);
	}

	@Override
	public CloseMessage fromJson(JsonObject root) throws JsonConversionException
	{
		return new CloseMessage(root);
	}

	@Override
	public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
	{
		root.addProperty(reasonKey, reason);
		super.buildJson(root, loopDetector);
	}

	@Override
	public void applyToClient(Client client) {
		client.disconnect(reason);
	}

	@Override
	public void applyToServer(Server server, ConnectionHandler source) {
		server.closeListener(source, reason);
	}

	@Override
	public long getKey() {
		return serialVersionUID;
	}
}
