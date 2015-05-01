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
 * This class represents boolean simprops
 * 
 * @author alex
 *
 */
public class BoolProp extends SimProp {

	private boolean value;
	
	private List<Observer> Observers = new LinkedList<Observer>();

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
		return Boolean.class;
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object o) {
		this.value = (boolean) (o);
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
//		System.err.println("REGISTER BOOL OBSERVER");
		Observers.add(observer);
	}
	
	@Override
	public void unregister(Observer observer){
//		System.err.println("UNREGISTER BOOL OBSERVER");
		Observers.remove(observer);
	}

	/* (non-Javadoc)
	 * @see evaluation.simulator.annotations.property.SimProp#changed()
	 */
	@Override
	public void changed() {
//		System.err.println("CHANGED BOOL");
		for ( Observer observer : Observers ) {
			observer.update((Observable) this, (Object) this.enabled);
		} 
	}

}
