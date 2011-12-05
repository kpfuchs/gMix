/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package internalInformationPort;


import java.io.IOException;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * Logs and/or displays messages depending on the message's <code>Level</code>, 
 * this logger's <code>DISPLAY_LEVEL</code> and  this logger's 
 * <code>LOG_LEVEL</code> (which are defined in the property file).
 * <p>
 * For a detailed explanation of the different levels see 
 * <code>http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/Level.html
 * </code>.
 * <p
 * Log files are stored in the folder "logfiles".
 * 
 * @author Karl-Peter Fuchs
 */
final class LogFileGenerator {
	
	/** 
	 * <code>Logger</code> object used to log messages.
	 */
	private static Logger logger;
	
	/** 
	 * Logging level that can be used to control logging output.
	 */
	private static Level logLevel;
	
	/** 
	 * Displaying level that can be used to control displaying output.
	 */
	private static int displayLevel;
	
	/* 
	 * Reads values from property file, gets "Logger", sets log level and 
	 * initializes log file.
	 */
	static {
		
		readValuesFromPorpertyFile();
		logger = Logger.getLogger(LogFileGenerator.class.getName());
		logger.setLevel(logLevel);
		initializeLogFile(new LocalFormatter());
		
	}
	
	
	/**
	 * Empty constructor. Never used since all methods are static.
	 */
	private LogFileGenerator() {
		
		
	}
	
	
	/** 
	 * Returns the <code>Logger</code> used by this class.
	 * 
	 * @return <code>Logger</code> used by this class.
	 */
	protected static Logger getLogger() {
		
		return logger;
		
	}

	
	/**
	 * Logs/displays the bypassed message if the bypassed <code>Level</code> is 
	 * higher than this logger's level(s) (<code>displayLevel</code> and 
	 * <code>logLevel</code>).
	 * 
	 * @param level		<code>Level</code> of the bypassed message.
	 * @param message	The message to be logged/displayed.
	 */
	protected static void log(Level level, String message) {
		
		logger.log(level, message);
		
	}

	
	/** 
	 * Generates a log file using the bypassed <code>Formatter</code>.
	 * <p>
	 * Generates a filename using the actual date and time and writes 
	 * information about the mix (this <code>LogFileGenerator</code> belongs 
	 * to) into the corresponding file.
	 * 
	 * @param formatter	<code>Formatter</code> for log file entries.
	 */
	private static void initializeLogFile(Formatter formatter) {

		FileHandler fileHandler = null;
		
		// generate FileHandler
		try {
			
			// generate file name
			String filename = "logfiles/logfile-";
			Calendar calendar = Calendar.getInstance();
			filename += calendar.get(Calendar.HOUR_OF_DAY);
			filename += calendar.get(Calendar.MINUTE);
			filename += "-";
			filename += calendar.get(Calendar.MONTH);
			filename += calendar.get(Calendar.DAY_OF_MONTH);
			filename += calendar.get(Calendar.YEAR);
			filename += ".txt";
			
			fileHandler = new FileHandler(filename);
			fileHandler.setFormatter(formatter);
			
		} catch (SecurityException e) {
			
			e.printStackTrace();
			System.exit(1);
			
		} catch (IOException e) {

			e.printStackTrace();
			System.exit(1);
			
		} 
		
		logger.addHandler(fileHandler);
		
		// save properties to log file
		String config = Settings.getProperties().toString();
		config = config.replace('{', '\t');
		config = config.replace(',', '\n');
		config = config.replace(' ', '\t');
		config = config.replace('}', ' ');
		logger.config("Settings: \n" +config);
		
	}


