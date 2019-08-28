package bthomas.hexmap.commands;

import bthomas.hexmap.common.net.ChatMessage;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This command is used to emulate a dice roll (eg "3d6" means roll and sum 3 six-sided dice)
 *
 * @author Brenda Thomas
 * @since 2019-03-11
 */
public class RollCommand extends HexCommand {
	private static final Pattern pattern = Pattern.compile("\\A(?:private )?([1-9][0-9]*)d([1-9][0-9]*) ?([+-][1-9][0-9]*)?\\Z");
	private static final String permission = "hexmap.commands.roll";
	private static int maxRollSize = 10;
	private static int maxDieSize = 100;

	@Override
	public boolean applyFromClient(Server server, ConnectionHandler client, String command) {

		/* add after default permissions
		if(!client.hasPermission(permission)) {
			respondToNoPermission(client, command);
		}
		*/

		//this command requires extra parts
		if(command == null) {
			respondToNoMatch(client, command);
			return false;
		}

		Matcher match = pattern.matcher(command);
		if(match.matches()) {
			//by properties of the regex, these are guaranteed to be positive integers
			int numDice = Integer.parseInt(match.group(1));
			int diceSize = Integer.parseInt(match.group(2));

			//require positive dice sizes and amounts
			if(numDice < 1 || diceSize < 1) {
				client.addMessage(new ChatMessage("Invalid roll command: roll " + command + ". Input positive size and number of dice."));
				return false;
			}

			//limit max size of dice rolls to prevent chat overload
			if(numDice > maxRollSize || diceSize > maxDieSize) {
				client.addMessage(new ChatMessage("Invalid roll command: roll " + command + ". Please input reasonably sized rolls."));
				return false;
			}

			String offsetString = match.group(3);
			int offset = offsetString != null ? Integer.parseInt(offsetString) : 0;
			boolean priv = command.contains("private");

			if(priv) {
				client.addMessage(new ChatMessage("Server: you rolled privately " + getDiceRoll(numDice, diceSize, offset, server.getRandom())));
			}
			else {
				// "Server: Joe rolled 3d6: 1 + 4 + 2 = 7"
				server.sendAll(new ChatMessage("Server: " + client.username + " rolled " + getDiceRoll(numDice, diceSize, offset, server.getRandom())));
			}

			return true;
		}
		else {
			respondToNoMatch(client, command);
			return false;
		}
	}

	@Override
	public boolean applyFromServer(Server server, String command) {
		//servers cannot roll dice
		return false;
	}

	/**
	 * Generates a string representing the output of a dice roll
	 *
	 * @param numDice The number of dice to roll
	 * @param diceSize The number of sides on each die
	 * @param offset The constant factor to apply to the roll
	 * @param rand The random generator to use on the dice
	 * @return A formatted string representing the dice roll
	 */
	private String getDiceRoll(int numDice, int diceSize, int offset, Random rand) {
		int total = offset;
		StringBuilder retString = new StringBuilder();

		//build start eg "3d6+2: "
		retString.append(numDice).append("d").append(diceSize);
		if(offset > 0) {
			retString.append("+").append(offset);
		}
		else if(offset < 0) {
			retString.append("-").append(Math.abs(offset));
		}
		retString.append(": ");

		//build rolls
		for(int i = 0; i < numDice; i++) {
			int r = rand.nextInt(diceSize) + 1;
			total += r;
			retString.append(r).append(" ");
			//add a plus if there's another die to publicRoll
			if(i < numDice - 1) {
				retString.append("+ ");
			}
		}
		if(offset > 0) {
			retString.append(" + ").append(offset).append(" ");
		}
		else if(offset < 0) {
			retString.append(" - ").append(Math.abs(offset)).append(" ");
		}
		retString.append("= ").append(total);
		return retString.toString();
	}

	@Override
	public String getDescription() {
		return "/roll[ private] <int>d<int>[(+|-)<int>]";
	}

	@Override
	public String getName() {
		return "roll";
	}
}
