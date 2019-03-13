package bthomas.hexmap;

import bthomas.hexmap.Logging.HexmapLogger;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.Server;

/***
 * Main class for HexMap, handles Logger creation and command line processing
 */
public class Main {

	//Error codes
	public static final int GENRAL_ERROR = -1, LOGGER_INIT_ERROR = -2, GUI_FAILURE = -3;

	//for handshaking between clients and server
	public static final String version = "HEXMAP 0.4";

	private boolean isServer = false;
	public static HexmapLogger logger;

	public Main(String[] args) {
		//TODO:
		//make gui to select server/client (ALA starmade)?
		//this might would require a custom GUI for the server
		//by doing it this way, we force the server to be opened in some type of terminal to allow for commands and output
		processCommandLineArguments(args);
		setupLogger();
	}

	public void run() {
		try {
			if(isServer) {
				Server server = new Server();
			}
			else {
				Client client = new Client();
			}
		}
		catch (Exception e) {
			logger.log(HexmapLogger.SEVERE, e.toString());
		}
		finally {
			logger.close();
		}
	}

	private void processCommandLineArguments(String[] args) {
		isServer = args.length > 0 && args[0].equals("server");
	}

	private void setupLogger() {
		if(isServer) {
			logger = new HexmapLogger("HexmapServerLog", true);
		}
		else {
			logger = new HexmapLogger("HexmapClientLog", false);
		}
	}

	public static void main(String[] args) {
		Main main = new Main(args);
		main.run();
	}
}
