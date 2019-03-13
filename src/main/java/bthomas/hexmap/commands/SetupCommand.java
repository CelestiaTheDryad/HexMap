package bthomas.hexmap.commands;

import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This command is used to change the size of the Hexmap
 *
 * @author Brenda Thomas
 * @since 2019-03-11
 */
public class SetupCommand extends HexCommand {
	private Pattern pattern = Pattern.compile("^([1-9][0-9]*) ([1-9][0-9]*)$");

	@Override
	public String getKey() {
		return "setup";
	}

	@Override
	public boolean applyFromClient(Server server, ConnectionHandler client, String command) {
		//OP command
		return false;
	}

	@Override
	public boolean applyFromServer(Server server, String command) {
		Matcher match = pattern.matcher(command);
		if(match.matches()) {
			int x = Integer.parseInt(match.group(1));
			int y = Integer.parseInt(match.group(2));
			server.setSize(x, y);
			System.out.println("Server: Size set to " + x + ", " + y + ".");
			return true;
		}
		else {
			System.out.println("Bad setup command.");
			return false;
		}
	}
}
