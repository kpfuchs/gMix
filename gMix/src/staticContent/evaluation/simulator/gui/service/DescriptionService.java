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
package staticContent.evaluation.simulator.gui.service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Implemented as a singleton
 * 
 * @author alex
 *
 */
public class DescriptionService extends Observable {

	private static DescriptionService _instance = null;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link DescriptionService}
	 */
	public static DescriptionService getInstance() {
		if (_instance == null) {
			_instance = new DescriptionService();
		}
		return _instance;
	}

	private String _description;

	private final List<Observer> _observers;

	private DescriptionService() {
		super();
		this._observers = new LinkedList<Observer>();
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#addObserver(java.util.Observer)
	 */
	@Override
	public void addObserver(Observer o) {
		this._observers.add(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#notifyObservers()
	 */
	@Override
	public void notifyObservers() {
		Iterator<Observer> iter = this._observers.iterator();
		while (iter.hasNext()) {
			iter.next().update(this, this._description);
		}
	}

	/**
	 * @param descr
	 * 		the description
	 */
	public void setDescription(String descr) {
		this._description = descr;
		this.notifyObservers();
	}

}
