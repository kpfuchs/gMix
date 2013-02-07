/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
package evaluation.loadGenerator;

import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_Scheduled_ExitNodeRequestReceiver;
import evaluation.loadGenerator.asFastAsPossible.AFAP_Echo_ExitNodeRequestReceiver;
import evaluation.loadGenerator.asFastAsPossible.AFAP_LoadGenerator;
import evaluation.loadGenerator.dynamicSchedule.DynamicScheduleLoadGenerator;
import evaluation.loadGenerator.fixedSchedule.FixedScheduleLoadGenerator;
import framework.core.AnonNode;
import framework.core.config.Paths;
import framework.core.config.Settings;
import framework.core.launcher.CommandLineParameters;
import framework.core.launcher.GMixTool;
import framework.core.launcher.ToolName;


public class LoadGenerator extends GMixTool {

	public static enum InsertLevel {APPLICATION_LEVEL, MIX_PACKET_LEVEL};
	public static enum AL_Mode {AFAP, TRACE_FILE, CONSTANT_RATE, POISSON};
	public static enum MPL_Mode {AFAP, CONSTANT_RATE, POISSON};
	
	public InsertLevel INSERT_LEVEL;
	public AL_Mode AL_MODE;
	public MPL_Mode MPL_MODE;
	
	public CommandLineParameters commandLineParameters;
	public Settings settings;
	
	public boolean IS_LOCAL_TEST;
	public int SCHEDULE_AHEAD;
	
	
	public LoadGenerator(CommandLineParameters commandLineParameters) {
		System.out.println("\nLoadGenerator started");
		this.commandLineParameters = commandLineParameters;
		if (commandLineParameters.gMixTool == ToolName.LOCAL_TEST)
			this.IS_LOCAL_TEST = true;
		this.commandLineParameters.gMixTool = ToolName.LOAD_GENERATOR;
		this.settings = commandLineParameters.generateSettingsObject();
		setLoadGeneratorPlugins();
		loadParameters(settings);
		
		boolean sendAsFastAsPossible = (AL_MODE == AL_Mode.AFAP || MPL_MODE == MPL_Mode.AFAP);
		boolean useFixedSchedule = (!settings.getPropertyAsBoolean("GLOBAL_IS_DUPLEX") || !settings.getPropertyAsBoolean("AL-TRACE_FILE-USE_DYNAMIC_SCHEDULE"));
		
		if (sendAsFastAsPossible) // use performance-optimized writer for AFAP mode (InsterLevel is irrelevant for AFAP)
			AFAP_LoadGenerator.createInstance(this);
		else if (useFixedSchedule) 
			FixedScheduleLoadGenerator.createInstance(this);
		else 
			DynamicScheduleLoadGenerator.createInstance(this);
	}
	
	
	private void setLoadGeneratorPlugins() {
		settings.setProperty("LAYER_5_PLUG-IN_MIX", "loadGeneratorPlugIn_v0_001");
		settings.setProperty("LAYER_5_PLUG-IN_CLIENT", "loadGeneratorPlugIn_v0_001");
	}
	
	
	private void loadParameters(Settings settings) {
		if (settings.getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("APPLICATION_LEVEL"))
			this.INSERT_LEVEL = InsertLevel.APPLICATION_LEVEL;
		else if (settings.getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("MIX_PACKET_LEVEL"))
			this.INSERT_LEVEL = InsertLevel.MIX_PACKET_LEVEL;
		else
			System.err.println("could not read property \"GENERATE_LOAD_ON\" from " +Paths.LG_PROPERTY_FILE_PATH); 
		this.SCHEDULE_AHEAD = settings.getPropertyAsInt("SCHEDULE_AHEAD");
		if (INSERT_LEVEL == InsertLevel.APPLICATION_LEVEL) {
			if (settings.getProperty("AL-MODE").equalsIgnoreCase("AFAP"))
				this.AL_MODE = AL_Mode.AFAP;
			else if (settings.getProperty("AL-MODE").equalsIgnoreCase("TRACE_FILE"))
				this.AL_MODE = AL_Mode.TRACE_FILE;
			else if (settings.getProperty("AL-MODE").equalsIgnoreCase("CONSTANT_RATE"))
				this.AL_MODE = AL_Mode.CONSTANT_RATE;
			else if (settings.getProperty("AL-MODE").equalsIgnoreCase("POISSON"))
				this.AL_MODE = AL_Mode.POISSON;
			else
				System.err.println("could not read property \"AL-MODE\" from " +Paths.LG_PROPERTY_FILE_PATH); 	
		} else if (INSERT_LEVEL == InsertLevel.MIX_PACKET_LEVEL) {
			if (settings.getProperty("MPL-MODE").equalsIgnoreCase("AFAP"))
				this.MPL_MODE = MPL_Mode.AFAP;
			else if (settings.getProperty("MPL-MODE").equalsIgnoreCase("CONSTANT_RATE"))
				this.MPL_MODE = MPL_Mode.CONSTANT_RATE;
			else if (settings.getProperty("MPL-MODE").equalsIgnoreCase("POISSON"))
				this.MPL_MODE = MPL_Mode.POISSON;
			else
				System.err.println("could not read property \"MPL-MODE\" from " +Paths.LG_PROPERTY_FILE_PATH); 	
		}
	}

	
	public static ExitNodeRequestReceiver createExitNodeRequestReceiver(AnonNode anonNode) {
		Settings settings = anonNode.getSettings();
		settings.addProperties(Paths.LG_PROPERTY_FILE_PATH);
		
		boolean afapModeOn = 
				((settings.getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("APPLICATION_LEVEL") 
						&& settings.getProperty("AL-MODE").equalsIgnoreCase("AFAP")) 
				|| 
				(anonNode.getSettings().getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("MIX_PACKET_LEVEL")
						&& settings.getProperty("MPL-MODE").equalsIgnoreCase("AFAP"))
			);
		
		if (afapModeOn)
			return AFAP_Echo_ExitNodeRequestReceiver.createInstance(anonNode);
		else if (anonNode.getSettings().getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("APPLICATION_LEVEL"))
			return new ALRR_Scheduled_ExitNodeRequestReceiver(anonNode);
		else if (anonNode.getSettings().getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("MIX_PACKET_LEVEL"))
			throw new RuntimeException("no special purpose ExitNodeRequestReceiver for MIX_PACKET_LEVEL"); 
		else
			throw new RuntimeException("could not read property \"GENERATE_LOAD_ON\" from " +Paths.LG_PROPERTY_FILE_PATH); 
	}
	
	
	public static InsertLevel getInsertLevel(AnonNode anonNode) {
		Settings settings = anonNode.getSettings();
		settings.addProperties(Paths.LG_PROPERTY_FILE_PATH);
		
		if (settings.getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("APPLICATION_LEVEL")) {
			return InsertLevel.APPLICATION_LEVEL;
		} else if (settings.getProperty("GENERATE_LOAD_ON").equalsIgnoreCase("MIX_PACKET_LEVEL")) {
			return InsertLevel.MIX_PACKET_LEVEL;
		} else
			throw new RuntimeException("could not read property \"GENERATE_LOAD_ON\" from " +Paths.LG_PROPERTY_FILE_PATH); 
	}
	
	
	public final static boolean VALIDATE_IO = false; // TODO
	
	static {
		if (VALIDATE_IO)
			System.err.println(
					"WARNING: LoadGenerator.VALIDATE_IO is enabled;"
					+" this may severely decrease performance"
				); 
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		CommandLineParameters params = new CommandLineParameters(args);
		params.gMixTool = ToolName.LOAD_GENERATOR;
		new LoadGenerator(params);
	}
}
