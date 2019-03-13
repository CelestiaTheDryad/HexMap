package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.commands.HexCommand;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

/**
 * This message contains any commands entered by a client
 *
 * @author Brendan Thomas
 * @since 2019-03-11
 */
public class CommandMessage extends HexMessage {

	private String key;
	private String command;


	/**
	 * Standard constructor
	 *
	 * @param key The key used to identify the desired command
	 * @param command All other remaining parts of the command
	 */
	public CommandMessage (String key, String command) {
		this.key = key;
		this.command = command;
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		HexCommand serverCommand = server.getCommand(key);
		if(serverCommand != null) {
			serverCommand.applyFromClient(server, source, command);
		}
		else {
			source.addMessage(new ChatMessage("invalid command: " + key));
		}
	}

	@Override
	public void ApplyToClient(Client client) {
		//clients do not receive commands, see command process.txt
	}
}
