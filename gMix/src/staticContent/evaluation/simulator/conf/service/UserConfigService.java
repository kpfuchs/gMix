/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package staticContent.evaluation.simulator.conf.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class UserConfigService {

	private static Logger logger = Logger.getLogger(UserConfigService.class);

	/**
	 * Constants for file
	 */
	private final static String CONFIGFILENAME = "user.properties";
	private final static String CONFIGPATH = "inputOutput/simulator/config";
	private final static String CONFIGFILE = CONFIGPATH + "/" + CONFIGFILENAME;

	/**
	 * Names of Options & Constants for default Values
	 */

	// Console Frame
	
	private final static String BROWSER_PATH = "BROWSER_PATH";
	private final static String BROWSER_PATH_DEFAULT = "firefox-bin";
	
	private final static String CONSOLEFRAME_XPOS = "CONSOLEFRAME_XPOS";
	private final static Integer CONSOLEFRAME_XPOS_DEFAULT = 600;

	private final static String CONSOLEFRAME_YPOS = "CONSOLEFRAME_YPOS";
	private final static Integer CONSOLEFRAME_YPOS_DEFAULT = 100;

	private final static String CONSOLEFRAME_WIDTH = "CONSOLEFRAME_WIDTH";
	private final static Integer CONSOLEFRAME_WIDTH_DEFAULT = 700;

	private final static String CONSOLEFRAME_HEIGHT = "CONSOLEFRAME_HEIGHT";
	private final static Integer CONSOLEFRAME_HEIGHT_DEFAULT = 750;

	// Help Frame
	private final static String HELPFRAME_XPOS = "HELPFRAME_XPOS";
	private final static Integer HELPFRAME_XPOS_DEFAULT = 600;

	private final static String HELPFRAME_YPOS = "HELPFRAME_YPOS";
	private final static Integer HELPFRAME_YPOS_DEFAULT = 100;

	private final static String HELPFRAME_WIDTH = "HELPFRAME_WIDTH";
	private final static Integer HELPFRAME_WIDTH_DEFAULT = 700;

	private final static String HELPFRAME_HEIGHT = "HELPFRAME_HEIGHT";
	private final static Integer HELPFRAME_HEIGHT_DEFAULT = 750;

	// Configuration Frame
	private final static String CONFTOOLFRAME_XPOS = "CONFTOOLFRAME_XPOS";
	private final static Integer CONFTOOLFRAME_XPOS_DEFAULT = 100;

	private final static String CONFTOOLFRAME_YPOS = "CONFTOOLFRAME_YPOS";
	private final static Integer CONFTOOLFRAME_YPOS_DEFAULT = 100;

	private final static String CONFTOOLFRAME_WIDTH = "CONFTOOLFRAME_WIDTH";
	private final static Integer CONFTOOLFRAME_WIDTH_DEFAULT = 500;

	private final static String CONFTOOLFRAME_HEIGHT = "CONFTOOLFRAME_HEIGHT";
	private final static Integer CONFTOOLFRAME_HEIGHT_DEFAULT = 750;

	// Main Gui
	private final static String MAINGUI_XPOS = "MAINGUI_XPOS";
	private final static Integer MAINGUI_XPOS_DEFAULT = 0;

	private final static String MAINGUI_YPOS = "MAINGUI_YPOS";
	private final static Integer MAINGUI_YPOS_DEFAULT = 0;

	private final static String MAINGUI_WIDTH = "MAINGUI_WIDTH";
	private final static Integer MAINGUI_WIDTH_DEFAULT = 1024;

	private final static String MAINGUI_HEIGHT = "MAINGUI_HEIGHT";
	private final static Integer MAINGUI_HEIGHT_DEFAULT = 768;

	private final static String MAINGUI_HSPLIT_DEVIDER_LOCATION = "MAINGUI_HSPLIT_DEVIDER_LOCATION";
	private final static Integer MAINGUI_HSPLIT_DEVIDER_LOCATION_DEFAULT = 200;

	private final static String MAINGUI_CONSOLE_HEIGHT = "MAINGUI_CONSOLE_HEIGHT";
	private final static Integer MAINGUI_CONSOLE_HEIGHT_DEFAULT = 650;

	// GUI Service
	private final static String GUISERVICE_SEPERATE_CONF_TOOL = "GUISERVICE_SEPERATE_CONF_TOOL";
	private final static Boolean GUISERVICE_SEPERATE_CONF_TOOL_DEFAULT = false;

	private final static String GUISERVICE_SEPERATE_HELP_TOOL = "GUISERVICE_SEPERATE_HELP_TOOL";
	private final static Boolean GUISERVICE_SEPERATE_HELP_TOOL_DEFAULT = false;

	private final static String GUISERVICE_SEPERATE_CONSOLE = "GUISERVICE_SEPERATE_CONSOLE";
	private final static Boolean GUISERVICE_SEPERATE_CONSOLE_DEFAULT = false;

	private final static String GUISERVICE_TOGGLE_HOME_TAB = "GUISERVICE_TOGGLE_HOME_TAB";
	private final static Boolean GUISERVICE_TOGGLE_HOME_TAB_DEFAULT = false;

	/**
	 * Config
	 */
	private static Properties configuration = null;
	private static String OUTPUTFOLDERPATH = "./inputOutput/simulator/output";

	// this will be parsed always at program start!
	static {
		File f = new File (OUTPUTFOLDERPATH);
		if (!f.exists() && !f.mkdir()){
			throw new RuntimeException("Failed to create folder "+OUTPUTFOLDERPATH+"!. Please check user rights!");
		}
		new UserConfigService();
	}

	private UserConfigService() {
		configuration = new Properties();

		try {
			File folder = new File(UserConfigService.CONFIGPATH);
			File conf = new File(UserConfigService.CONFIGFILE);

			if (!folder.exists()) {
				folder.mkdir();
			}

			if (!conf.exists()) {
				conf.createNewFile();
			}

			FileOutputStream oFile = new FileOutputStream(conf, true);
			oFile.close();

			UserConfigService.configuration.load(new FileInputStream(UserConfigService.CONFIGFILE));
		} catch (IOException e) {
			logger.log(Level.FATAL, "Initializing User Properties failed! Reason:" + e.toString());
			// Without storable UserOptions the GUI should stop
			throw new RuntimeException("Initializing User Properties failed! Reason:" + e.toString());
		}
	}
	
	public static String getBRWOSER_PATH(){
		String result = getString(BROWSER_PATH);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + GUISERVICE_TOGGLE_HOME_TAB);
			return BROWSER_PATH_DEFAULT;
		}
	}

	public static Boolean getGUISERVICE_TOGGLE_HOME_TAB() {
		Boolean result = getBool(GUISERVICE_TOGGLE_HOME_TAB);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + GUISERVICE_TOGGLE_HOME_TAB);
			return GUISERVICE_TOGGLE_HOME_TAB_DEFAULT;
		}
	}

	public static void setGUISERVICE_TOGGLE_HOME_TAB(Boolean b) {
		setBool(GUISERVICE_TOGGLE_HOME_TAB, b);
	}

	public static Boolean getGUISERVICE_SEPERATE_CONSOLE() {
		Boolean result = getBool(GUISERVICE_SEPERATE_CONSOLE);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + GUISERVICE_SEPERATE_CONSOLE);
			return GUISERVICE_SEPERATE_CONSOLE_DEFAULT;
		}
	}

	public static void setGUISERVICE_SEPERATE_CONSOLE(Boolean b) {
		setBool(GUISERVICE_SEPERATE_CONSOLE, b);
	}

	public static Boolean getGUISERVICE_SEPERATE_HELP_TOOL() {
		Boolean result = getBool(GUISERVICE_SEPERATE_HELP_TOOL);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + GUISERVICE_SEPERATE_HELP_TOOL);
			return GUISERVICE_SEPERATE_HELP_TOOL_DEFAULT;
		}
	}

	public static void setGUISERVICE_SEPERATE_HELP_TOOL(Boolean b) {
		setBool(GUISERVICE_SEPERATE_HELP_TOOL, b);
	}

	public static Boolean getGUISERVICE_SEPERATE_CONF_TOOL() {
		Boolean result = getBool(GUISERVICE_SEPERATE_CONF_TOOL);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + GUISERVICE_SEPERATE_CONF_TOOL);
			return GUISERVICE_SEPERATE_CONF_TOOL_DEFAULT;
		}
	}

	public static void setGUISERVICE_SEPERATE_CONF_TOOL(Boolean b) {
		setBool(GUISERVICE_SEPERATE_CONF_TOOL, b);
	}

	public static Integer getMAINGUI_CONSOLE_HEIGHT() {
		Integer result = getInteger(MAINGUI_CONSOLE_HEIGHT);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + MAINGUI_CONSOLE_HEIGHT);
			return MAINGUI_CONSOLE_HEIGHT_DEFAULT;
		}
	}

	public static void setMAINGUI_CONSOLE_HEIGHT(Integer value) {
		setInteger(MAINGUI_CONSOLE_HEIGHT, value);
	}

	public static Integer getMAINGUI_HSPLIT_DEVIDER_LOCATION() {
		Integer result = getInteger(MAINGUI_HSPLIT_DEVIDER_LOCATION);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + MAINGUI_HSPLIT_DEVIDER_LOCATION);
			return MAINGUI_HSPLIT_DEVIDER_LOCATION_DEFAULT;
		}
	}

	public static void setMAINGUI_HSPLIT_DEVIDER_LOCATION(Integer value) {
		setInteger(MAINGUI_HSPLIT_DEVIDER_LOCATION, value);
	}

	public static Integer getMAINGUI_HEIGHT() {
		Integer result = getInteger(MAINGUI_HEIGHT);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + MAINGUI_HEIGHT);
			return MAINGUI_HEIGHT_DEFAULT;
		}
	}

	public static void setMAINGUI_HEIGHT(Integer value) {
		setInteger(MAINGUI_HEIGHT, value);
	}

	public static Integer getMAINGUI_WIDTH() {
		Integer result = getInteger(MAINGUI_WIDTH);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + MAINGUI_WIDTH);
			return MAINGUI_WIDTH_DEFAULT;
		}
	}

	public static void setMAINGUI_WIDTH(Integer value) {
		setInteger(MAINGUI_WIDTH, value);
	}

	public static Integer getMAINGUI_YPOS() {
		Integer result = getInteger(MAINGUI_YPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + MAINGUI_YPOS);
			return MAINGUI_YPOS_DEFAULT;
		}
	}

	public static void setMAINGUI_YPOS(Integer value) {
		setInteger(MAINGUI_YPOS, value);
	}

	public static Integer getMAINGUI_XPOS() {
		Integer result = getInteger(MAINGUI_XPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + MAINGUI_XPOS);
			return MAINGUI_XPOS_DEFAULT;
		}
	}

	public static void setMAINGUI_XPOS(Integer value) {
		setInteger(MAINGUI_XPOS, value);
	}

	public static Integer getCONFTOOLFRAME_HEIGHT() {
		Integer result = getInteger(CONFTOOLFRAME_HEIGHT);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONFTOOLFRAME_HEIGHT);
			return CONFTOOLFRAME_HEIGHT_DEFAULT;
		}
	}

	public static void setCONFTOOLFRAME_HEIGHT(Integer value) {
		setInteger(CONFTOOLFRAME_HEIGHT, value);
	}

	public static Integer getCONFTOOLFRAME_WIDTH() {
		Integer result = getInteger(CONFTOOLFRAME_WIDTH);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONFTOOLFRAME_WIDTH);
			return CONFTOOLFRAME_WIDTH_DEFAULT;
		}
	}

	public static void setCONFTOOLFRAME_WIDTH(Integer value) {
		setInteger(CONFTOOLFRAME_WIDTH, value);
	}

	public static Integer getCONFTOOLFRAME_YPOS() {
		Integer result = getInteger(CONFTOOLFRAME_YPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONFTOOLFRAME_YPOS);
			return CONFTOOLFRAME_YPOS_DEFAULT;
		}
	}

	public static void setCONFTOOLFRAME_YPOS(Integer value) {
		setInteger(CONFTOOLFRAME_YPOS, value);
	}

	public static Integer getCONFTOOLFRAME_XPOS() {
		Integer result = getInteger(CONFTOOLFRAME_XPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONFTOOLFRAME_XPOS);
			return CONFTOOLFRAME_XPOS_DEFAULT;
		}
	}

	public static void setCONFTOOLFRAME_XPOS(Integer value) {
		setInteger(CONFTOOLFRAME_XPOS, value);
	}

	public static Integer getHELPFRAME_HEIGHT() {
		Integer result = getInteger(HELPFRAME_HEIGHT);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + HELPFRAME_HEIGHT);
			return HELPFRAME_HEIGHT_DEFAULT;
		}
	}

	public static void setHELPFRAME_HEIGHT(Integer value) {
		setInteger(HELPFRAME_HEIGHT, value);
	}

	public static Integer getHELPFRAME_WIDTH() {
		Integer result = getInteger(HELPFRAME_WIDTH);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + HELPFRAME_WIDTH);
			return HELPFRAME_WIDTH_DEFAULT;
		}
	}

	public static void setHELPFRAME_WIDTH(Integer value) {
		setInteger(HELPFRAME_WIDTH, value);
	}

	public static Integer getHELPFRAME_YPOS() {
		Integer result = getInteger(HELPFRAME_YPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + HELPFRAME_YPOS);
			return HELPFRAME_YPOS_DEFAULT;
		}
	}

	public static void setHELPFRAME_YPOS(Integer value) {
		setInteger(HELPFRAME_YPOS, value);
	}

	public static Integer getHELPFRAME_XPOS() {
		Integer result = getInteger(HELPFRAME_XPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + HELPFRAME_XPOS);
			return HELPFRAME_XPOS_DEFAULT;
		}
	}

	public static void setHELPFRAME_XPOS(Integer value) {
		setInteger(HELPFRAME_XPOS, value);
	}

	public static Integer getCONSOLEFRAME_HEIGHT() {
		Integer result = getInteger(CONSOLEFRAME_HEIGHT);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONSOLEFRAME_HEIGHT);
			return CONSOLEFRAME_HEIGHT_DEFAULT;
		}
	}

	public static void setCONSOLEFRAME_HEIGHT(Integer value) {
		setInteger(CONSOLEFRAME_HEIGHT, value);
	}

	public static Integer getCONSOLEFRAME_WIDTH() {
		Integer result = getInteger(CONSOLEFRAME_WIDTH);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONSOLEFRAME_WIDTH);
			return CONSOLEFRAME_WIDTH_DEFAULT;
		}
	}

	public static void setCONSOLEFRAME_WIDTH(Integer value) {
		setInteger(CONSOLEFRAME_WIDTH, value);
	}

	public static Integer getCONSOLEFRAME_YPOS() {
		Integer result = getInteger(CONSOLEFRAME_YPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONSOLEFRAME_YPOS);
			return CONSOLEFRAME_YPOS_DEFAULT;
		}
	}

	public static void setCONSOLEFRAME_YPOS(Integer value) {
		setInteger(CONSOLEFRAME_YPOS, value);
	}

	public static Integer getCONSOLEFRAME_XPOS() {
		Integer result = getInteger(CONSOLEFRAME_XPOS);
		if (result != null) {
			return result;
		} else {
			logger.log(Level.DEBUG, "Using default value for Item " + CONSOLEFRAME_XPOS);
			return CONSOLEFRAME_XPOS_DEFAULT;
		}
	}

	public static void setCONSOLEFRAME_XPOS(Integer value) {
		setInteger(CONSOLEFRAME_XPOS, value);
	}

	/**
	 * private helper methods
	 */

	private static String getString(String item) {
		String readValue = configuration.getProperty(item);
		if (readValue == null) {
			logger.log(Level.DEBUG, "Item in property not found: " + item);
			return null;
		}
		if (readValue.equals("true") || readValue.equals("false")) {
			return String.valueOf(readValue);
		} else {
			logger.log(Level.DEBUG, "Item:  " + item + " could not be parsed to a boolean");
			return null;
		}
	}
	
	private static Boolean getBool(String item) {
		String readValue = configuration.getProperty(item);
		if (readValue == null) {
			logger.log(Level.DEBUG, "Item in property not found: " + item);
			return null;
		}
		if (readValue.equals("true") || readValue.equals("false")) {
			return Boolean.parseBoolean(readValue);
		} else {
			logger.log(Level.DEBUG, "Item:  " + item + " could not be parsed to a boolean");
			return null;
		}
	}

	private static Integer getInteger(String item) {
		Integer returnvalue = null;
		String readValue = configuration.getProperty(item);
		if (readValue == null) {
			logger.log(Level.DEBUG, "Item in property not found: " + item);
			return null;
		}
		try {
			returnvalue = Integer.parseInt(readValue);
		} catch (Exception e) {
			logger.log(Level.DEBUG, "Item:  " + item + " could not be parsed to an integer");
			return null;
		}
		return returnvalue;
	}

	private static void setBool(String key, boolean value) {
		try {
			UserConfigService.configuration.setProperty(key, new Boolean(value).toString());
			UserConfigService.configuration.store(new FileOutputStream(UserConfigService.CONFIGFILE), null);
		} catch (IOException e) {
			logger.log(Level.DEBUG, "Could not save item " + key + " to userConfig file!");
		}
	}

	private static void setInteger(String key, Integer value) {
		try {
			UserConfigService.configuration.setProperty(key, new Integer(value).toString());
			UserConfigService.configuration.store(new FileOutputStream(UserConfigService.CONFIGFILE), null);
		} catch (IOException e) {
			logger.log(Level.DEBUG, "Could not save item " + key + " to userConfig file!");
		}
	}

	/**
	 * Not needed right now..
	 */
	// private String getString(String item) {
	// String readValue = configuration.getProperty(item);
	// if (readValue == null) {
	// logger.log(Level.DEBUG, "Item in property not found: "+item);
	// return null;
	// }
	// return readValue;
	// }
}