	/** 
	 * Loads <code>LOG_LEVEL</code> and <code>DISPLAY_LEVEL</code> from 
	 * property file.
	 * 
	 * @see #logLevel
	 * @see #displayLevel
	 */
	private static void readValuesFromPorpertyFile() {
		
		String logLevelAsString = Settings.getProperty("LOG_LEVEL");
		
		if (logLevelAsString.compareTo("OFF") == 0) {
			
			logLevel = Level.OFF;
			
		} else if (logLevelAsString.compareTo("SEVERE") == 0) {
			
			logLevel = Level.SEVERE;
			
		} else if (logLevelAsString.compareTo("WARNING") == 0) {
			
			logLevel = Level.WARNING;
			
		} else if (logLevelAsString.compareTo("INFO") == 0) {
			
			logLevel = Level.INFO;
			
		} else if (logLevelAsString.compareTo("CONFIG") == 0) {
			
			logLevel = Level.CONFIG;
			
		} else if (logLevelAsString.compareTo("FINE") == 0) {
			
			logLevel = Level.FINE;
			
		} else if (logLevelAsString.compareTo("FINER") == 0) {
			
			logLevel = Level.FINER;
			
		} else if (logLevelAsString.compareTo("FINEST") == 0) {
			
			logLevel = Level.FINEST;
			
		} else if (logLevelAsString.compareTo("SEVERE") == 0) {
			
			logLevel = Level.SEVERE;
			
		} else {
			
			logLevel = Level.ALL;
			
		}

		String displayLevelAsString = Settings.getProperty("DISPLAY_LEVEL");
		
		if (displayLevelAsString.compareTo("INFO") == 0) {
			
			displayLevel = Level.INFO.intValue();
			
		} else if (displayLevelAsString.compareTo("CONFIG") == 0) {
			
			displayLevel = Level.CONFIG.intValue();
			
		} else if (displayLevelAsString.compareTo("FINE") == 0) {
			
			displayLevel = Level.FINE.intValue();
			
		} else if (displayLevelAsString.compareTo("FINER") == 0) {
			
			displayLevel = Level.FINER.intValue();
			
		} else if (displayLevelAsString.compareTo("FINEST") == 0) {
			
			displayLevel = Level.FINEST.intValue();
			
		} else if (displayLevelAsString.compareTo("SEVERE") == 0) {
			
			displayLevel = Level.SEVERE.intValue();
			
		} else {
			
			displayLevel = Level.ALL.intValue();
			
		}
		
	}

	
	/** 
	 * <code>Formatter</code> used to format log file entries.
	 * 
	 * @author Karl-Peter Fuchs
	 */
	private static class LocalFormatter extends Formatter {

		
		/** 
		 * Formats the bypassed <code>LogRecord</code> to the following form:
		 * <code>DATE TIME OBJECT_NAME LOG_LEVEL: LOG_MESSAGE</code>. Displays 
		 * messages on standard output according to the <code>displayLevel
		 * </code> set.
		 * 
		 * @param	logRecord	The log record to be formatted.
		 * 
		 * @return				The formatted log record.
		 * 
		 * @see LogFileGenerator#displayLevel
		 */
		@Override
		public String format(LogRecord logRecord) {
			
			StringBuffer stringBuffer = new StringBuffer(1024);
			stringBuffer.append(new java.util.Date());
			stringBuffer.append(':');
			stringBuffer.append(' ');
			stringBuffer.append(logRecord.getLevel());
			stringBuffer.append(':');
			stringBuffer.append('\t');
			stringBuffer.append(formatMessage(logRecord));
			stringBuffer.append('\n');
			
			String result = stringBuffer.toString();			
			
			if (displayLevel == Level.ALL.intValue()) { 
				// display any log
				
				if (	logRecord.getLevel() != Level.SEVERE 
						&& 
						logRecord.getLevel() != Level.WARNING
						&&
						logRecord.getLevel() != Level.INFO
						) {	// Note: SEVERE, WARNING and INFO are always 
							// displayed (by Logger, automatically). This 
							// if-clause prevents double output.
					
					System.out.println(formatMessage(logRecord));
					
				}
				
			} else if (displayLevel == Level.FINEST.intValue()) {
				
				if (	logRecord.getLevel() != Level.SEVERE 
						&& 
						logRecord.getLevel() != Level.WARNING
						&&
						logRecord.getLevel() != Level.INFO
						) {	// Note: SEVERE, WARNING and INFO are always 
							// displayed (by Logger, automatically). This 
							// if-clause prevents double output.
					
					System.out.println(formatMessage(logRecord));
					
				}
				
			} else if (displayLevel == Level.FINER.intValue()) {
				
				if (	logRecord.getLevel() == Level.INFO
						||
						logRecord.getLevel() == Level.CONFIG
						|| 
						logRecord.getLevel() == Level.FINE
						|| 
						logRecord.getLevel() == Level.FINER
						) {
					
					System.out.println(formatMessage(logRecord));
					
				}
				
			} else if (displayLevel == Level.FINE.intValue()) {
				
				if (	logRecord.getLevel() == Level.INFO
						||
						logRecord.getLevel() == Level.CONFIG
						|| 
						logRecord.getLevel() == Level.FINE
						) {
					
					System.out.println(formatMessage(logRecord));
					
				}
				
			} else if (displayLevel == Level.CONFIG.intValue()) {
				
				if (	logRecord.getLevel() == Level.INFO
						||
						logRecord.getLevel() == Level.CONFIG
						) {
					
					System.out.println(formatMessage(logRecord));
					
				}
				
			} else { // level "INFO"
				
				// SEVERE, WARNING and INFO are always 
				// displayed automatically (by Logger).
				
			}
			
		return result;
			
		}
		
	}
	
}
