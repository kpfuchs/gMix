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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.test;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HtmlParser;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.LocalClassLoader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;


public class Testfall {

    public Testfall() {

    }

    public static void main(String[] args) {
        // byte[] a = {1, -69};
        // int i = Util.unsignedShortToInt(a);
        // System.out.println(i);
        // InetSocketAddress add = new InetSocketAddress(i);
        // System.out.println(add.getPort());
        // byte[] message =
        // {0,0,0,-45,72,84,84,80,47,49,46,49,32,51,48,52,32,78,111,116,32,77,111,100,105,102,105,101,100,13,10,68,97,116,101,58,32,84,117,101,44,32,48,57,32,74,117,108,32,50,48,49,51,32,50,51,58,53,50,58,50,57,32,71,77,84,13,10,83,101,114,118,101,114,58,32,65,112,97,99,104,101,47,50,46,50,46,50,50,32,40,68,101,98,105,97,110,41,13,10,67,111,110,110,101,99,116,105,111,110,58,32,75,101,101,112,45,65,108,105,118,101,13,10,75,101,101,112,45,65,108,105,118,101,58,32,116,105,109,101,111,117,116,61,53,44,32,109,97,120,61,49,48,48,13,10,69,84,97,103,58,32,34,52,99,99,54,50,57,54,45,52,98,45,52,100,56,50,52,57,57,98,52,54,48,48,48,34,13,10,86,97,114,121,58,32,65,99,99,101,112,116,45,69,110,99,111,100,105,110,103,13,10,13};
        // System.out.println(Integer.parseInt("d0a",16));
        // loadClass();
        String motherUrl = "http://www.informatik.uni-hamburg.de/svs/team/fuchs.php";

        URI startUrl = null;
        try {
            startUrl = new URI(motherUrl);

        } catch (URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        String testString = "<img style=\"margin-right:12px;\" src=\"../fuchs/fuchs.jpg\"><img alt=\"Logo des Fachbereichs Informatik\" src=\"/icons/fbi_neu/fbilogo_grau64x64.gif\" height=\"64\" width=\"64\" /><script src=\"/SVS/lib/prototype.js\" type=\"text/javascript\"></script><link rel=\"stylesheet\" href=\"/SVS/includes/svs.css\" type=\"text/css\" media=\"all\" />";
        List<String> ergebnis = HtmlParser.getAllRessourcesHtml(motherUrl,testString);
        for (String e : ergebnis) {
           System.out.println(e);
//            try {
//                URI bla = new URI(e);
//                System.out.println("------------------------");
//                System.out.println(e);
//                System.out.println(bla.normalize());
//                System.out.println(bla.relativize(startUrl));
//                System.out.println(startUrl.relativize(bla));
//                System.out.println(startUrl.resolve(bla).getPath());
//            } catch (URISyntaxException e1) {
//                // TODO Auto-generated catch block
//                e1.printStackTrace();
//            }
           
        }

    }

    public static void loadClass() {

        try {
            Testwrite xxx = LocalClassLoader.newInstance("test", "Testwrite.java");
            xxx.write();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void start() {

        byte[] buffer = { 5, 1, 0 };
        System.out.println(buffer[0]);
        buffer = changeBuffer(buffer);
        System.out.println(buffer[0]);
    }

    public static byte[] changeBuffer(byte[] buffer) {
        System.out.println(buffer[0]);
        buffer = Arrays.copyOfRange(buffer, 1, 2);

        System.out.println(buffer[0]);
        return buffer;

    }
}