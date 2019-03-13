package bthomas.hexmap;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.Server;

/***
 * Main class for HexMap
 */
public class Main {

	//for handshaking between clients and server
	public static final String version = "HEXMAP 0.4";

	public static void main(String[] args) {
		//TODO:
		//make gui to select server/client (ALA starmade)?
		//this might would require a custom GUI for the server
		//by doing it this way, we force the server to be opened in some type of terminal to allow for commands and output
		if(args.length == 0) {
			Client.main(args);
		}
		else if(args[0].equals("server")) {
			Server.main(args);
		}
	}
}
