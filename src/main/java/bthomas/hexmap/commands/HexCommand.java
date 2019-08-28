package bthomas.hexmap.commands;

import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.common.net.ChatMessage;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

/**
 * Base class for Hexmap commands
 * Inherit from this class to implement new commands
 *
 * @author Brenda Thomas
 * @since 2019-03-11
 */
public abstract class HexCommand {

	/**
	 * Gets the name used for this command (see Command process.txt)
	 * This key needs to be unique for each command and should never change
	 *
	 * @return The name for this command
	 */
	public abstract String getName();

	/**
	 * Gets a description of how to use this command, using light regex format
	 *
	 * @return The discription string for this command
	 */
	public abstract String getDescription();

	/**
	 * Applies this command to a server, keeping track of who entered the command
	 *
	 * @param server The server to apply the command on
	 * @param client The client sending the command
	 * @param command The remainder of the command test, or null if there was no additional text
	 * @return True if the command could be successfully executed, false otherwise
	 */
	public abstract boolean applyFromClient(Server server, ConnectionHandler client, String command);

	/**
	 * Applies this command to a server, knowing the command came from the server console
	 *
	 * @param server The server to apply the command on
	 * @param command The remainder of the command test, or null if there was no additional text
	 * @return True if the command could be successfully executed, false otherwise
	 */
	public abstract boolean applyFromServer(Server server, String command);

	/**
	 * Default behavior for when a invalid command is entered into the server console
	 *
	 * @param source The server where the command was entered
	 * @param command The remainder of the command after the key
	 */
	public final void respondToNoMatch(Server source, String command) {
		//log to server and print to server console
		Main.logger.log(HexmapLogger.INFO, getDefaultRejectMessage(command));
	}

	/**
	 * Default behavior when a client enters an invalid command
	 *
	 * @param source The client connection that the command originated from
	 * @param command The remainder of the command after the key
	 */
	public final void respondToNoMatch(ConnectionHandler source, String command) {
		//log command attempt and send notification to client
		Main.logger.log(HexmapLogger.INFO, source.username + " attempted invalid command: \"/" + getName()
				+ (command != null ? " " + command : "") + "\"");
		source.addMessage(new ChatMessage(getDefaultRejectMessage(command)));
	}

	/**
	 * default behavior when a client tries to use a command without the correct permission
	 *
	 * @param source The client attempting the command
	 * @param command The remainder of the command they entered
	 */
	public final void respondToNoPermission(ConnectionHandler source, String command) {
		source.addMessage(new ChatMessage("You do not have permission to use command: " + getName()));
		Main.logger.log(HexmapLogger.INFO, source.username + " attempted to use command without permissions: " + getName());
	}

	/**
	 * The default error message given for an invalid command
	 *
	 * @param command The remainder of the command after the key
	 * @return The error message for this command
	 */
	public final String getDefaultRejectMessage(String command) {
		return "Invalid command: \"/" + getName() + (command != null ? " " + command : "")
				+ "\". Input in form \"" + getDescription() + "\".";
	}
}
