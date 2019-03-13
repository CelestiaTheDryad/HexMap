package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

/**
 * This message contains a chat message from a player
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class ChatMessage extends HexMessage {
	private String text;

	/**
	 * Standard constructor
	 *
	 * @param text The message as to be printed in the chat box
	 */
	public ChatMessage(String text) {
		this.text = text;
	}

	@Override
	public void ApplyToClient(Client client) {
		client.waitForGUI();
		SwingUtilities.invokeLater(() -> client.chatAppend(text));
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		this.text = source.username + ": " + this.text;
		server.sendAll(this);
	}
}
