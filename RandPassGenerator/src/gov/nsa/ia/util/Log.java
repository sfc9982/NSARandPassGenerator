package gov.nsa.ia.util;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * An instance of the Log class provides logging function for random pass
 * generator. This class also implements a self-test method.
 *
 * @author sfc9982
 */
public class Log {

	/**
	 * This getLogger method returns a modified Logger object. Which was configured
	 * to output formatted debug information.
	 *
	 * @param className String class name for format
	 *
	 * @return logger ready to use
	 */
	public static Logger getLogger(String className) {
		Logger retLogger = Logger.getLogger(className);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {
			private static final String FORMAT = "[%1$s] [%2$tF %2$tT] %3$s %n";

			@Override
			public String format(LogRecord record) {
				return String.format(FORMAT, record.getLevel(), new Date(record.getMillis()), record.getMessage());
			}

		});
		retLogger.setUseParentHandlers(false);
		retLogger.addHandler(handler);

		return retLogger;
	}

	public static Logger getMute() {
		Logger retLogger = Logger.getLogger("Mute");
		retLogger.setLevel(Level.OFF);

		return retLogger;
	}

	/**
	 * This main method performs unit testing for the logging class, by calling its
	 * self-test method if no args are given, or by passing logging message each
	 * command-line argument if args are given.
	 *
	 * @param args list of strings
	 */
	public static void main(String[] args) {
		System.out.println("Testing Log:");
		Logger debugLog = Log.getLogger(Log.class.getName());
		debugLog.log(Level.INFO, "An INFO level log!");
	}

}
