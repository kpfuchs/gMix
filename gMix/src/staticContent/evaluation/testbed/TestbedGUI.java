/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2015  SVS
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

package staticContent.evaluation.testbed;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import staticContent.evaluation.testbed.core.ExperimentSeries;

public class TestbedGUI {

	static JFrame frame;
	static JLabel label = new JLabel(
			"Select the experiment script you want to load:");
	static JLabel label2 = new JLabel(" ");
	static JComboBox<String> comboBox = new JComboBox<String>();
	static JButton button = new JButton("Run");
	private static ArrayList<String> fileList;
	static String[] description = null;
	private static GridBagConstraints c;
	
	// Change, if package changes
	final static String pathToTestbedExperimentScripts = "../gMix/inputOutput/testbed/experimentDefinitions";
		

	private static void createAndShowGUI() {
		frame = new JFrame("gMix - Testbed");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Toolkit.getDefaultToolkit().createImage(
				"etc/img/icons/icon128.png"));

		getFileList();
		fillComboBox();

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				int selectedIndex = comboBox.getSelectedIndex();
				String[] path = { description[selectedIndex] };
				ExperimentSeries.main(path);
			}

		});
		
		setComponentsInWindow();

		// Display the window.
		frame.pack();
		frame.setVisible(true);
		frame.setSize(500, 200);
		frame.setLocationRelativeTo(null);
	}

	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private static List<String> getFileList() {
		fileList = new ArrayList<String>();
		File[] files = new File(
				pathToTestbedExperimentScripts)
				.listFiles();

		for (File file : files) {
			if (file.isFile() && !file.getName().contains("topology")) {
				fileList.add(file.getName());
			}
		}
		return fileList;
	}

	private static void fillComboBox() {
		description = new String[fileList.size()];
		fileList.toArray(description);

		for (int i = 0; i < description.length; i++) {
			comboBox.addItem(description[i]);
		}
	}

	private static void setComponentsInWindow() {
		frame.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		frame.add(label, c);

		c.gridx = 0;
		c.gridy = 1;
		frame.add(comboBox, c);
		
		c.gridx = 0;
		c.gridy = 2;
		frame.add(label2, c);

		c.gridx = 0;
		c.gridy = 3;
		frame.add(button, c);
	}

}

