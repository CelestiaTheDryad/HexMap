package bthomas.hexmap.commands;

import bthomas.hexmap.Logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.net.ChatMessage;
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
	 * Gets the key used for this command (see Command process.txt)
	 * This key needs to be unique for each command and should never change
	 *
	 * @return The key for this command
	 */
	public abstract String getKey();

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
	public void respondToNoMatch(Server source, String command) {
		//log to server and print to server console
		Main.logger.log(HexmapLogger.INFO, getDefaultRejectMessage(command));
	}

	/**
	 * Default behavior when a client enters an invalid command
	 *
	 * @param source The client connection that the command originated from
	 * @param command The remainder of the command after the key
	 */
	public void respondToNoMatch(ConnectionHandler source, String command) {
		//log command attempt and send notification to client
		Main.logger.log(HexmapLogger.INFO, source.username + " attempted invalid command: \"/" + getKey()
				+ (command != null ? " " + command : "") + "\"");
		source.addMessage(new ChatMessage(getDefaultRejectMessage(command)));
	}

	/**
	 * The default error message given for an invalid command
	 *
	 * @param command The remainder of the command after the key
	 * @return The error message for this command
	 */
	public String getDefaultRejectMessage(String command) {
		return "Invalid command: \"/" + getKey() + (command != null ? " " + command : "")
				+ "\". Input in form \"" + getDescription() + "\".";
	}
}
