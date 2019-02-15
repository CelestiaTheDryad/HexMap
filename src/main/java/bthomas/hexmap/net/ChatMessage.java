package bthomas.hexmap.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;

import javax.swing.*;

public class ChatMessage extends HexMessage {
	private String text;
	private int UUID;

	public ChatMessage(String text, int UUID) {
		this.text = text;
		this.UUID = UUID;
	}

	@Override
	public void ApplyToClient(Client client) {
		SwingUtilities.invokeLater(() -> client.chatAppend(text));
	}

	@Override
	public void ApplyToServer(Server server, ConnectionHandler source) {
		server.sendAll(this);
	}
}
