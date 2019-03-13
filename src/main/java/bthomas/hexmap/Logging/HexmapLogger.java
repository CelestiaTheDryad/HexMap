package bthomas.hexmap.Logging;

import bthomas.hexmap.Main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Logger base for Hexmap
 * Sets up logging to output to both a file and STD out as applicable
 */
public class HexmapLogger {
	public static final String INFO = "[INFO]: ", DEBUG = "[DEBUG]: ", ERROR = "[ERROR]: ", SEVERE = "[SEVERE]: ";

	private PrintWriter outputFile;
	private boolean writeToSTD;

	/**
	 * Standard constructor, creates an output file with a given name,
	 * if the output file already exists, renames it to one marked old and
	 * creates a new one (deleting a preexisting old file if it exists)
	 *
	 * @param logName the name of the file to output to
	 * @param writeToSTD whether the logger should log to STD out as well
	 */
	public HexmapLogger(String logName, boolean writeToSTD) {
		this.writeToSTD = writeToSTD;
		File logfile = new File(logName + ".txt");

		//move and replace current file with old file if necessary
		if(logfile.exists()) {
			File oldLogFile = new File(logName + ".old.txt");
			try {
				Files.move(logfile.toPath(), oldLogFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				System.out.println(e.toString());
				System.exit(Main.LOGGER_INIT_ERROR);
			}
		}

		//create reader for current log file
		try {
			outputFile = new PrintWriter(new BufferedWriter(new FileWriter(logfile)));
		}
		catch (IOException e) {
			System.out.println(e.toString());
			System.exit(Main.LOGGER_INIT_ERROR);
		}
	}

	/**
	 * Logs a message with this logger
	 *
	 * @param level The info level to mark this message as
	 * @param message The message to log
	 */
	public void log(String level, String message) {
		outputFile.println(level + message);
		if(writeToSTD) {
			System.out.println(level + message);
		}
	}

	public void flush() {
		outputFile.flush();
	}

	public void close() {
		outputFile.close();
	}
}
