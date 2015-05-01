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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.encdns.LibSodiumWrapper;

/**
 * This tool generates a keypair for use with EncDNS. The keys will be written
 * to two files. The path and prefix for the name can be specified. The
 * public key will be stored in <prefix>.pk, the secret key in <prefix>.sk.
 * 
 * @author Jens Lindemann
 */
public class EncDNSKeyGenerator {
    private static Options _opt;
    private LibSodiumWrapper _libSodium;
    
    /**
     * Constructor for EncDNSKeyGenerator
     * 
     * @param path path for saving key file
     * @param prefix filename prefix
     */
    public EncDNSKeyGenerator(String path, String prefix) {
        _libSodium = new LibSodiumWrapper();
        
        // Generate keys
        byte[] pk = new byte[_libSodium.PKBYTES];
        byte[] sk = new byte[_libSodium.SKBYTES];
        _libSodium.jni_generateKeys(pk, sk);
        
        // Construct filenames
        String pkFilename = path + prefix + ".pk";
        String skFilename = path + prefix + ".sk";
        
        // save to files
        System.out.println("saving plublic key to " +pkFilename); 
        writeByteArrayToFile(pk, pkFilename);
        System.out.println("saving secret key to " +pkFilename); 
        writeByteArrayToFile(sk, skFilename);
    }
    
    /**
     * Writes a byte[] to a file.
     * @param array byte[] to write
     * @param filename file to write to
     */
    private void writeByteArrayToFile(byte[] array, String filename) {
        try {
           DataOutputStream os = new DataOutputStream(new FileOutputStream(filename));
           os.write(array);
           os.close();
        } catch(FileNotFoundException e) {
            System.err.println("File could not be created: " + filename);
        } catch(IOException e) {
            System.err.println("Error when writing to file " + filename);
        }
    }
    
     /**
     * Main method.
     * @param args the command line arguments (see -h for documentation)
     */
    public static void main(String[] args) {
        try {
            _opt = new Options();
            _opt.addOption("h", "help", false, "prints this message");
            _opt.addOption("p", "path", false, "path for saving key files (default: ./lib/)");
            _opt.addOption("n", "name", false, "filename prefix (default: encdns)");
            _opt.addOption("s", "libsodiumwrap", true, "path to libsodiumWrap library (default: ./lib/");
            
            Options helpOpts = new Options();
            helpOpts.addOption("h", "help", false, "prints this message");
            
            CommandLineParser parser = new BasicParser();
            
            // We need to check for the help parameter first, as parsing all
            // simultaneously will throw an exception if a required option
            // is missing.
            CommandLine cmd = parser.parse(helpOpts, args, true);
            if (cmd.hasOption("help")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("EncDNSKeyGenerator", _opt);
                return;
            }
            
            // Now we can parse all options
            cmd = parser.parse(_opt, args);

            // get parameter values passed by CLI
            String libStr = cmd.getOptionValue("libsodiumwrap");
            String pathStr = cmd.getOptionValue("path");
            String nameStr = cmd.getOptionValue("name");
            
            // set standard values if corresponding CLI parameters are not set
            if(libStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                    libStr = System.getProperty("user.dir") + "\\lib\\libsodiumWrap.dll";
                } else { // assume a Unix-like OS
                    libStr = System.getProperty("user.dir") + "/lib/libsodiumWrap.so";
                }
            }
            if(nameStr == null) nameStr = "encdns";
            
            if(pathStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                    pathStr = ".\\lib\\";
                } else { // assume a Unix-like OS
                    pathStr = "./lib/";
                }
            }

            // load the required C wrapper library
            System.load(libStr);
            
            new EncDNSKeyGenerator(pathStr, nameStr);
        } catch (MissingOptionException e) {
            // print an error and the help if a required option is missing
             System.out.println("Missing required option: " + e.getMessage());
             HelpFormatter hf = new HelpFormatter();
             hf.printHelp("EncDNSClientProxy", _opt);
        } catch (ParseException e) {
            System.err.println(e);
        }
    }
}