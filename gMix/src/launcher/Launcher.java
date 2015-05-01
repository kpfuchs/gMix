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
package launcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import staticContent.evaluation.localTest.LocalTestLoadGen;
import staticContent.evaluation.simulator.SimulatorGUI;
import staticContent.evaluation.testbed.TestbedGUI;
import staticContent.evaluation.traceParser.ScenarioExtractorGUI;
import staticContent.framework.aboutBoxGui.AboutBoxGUI;
import staticContent.framework.launcher.GlobalLauncher;
import staticContent.framework.portableDist.RunBuildPortableDist;

public class Launcher extends JFrame {

	private static final long serialVersionUID = -445572519649506779L;

	private JLabel label;
	private JButton buttonSimulator;
	private JButton buttonTestbed;
	private JButton buttonGlobalLauncher;
	private JButton buttonLocalTest;
	private JButton buttonScenarioExtractor;
	private JButton buttonPortableDist;
	private JButton buttonAboutGmix;

	private JLabel labelSimulator;
	private JLabel labelTestbed;

	private JLabel labelGlobalLauncher;

	private JLabel labelLocalTest;

	private JLabel labelScenarioExtractor;

	private JLabel labelPortableDistributionCreator;

	private GridBagConstraints c;

	private JLabel label2;

	public Launcher() {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Nimbus Look And Feel not found");
		}
		setTitle("gMix Framework Launcher");
		setSize(750, 360);
		setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().createImage(
				"etc/img/icons/icon128.png"));

		labelSimulator = new JLabel(
				"Configure and run simulated experiments (easy use, single machine)");
		labelTestbed = new JLabel(
				"Run distributed experiments (complex use, distributed over more machines)");
		labelGlobalLauncher = new JLabel("Run several demos");
		labelLocalTest = new JLabel("Test and debug plug-ins locally");
		labelScenarioExtractor = new JLabel("Generate specific trace files for evaluation");
		labelPortableDistributionCreator = new JLabel(
				"Build a portable distribution of the current framework state");
		label = new JLabel(
				"For more information see the gMix-Tutorial (Help/Welcome or Help/gMix Tutorial).");

		label2 = new JLabel(" ");

		buttonSimulator = new JButton("Simulator");
		buttonSimulator.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				SimulatorGUI.main(null);
			}
		});

		buttonTestbed = new JButton("Testbed");
		buttonTestbed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				TestbedGUI.main(null);
			}
		});

		buttonGlobalLauncher = new JButton("Global Launcher");
		buttonGlobalLauncher.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				GlobalLauncher.main(new String[0]);
			}
		});

		buttonLocalTest = new JButton("Local Test");
		buttonLocalTest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				LocalTestLoadGen.main(new String[0]);
			}
		});

		buttonScenarioExtractor = new JButton("Scenario Extractor");
		buttonScenarioExtractor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				ScenarioExtractorGUI.main(null);
			}
		});

		buttonPortableDist = new JButton("Portable Distribution Creator");
		buttonPortableDist.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				RunBuildPortableDist.main(null);
			}
		});
		
		buttonAboutGmix = new JButton("About gMix");
		buttonAboutGmix.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AboutBoxGUI.main(null);
			}
		});

		addContent();

		setVisible(true);
		this.setLocationRelativeTo(null);

	}

	private void addContent() {

		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);

		c.gridx = 0;
		c.gridy = 0;
		add(buttonSimulator, c);

		c.gridx = 1;
		c.gridy = 0;
		add(labelSimulator, c);

		c.gridx = 0;
		c.gridy = 1;
		add(buttonTestbed, c);

		c.gridx = 1;
		c.gridy = 1;
		add(labelTestbed, c);

		c.gridx = 0;
		c.gridy = 2;
		add(buttonGlobalLauncher, c);

		c.gridx = 1;
		c.gridy = 2;
		add(labelGlobalLauncher, c);

		c.gridx = 0;
		c.gridy = 3;
		add(buttonLocalTest, c);

		c.gridx = 1;
		c.gridy = 3;
		add(labelLocalTest, c);

		c.gridx = 0;
		c.gridy = 4;
		add(buttonScenarioExtractor, c);

		c.gridx = 1;
		c.gridy = 4;
		add(labelScenarioExtractor, c);

		c.gridx = 0;
		c.gridy = 5;
		add(buttonPortableDist, c);

		c.gridx = 1;
		c.gridy = 5;
		add(labelPortableDistributionCreator, c);

		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 2;
		add(label, c);
		
		
		c.gridx = 0;
		c.gridy = 7;
		c.gridwidth = 2;
		add(label2, c);
		
		
		c.gridx = 0;
		c.gridy = 8;
		c.gridwidth = 1;
		add(buttonAboutGmix, c);
		
	}

	public static void main(String[] args) {
		new Launcher();
	}

}
