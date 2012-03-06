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

package gui;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import framework.Implementation;
import framework.LocalClassLoader;


/**
 * 
 * @author Daniel Kaiser
 *
 */
public class mixGUI extends javax.swing.JFrame {
	
	public enum Component {
		ExternalInformationPort ("ExternalInformationPort"),
		InputOutputHandler ("InputOutputHandler"),
		KeyGenerator ("KeyGenerator"),
		MessageProcessor ("MessageProcessor"),
		NetworkClock ("NetworkClock"),
		OutputStrategy ("OutputStrategy"),
		UserDatabase ("UserDatabase");
		
		private final String name;
		
		Component(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public class Panel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1642260334818260371L;
		private JLabel chosenLabel;
		private JLabel descriptionLabel;
		private JButton chooseButton;
		private JButton configureButton;
		private JPanel upperPanel = new JPanel();
		private JPanel lowerPanel = new JPanel();

		Panel(final String name) {

			this.setPreferredSize(new java.awt.Dimension(500, 90));
			this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			{
				upperPanel.setPreferredSize(new java.awt.Dimension(450, 45));
				this.add(upperPanel, BorderLayout.NORTH);
				{
					descriptionLabel = new JLabel();
					upperPanel.add(descriptionLabel);
					descriptionLabel.setText("Choose " + name + ":");
				}
				{
					chooseButton = new JButton();
					upperPanel.add(chooseButton);
					chooseButton.setText("Locate Class");

					chooseButton.addActionListener(
							new ActionListener(){
								public void actionPerformed(ActionEvent e) {
									JFileChooser chooser = new JFileChooser();
									FileNameExtensionFilter filter = new FileNameExtensionFilter(
											"Java Source Code", "java");
									chooser.setFileFilter(filter);

									String packageName = new String( new char[] { (char) ( (byte) name.charAt(0) + 32 ) } );
									packageName += name.substring(1, name.length());

									chooser.setCurrentDirectory(new File("src/" + packageName));

									int returnVal = chooser.showOpenDialog(upperPanel);
									if(returnVal == JFileChooser.APPROVE_OPTION) {

										//TODO reinladen der Klasse in das Interface prüfen;
										//irgendwie muss die richtige Methode genutzt werden...

										String binaryName = chooser.getSelectedFile().getName();
										//binaryName = LocalClassLoader.normalizeBinaryClassName(binaryName);

										boolean success = true;

										/*try {
											Class<?> testClass = LocalClassLoader.loadClassFile(packageName + "." + binaryName);

											//TODO ist es möglich auf einen Klassentyp zu testen, ohne zu instanziieren?
											Object o = testClass.newInstance();

											if (!(o instanceof Implementation)) {
												success = false;
											} else {
												//TODO vereinfachen
												//switch auf eine enum... --> Component
												

											}



										} catch (Exception e1) {
											success = false;
											e1.printStackTrace();
										}
*/
										if (success) {

											chosenLabel.setText("Chosen implementation: " + binaryName);
											configureButton=new JButton();
											configureButton.setText("Configure");
											lowerPanel.add(configureButton);
											
										}
										//TODO else -- Fehlermeldung
									}
								}
							});

				}
			}
			{
				lowerPanel.setPreferredSize(new java.awt.Dimension(450, 45));
				this.add(lowerPanel, BorderLayout.SOUTH);

				{
					chosenLabel = new JLabel();
					lowerPanel.add(chosenLabel);
					chosenLabel.setText("No " + name + " chosen");

				}
			}

			setDefaultCloseOperation(EXIT_ON_CLOSE);
		}

		//TODO instantiate from enumeration-type
		public Panel(Component component) {
			// TODO Auto-generated constructor stub
		}
	}

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel("com.apple.laf.AquaLookAndFeel");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 1330463905820511574L;
	private Panel[] panels = new Panel[7];
	private JPanel mainPanel = new JPanel();
	private JPanel buttonPanel = new JPanel();
	private JButton OpenButton;
	private JButton ValidateButton;
	private JButton CloseButton;

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mixGUI inst = new mixGUI();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}

	public mixGUI() {
		super();
		initGUI();
	}

	private void initGUI() {
		try {
			{
				getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

				OpenButton = new JButton();
				buttonPanel.add(OpenButton);
				OpenButton.setText("Open Configuration");
				OpenButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						System.exit(0);
					}
				});

				ValidateButton = new JButton();
				buttonPanel.add(ValidateButton);
				ValidateButton.setText("Validate Configuration");
				ValidateButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						System.exit(0);
					}
				});

				ValidateButton.setEnabled(false);

				CloseButton = new JButton();
				buttonPanel.add(CloseButton);
				CloseButton.setText("Save Configuration");
				CloseButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						System.exit(0);
					}
				});

				CloseButton.setEnabled(false);
			}
			{
				getContentPane().add(mainPanel, BorderLayout.NORTH);
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
				mainPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

				{
					String[] s = { "ExternalInformationPort", "InputOutputHandler", "KeyGenerator", "MessageProcessor",
							"NetworkClock", "OutputStrategy", "UserDatabase" };
					
					//TODO iterate over the enum...

					for (int i=0; i < 7; i++) {
						panels[i] = new Panel(s[i]);
						mainPanel.add(panels[i]);
					}
				}

			}

			this.setSize(520, 700);

		} catch (Exception e) {
			e.printStackTrace();
		}	
	}	
}
