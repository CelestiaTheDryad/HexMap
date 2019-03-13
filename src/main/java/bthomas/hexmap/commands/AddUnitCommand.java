package bthomas.hexmap.commands;

import bthomas.hexmap.common.Unit;
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
	private Pattern pattern = Pattern.compile("^([a-zA-Z0-9]+) (?:[1-9][0-9]* ){4}[1-9][0-9]*$");

	@Override
	public String getKey() {
		return "add";
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
			String[] parts = command.split(" ");
			int r = Integer.parseInt(parts[3]);
			int g = Integer.parseInt(parts[4]);
			int b = Integer.parseInt(parts[5]);

			//check colors ok
			if(r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
				System.out.println("Unit colors not in acceptable range (0-255).");
				return false;
			}

			//TODO: make sure unit is in bounds
			Unit newUnit = new Unit(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), new Color(r, g, b));
			server.addUnit(newUnit);
			server.sendAll(new NewUnitMessage(newUnit));
			System.out.println("Server: added unit: " + parts[0] + " at: " + parts[1] + ", " + parts[2] +
					" with color: " + r + " " + g + " " + b + ".");
			return true;
		}
		else {
			System.out.println("Bad add command.");
			return false;
		}
	}
}
