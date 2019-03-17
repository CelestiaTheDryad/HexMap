package bthomas.hexmap.commands;

import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.common.Unit;
import bthomas.hexmap.net.ChatMessage;
import bthomas.hexmap.net.NewUnitMessage;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This command adds a new unit to the Hexmap board
 *
 * @author Brenda Thomas
 * @since 2019-03-11
 */
public class AddUnitCommand extends HexCommand {

	// "alphanumeric-string number number number number number"
	private static final Pattern pattern = Pattern.compile("\\A([a-zA-Z0-9]+) (?:[1-9][0-9]* ){4}[1-9][0-9]*\\Z");
	private static final String permission = "hexmap.commands.addunit";

	@Override
	public String getKey() {
		return "add";
	}

	@Override
	public String getDescription() {
		return "/add <name> <x> <y> <r> <g> <b>";
	}

	@Override
	public boolean applyFromClient(Server server, ConnectionHandler client, String command) {
		if(!client.hasPermission(permission)) {
			respondToNoPermission(client, command);
			return false;
		}

		//this command requires extra info
		if(command == null) {
			respondToNoMatch(client, command);
			return false;
		}

		return generalApply(server, client, command);
	}

	@Override
	public boolean applyFromServer(Server server, String command) {
		//this command requires extra info
		if(command == null) {
			respondToNoMatch(server, command);
			return false;
		}

		return generalApply(server, null, command);
	}

	/**
	 * Generalized apply function that handles command from both clients and server
	 *
	 * @param server The server the command is being applied to
	 * @param client The client that sent the command or null if the server sent the command
	 * @param command The remainder of the command that was entered
	 * @return True if the command was applied successfully, false otherwise
	 */
	private boolean generalApply(Server server, ConnectionHandler client, String command) {
		Matcher match = pattern.matcher(command);
		if(match.matches()) {
			String[] parts = command.split(" ");

			//by properties of regex, all ParseInts are valid
			int r = Integer.parseInt(parts[3]);
			int g = Integer.parseInt(parts[4]);
			int b = Integer.parseInt(parts[5]);

			//check colors ok
			if(r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
				if(client == null) {
					Main.logger.log(HexmapLogger.INFO, "Cannot add unit, invalid color code.");
				}
				else {
					Main.logger.log(HexmapLogger.INFO, client.username + " attempted invalid add command: invalid color code.");
					client.addMessage(new ChatMessage("Cannot add unit, invalid color code."));
				}
				return false;
			}

			int x = Integer.parseInt(parts[1]);
			int y = Integer.parseInt(parts[2]);

			//check coordinates OK
			Dimension serverSize = server.getSize();
			if(x < 0 || x >= serverSize.width || y < 0 || y >= serverSize.height) {
				if(client == null) {
					Main.logger.log(HexmapLogger.INFO, "Cannot add unit, location not on board.");
				}
				else {
					Main.logger.log(HexmapLogger.INFO, client.username + " attempted invalid add command: location not on board.");
					client.addMessage(new ChatMessage("Cannot add unit, location not on board."));
				}
			}

			Unit newUnit = new Unit(parts[0], x, y, new Color(r, g, b));
			server.addUnit(newUnit);

			if(client == null) {
				Main.logger.log(HexmapLogger.INFO, "Server added unit: " + parts[0] + " at: " + parts[1] + ", "
						+ parts[2] + " with color: " + r + " " + g + " " + b + ".");
			}
			else {
				Main.logger.log(HexmapLogger.INFO, client.username + " added unit: " + parts[0] + " at: " + parts[1] + ", "
						+ parts[2] + " with color: " + r + " " + g + " " + b + ".");
				client.addMessage(new ChatMessage("Unit added."));
			}

			server.sendAll(new NewUnitMessage(newUnit));
			return true;
		}
		else {
			if(client == null) {
				respondToNoMatch(server, command);
			}
			else {
				respondToNoMatch(client, command);
			}
			return false;
		}
	}
}
