package bthomas.hexmap.commands;

import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.sun.istack.internal.Nullable;

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
	 * This key needs to be unique for each command
	 *
	 * @return The key for this command
	 */
	//TODO: this is not secure, ideally would abstract that all subclasses have a final String key
	public abstract String getKey();

	/**
	 * Applies this command to a server, keeping track of who entered the command
	 *
	 * @param server The server to apply the command on
	 * @param client The client sending the command
	 * @param command The remainder of the command test, or null if there was no additional text
	 * @return True if the command could be successfully executed, false otherwise
	 */
	public abstract boolean applyFromClient(Server server, ConnectionHandler client, @Nullable String command);

	/**
	 * Applies this command to a server, knowing the command came from the server console
	 *
	 * @param server The server to apply the command on
	 * @param command The remainder of the command test, or null if there was no additional text
	 * @return True if the command could be successfully executed, false otherwise
	 */
	public abstract boolean applyFromServer(Server server, @Nullable String command);
}
