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
package staticContent.evaluation.simulator.annotations.property;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * This class represents an string simprop
 * 
 * @author alex
 *
 */
public class StringProp extends SimProp {

	String possibleValues;
	String value;
	boolean multiSelection;
	
	private List<Observer> Observers = new LinkedList<Observer>();
	
	/**
	 * @return
	 * 		a string with possible values (comma seperated)
	 */
	public String getPossibleValues() {
		return this.possibleValues;
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#getValue()
	 */
	@Override
	public Object getValue() {
		return this.value;
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#getValueType()
	 */
	@Override
	public Class<?> getValueType() {
		return String.class;
	}

	/**
	 * Sets the possible values
	 * 
	 * @param values
	 */
	public void setPossibleValues(String values) {
		this.possibleValues = values;
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object o) {
		this.value = (String) (o);
		changed();
	}
	
	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#toString()
	 */
	@Override
	public String toString() {
		return super.getName() + "" + this.value;
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#register(java.util.Observer)
	 */
	@Override
	public void register(Observer observer){
//		System.err.println("REGISTER STRING OBSERVER");
		Observers.add(observer);
	}
	
	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#unregister(java.util.Observer)
	 */
	@Override
	public void unregister(Observer observer){
//		System.err.println("UNREGISTER STRING OBSERVER");
		Observers.remove(observer);
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#changed()
	 */
	@Override
	public void changed() {
//		System.err.println("CHANGED STRING");
		for ( Observer observer : Observers ) {
			observer.update((Observable) this, (Object) this.enabled);
		} 
	}

	/**
	 * Enables or disables multiselection
	 * 
	 * @param multiSelection
	 */
	public void setMultiSelection(boolean multiSelection) {
		this.multiSelection = multiSelection;
		
	}
	
	/**
	 * @return
	 * 		true if multiselection is allowed, otherwise false
	 */
	public boolean getMultiSelection() {
		return multiSelection;
		
	}
}
