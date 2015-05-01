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

package staticContent.framework.aboutBoxGui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class AboutBoxGUI {

	static JFrame frame;
	static JLabel label = new JLabel(
			"<html>gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/ <br/> Copyright (C) 2015  SVS <br/><br/> This program comes with ABSOLUTELY NO WARRANTY. <br/>This is free software, and you are welcome to redistribute it under certain conditions. </html>");
	static JLabel label2 = new JLabel(" ");
	static JButton button = new JButton("OK");
	private static GridBagConstraints c;

	private static void createAndShowGUI() {
		frame = new JFrame("About gMix");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Toolkit.getDefaultToolkit().createImage(
				"etc/img/icons/icon128.png"));

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}

		});

		setComponentsInWindow();

		// Display the window.
		frame.pack();
		frame.setVisible(true);
		frame.setSize(600, 200);
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

	private static void setComponentsInWindow() {
		frame.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		frame.add(label, c);

		c.gridx = 0;
		c.gridy = 1;
		frame.add(label2, c);

		c.gridx = 0;
		c.gridy = 2;
		frame.add(button, c);
	}

}
