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
package staticContent.framework.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Formatter;

import staticContent.framework.launcher.CommandLineParameters;
import staticContent.framework.launcher.ToolName;


public class ToolSelectorCommandLine {

	
	public ToolSelectorCommandLine(CommandLineParameters params) {
		if (params.gMixTool != null && params.gMixTool != ToolName.NOT_SET) // start desired tool directly
			ToolName.execute(params);
		else
			displayUserInputOptions(null);
		params.gMixTool = getUserInput();
		ToolName.execute(params);
	}
	
	
	private static ToolName getUserInput() {
		ToolName t = ToolName.NOT_SET;
		boolean isFirst = true;
		do {
			if (isFirst)
				isFirst = false;
			else
				displayUserInputOptions(null);
			String userInput = readString();
			if (userInput.equals("?") || userInput.equalsIgnoreCase("help"))
				displayUsage();
			else if (userInput.equalsIgnoreCase("q"))
				System.exit(0);
			t = ToolName.getToolByIdentifier(userInput);
		} while (t == ToolName.NOT_SET);
		return t;
	}
	
	
	private static void displayUserInputOptions(String errorMessage) {
		displayHeader();
		printlnMax80("note: ", "the \"normal\" way to start the gMix framework is via \"java [JavaOptions] -jar gMixFramework.jar [noGUI] [gMixTool] [cofigFile] [toolParameters]\"");
		printlnMax80("      ", "configuration is done via the config files in the \"inputOutput\" folder"); 
		printlnMax80("      ", "configuration via this command line tool is not supported"); 
		System.out.println();
		displayHorizontalBar();
		System.out.println(); 
		printlnMax80("type [?] for further details or choose one of the following options:");
		System.out.println();
		Formatter f = new Formatter();
		for (ToolName tool: ToolName.validValues())
			f.format("     %-4s %-15s  %s\n", "[" +tool.numericIdentifier +"]", tool.identifiers[0], tool.descriptionShort);
		f.format("\n     %-4s %-15s  %s", "[q]", "quit", "exits the program");
		String[] lines = f.toString().split("\n");
		for (String line:lines)
			printlnMax80(27, line);
		displayCopyright();
		System.out.println();
		if (errorMessage == null) {
			System.out.print("your choice: ");
		} else {
			System.out.println(errorMessage); 
			System.out.print("new choice: ");
		}
	}
	
	
	private static void displayHeader() {
		displayHorizontalBar();
		printlnMax80("gMix Framework - unstable pre alpha release v0.004 (" +Calendar.getInstance().get(Calendar.YEAR) +")");
		printlnMax80("https://svs.informatik.uni-hamburg.de/gmix/");
		//printlnMax80("This program comes with ABSOLUTELY NO WARRANTY. Type [w] for more info");
		displayHorizontalBar();
		System.out.println();
	}
	
	
	private static void displayCopyright() {
		System.out.println();
		displayHorizontalBar();
		printlnMax80("Copyright (C) " +Calendar.getInstance().get(Calendar.YEAR) +" SVS, https://svs.informatik.uni-hamburg.de/gmix/"); 
		printlnMax80("This program comes with ABSOLUTELY NO WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. You should have received a copy of the GNU General Public License along with this program. If not, see<http://www.gnu.org/licenses/>"); 
		displayHorizontalBar();
	}
	
	
	private static void displayHorizontalBar() {
		System.out.println("--------------------------------------------------------------------------------");
	}
	
	
	private static void displayUsage() {
		displayHeader(); 
		printlnMax80("Usage: ", "\"java [JavaOptions] -jar gMixFramework.jar [noGUI] [TOOL] [cofigFile] [toolParameters]\"");
		System.out.println(); 
		printlnMax80("starting with [noGUI] (e.g. \"java -jar gMixFramework.jar -noGUI -TOOL=mix\") willstart the framework in command line mode");
		System.out.println(); 
		displayHorizontalBar();
		System.out.println(); 
		printlnMax80("available [gMixTool]s:");
		System.out.println(); 
		Formatter f = new Formatter();
		for (ToolName tool: ToolName.validValues())
			f.format("     %-4s %-15s  %5s\n", "[" +tool.numericIdentifier +"]", tool.identifiers[0], tool.descriptionShort);
		String[] lines = f.toString().split("\n");
		for (String line:lines)
			printlnMax80(27, line);
		System.out.println();
		displayHorizontalBar();
		System.out.println(); 
		printlnMax80("possible [toolParameters]:");
		printlnMax80("  ", "-CONFIGFILE=filename.txt -- will start the desired tool with the configuration file provided (files are located in ./inputOutput/anonNode/). Example: \"java -jar gMixFramework.jar -noGUI -TOOL=mix -CONFIGFILE=filename.txt\""); 
		printlnMax80("  ", "-OVERWRITE='[KEY_1=VALUE_1],[KEY_2=VALUE_2],...[KEY_n=VALUE_n]' -- will overwrite all keys ([KEY]) in the current configuration with the values ([VALUE]) provided. Example: \"java -jar gMixFramework.jar -noGUI -TOOL=mix -OVERWRITE='INFO_SERVICE_ADDRESS=10.1.1.11,INFOSERVICE_PORT=22002'\"");
		System.out.println(); 
		printlnMax80("suggested [-JavaOptions] include:");
		printlnMax80("  ", "-ea                     to enable assertions for debugging"); 
		printlnMax80("  ", "-Xms<size> -Xmx<size>   to associate more ram (Java heap size). example: \"-Xms1024m -Xmx1024m\" to associate 1 gigabyte."); 
		System.out.println();
		printlnMax80("if you want to run a class not specified in [-gMixTool]s use: \"java -cp gMixFramework.jar packagaName.ClassName\""); 
		//printlnMax80("\npress RETURN to continue"); 
		displayHorizontalBar();
		waitForReturn();
	}
	
	
	private static void printlnMax80(String toPrint) {
		printlnMax80(null, toPrint);
	}
		
		
	private static void printlnMax80(String indent, String toPrint) {
		String indentFurther = (indent == null) ? null : getEmptyString(indent.length());
		if (indent != null && indentFurther != null && indent.length() != indentFurther.length())
			throw new RuntimeException("check params");
		int maxLen = (indent == null) ? 80 : 80 - indent.length();
		if (toPrint.length() < maxLen) {
			if (indent != null)
				System.out.println(indent + toPrint);
			else
				System.out.println(toPrint);
		} else {
			String[] lines = split(toPrint, maxLen);
			if (indent != null)
				System.out.println(indent + lines[0]);
			else
				System.out.println(lines[0]);
			for (int i=1; i<lines.length; i++)
				if (indentFurther != null)
					System.out.println(indentFurther + lines[i]);
				else
					System.out.println(lines[i]);
		}
	}
	
	
	private static void printlnMax80(int indent, String toPrint) {
		if (toPrint.length() < 80) {
			System.out.println(toPrint);
		} else {
			String indentStr = getEmptyString(indent);
			int maxCharPerLine = 80 - indent;
			String firstLine = toPrint.substring(0, 80);
			System.out.println(firstLine);
			String remaining = toPrint.substring(80);
			String[] lines = split(remaining, maxCharPerLine);
			for (int i=0; i<lines.length; i++)
				System.out.println(indentStr + lines[i]);
		}
	}
	
	
	private static String getEmptyString(int len) {
		String res = "";
		for (int i=0; i<len; i++)
			res += " ";
		return res; 
	}
	
    
	private static String[] split(String src, int len) {
		String[] result = new String[(int)Math.ceil((double)src.length()/(double)len)];
		for (int i=0; i<result.length; i++)
			result[i] = src.substring(i*len, Math.min(src.length(), (i+1)*len));
		return result;
	}
	
	
	private static String readString() {
		try {
    		BufferedReader input =  new BufferedReader(new InputStreamReader(System.in));
    		return input.readLine();
    	} catch (IOException e) {
    		e.printStackTrace();
    		return readString();
    	}
    }
	
	
	private static void waitForReturn() {
		System.out.println("\npress [RETURN] to continue"); 
		readString();
	}
	
}
