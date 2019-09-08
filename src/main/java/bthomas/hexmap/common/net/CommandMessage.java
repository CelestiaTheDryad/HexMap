package bthomas.hexmap.common.net;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.commands.HexCommand;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import java.util.HashSet;

/**
 * This message contains any commands entered by a client
 *
 * @author Brendan Thomas
 * @since 2019-03-11
 */
public class CommandMessage extends HexMessage {

	private static final long serialVersionUID = 1925172692968220602L;

	private final String name;
	private final static String nameKey = "name";
	private final String command;
	private final static String commandKey = "command";


	/**
	 * Standard constructor
	 *
	 * @param name The name used to identify the desired command
	 * @param command All other remaining parts of the command
	 */
	public CommandMessage (String name, String command) {
		this.name = name;
		this.command = command;
	}

	/**
	 * Dummy constructor to use with JSON serialization
	 */
	public CommandMessage()
	{
		this(null, null);
	}

	/**
	 Constructs an object from JSON representation

	 @param root
	 The JsonObject containing the data for this object.

	 @throws JsonConversionException
	 If there is not proper data stored in the JsonObject
	 */
	public CommandMessage(JsonObject root) throws JsonConversionException
	{
		name = JsonUtils.getString(root, nameKey);
		command = JsonUtils.getString(root, commandKey);
	}

	@Override
	public CommandMessage fromJson(JsonObject root) throws JsonConversionException
	{
		return new CommandMessage(root);
	}

	@Override
	public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
	{
		super.buildJson(root, loopDetector);
		root.addProperty(nameKey, name);
		root.addProperty(commandKey, command);
	}

	@Override
	public void applyToServer(Server server, ConnectionHandler source) {
		HexCommand serverCommand = server.getCommand(name);
		if(serverCommand != null) {
			serverCommand.applyFromClient(server, source, command);
		}
		else {
			String response = "invalid command: \"" + name + "\"";
			Main.logger.log(HexmapLogger.INFO, source.username + " attempted " + response);
			source.addMessage(new ChatMessage(response));
		}
	}

	@Override
	public void applyToClient(Client client) {
		//clients do not receive commands, see command process.txt
	}

	@Override
	public long getKey() {
		return serialVersionUID;
	}
}
