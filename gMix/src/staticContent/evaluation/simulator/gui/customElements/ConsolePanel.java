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
package staticContent.evaluation.simulator.gui.customElements;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import staticContent.evaluation.simulator.gui.console.TextAreaConsoleAppender;

/**
 * {@link JPanel} containing the console
 * 
 * @author nachkonvention
 * 
 */
@SuppressWarnings("serial")
public class ConsolePanel extends JPanel {

	private static ConsolePanel instance = null;

	/**
	 * Singleton
	 * 
	 * @return instance of {@link ConsolePanel}
	 */
	public static ConsolePanel getInstance() {
		if (instance == null) {
			instance = new ConsolePanel();
		}
		return instance;
	}

	private final JTextPane textArea;
	private final JScrollPane scroll;

	private ConsolePanel() {

		this.textArea = new JTextPane();
		this.scroll = new JScrollPane(this.textArea);

		this.setLayout(new BorderLayout());
		this.textArea.setEditable(false);

		this.add(this.scroll, BorderLayout.CENTER);
		this.textArea.setVisible(true);

		this.scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				TextAreaConsoleAppender textAreaConsoleAppender = new TextAreaConsoleAppender();
				textAreaConsoleAppender.setTextArea(ConsolePanel.getInstance().textArea);
			}
		});
	}

}
