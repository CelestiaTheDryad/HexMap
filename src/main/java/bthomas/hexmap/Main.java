package bthomas.hexmap;

import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.Server;

/***
 * Main class for HexMap, handles Logger creation and command line processing
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public class Main {

	//Error codes
	public static final int GENERAL_ERROR = -1, LOGGER_INIT_ERROR = -2;

	//for handshaking between clients and server
	public static final String version = "HEXMAP 0.4-DEV";

	private boolean isServer = false;
	public static HexmapLogger logger;

	public Main(String[] args) {
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
			logger.log(HexmapLogger.SEVERE, HexmapLogger.getStackTraceString(e));
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
			logger = new HexmapLogger("HexmapClientLog", true);
		}
	}

	public static void main(String[] args) {
		//set exceptions from all threads to end up in our nice logger
		Thread.setDefaultUncaughtExceptionHandler((t, e) ->
			Main.logger.log(HexmapLogger.SEVERE, "Uncaught exception in: " + t.getName() + System.lineSeparator()
					+ HexmapLogger.getStackTraceString(e))
		);

		//run the program
		Main main = new Main(args);
		main.run();
	}
}
