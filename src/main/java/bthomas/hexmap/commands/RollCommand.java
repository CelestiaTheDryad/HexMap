package bthomas.hexmap.commands;

import bthomas.hexmap.net.ChatMessage;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RollCommand extends HexCommand {
	private Pattern roll = Pattern.compile("^([1-9][0-9]*)d([1-9][0-9]*)$");

	@Override
	public boolean applyFromClient(Server server, ConnectionHandler client, String command) {
		Matcher match = roll.matcher(command);
		if(match.matches()) {
			//by properties of the regex, these are guaranteed to be positive integers
			//TODO: handle input of too-large numbers
			int numDice = Integer.parseInt(match.group(1));
			int diceSize = Integer.parseInt(match.group(2));
			int total = 0;
			Random rand = server.getRandom();
			StringBuilder retString = new StringBuilder();
			retString.append("" + numDice + "d" + diceSize + ": ");
			for(int i = 0; i < numDice; i++) {
				int r = rand.nextInt(diceSize) + 1;
				total += r;
				retString.append(r + " ");
				//add a plus if there's another die to roll
				if(i < numDice - 1) {
					retString.append("+ ");
				}
			}
			retString.append("= " + total);

			server.sendAll(new ChatMessage(client.username + ": " + retString.toString()));

			return true;
		}
		else {
			client.addMessage(new ChatMessage("Invalid roll: " + command + ". Input in form \"roll 3d6\"."));
			return false;
		}
	}

	@Override
	public boolean applyFromServer(Server server, String command) {
		//servers cannot roll dice
		return false;
	}

	@Override
	public String getKey() {
		return "roll";
	}
}
