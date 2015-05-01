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

import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class represents an integer simprop
 * 
 * @author alex
 * 
 */
public class IntProp extends SimProp {

	private static Logger logger = Logger.getLogger(IntProp.class);

	private int maxValue;
	private int minValue;
	private int value;

	private boolean auto;
	private boolean enableAuto;
	private boolean unlimited;
	private boolean enableUnlimited;

	private List<Observer> Observers = new LinkedList<Observer>();

	private String guiElement;

	private int stepSize;

	/**
	 * @return status of the auto checkbox
	 */
	public boolean getAuto() {
		return this.auto;
	}

	/**
	 * @param auto
	 *            status of the auto checkbox
	 */
	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	/**
	 * @return visibility of the auto checkbox
	 */
	public boolean getEnableAuto() {
		return this.enableAuto;
	}

	/**
	 * @param auto
	 *            visibility of the auto checkbox
	 */
	public void setEnableAuto(boolean auto) {
		this.enableAuto = auto;
	}

	/**
	 * @return status of the unlimited checkbox
	 */
	public boolean getUnlimited() {
		return this.unlimited;
	}

	/**
	 * @param unlimited
	 *            status of the unlimited checkbox
	 */
	public void setUnlimited(boolean unlimited) {
		this.unlimited = unlimited;
	}

	/**
	 * @return visibility of the unlimited checkbox
	 */
	public boolean getEnableUnlimited() {
		return this.enableUnlimited;
	}

	/**
	 * @param unlimited
	 *            visibility of the unlimited checkbox
	 */
	public void setEnableUnlimited(boolean unlimited) {
		this.enableUnlimited = unlimited;
	}

	/**
	 * @return maximum value
	 */
	public int getMaxValue() {
		return this.maxValue;
	}

	/**
	 * @return minimum value
	 */
	public int getMinValue() {
		return this.minValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see evaluation.simulator.annotations.property.SimProp#getValue()
	 */
	@Override
	public Object getValue() {
		return this.value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see evaluation.simulator.annotations.property.SimProp#getValueType()
	 */
	@Override
	public Class<?> getValueType() {
		return Integer.class;
	}

	/**
	 * @param maxValue
	 *            maximum value
	 */
	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @param minValue
	 *            minimum value
	 */
	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * evaluation.simulator.annotations.property.SimProp#setValue(java.lang.
	 * Object)
	 */
	@Override
	public void setValue(Object o) {

		int tmp = (int) (o);
		if ((tmp <= this.getMaxValue()) && (tmp >= this.getMinValue())) {
			this.value = tmp;
			this.changed();
			return;
		}

		logger.log(Level.ERROR, "For " + super.getPropertyID() + " Value not in rage! " + tmp + "(int) is not in ("
				+ this.getMinValue() + ", " + this.getMaxValue() + ")");
		JOptionPane.showMessageDialog(null, "The value for " + super.getName() + " is not in range.", "Boundary error",
				JOptionPane.ERROR_MESSAGE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see evaluation.simulator.annotations.property.SimProp#toString()
	 */
	@Override
	public String toString() {
		return super.getName() + "" + this.value;
	}

	/**
	 * @return step size for the spinner element (gui)
	 */
	public int getStepSize() {
		return stepSize;
	}

	/**
	 * @param stepSize
	 *            step size for the spinner element (gui)
	 */
	public void setStepSize(int stepSize) {
		this.stepSize = stepSize;
	}

	/**
	 * @return gui element identifying string ("spinner" or "slider")
	 */
	public String getGuiElement() {
		return guiElement;
	}

	/**
	 * @param guiElement
	 *            gui element identifying string ("spinner" or "slider")
	 */
	public void setGuiElement(String guiElement) {
		this.guiElement = guiElement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * evaluation.simulator.annotations.property.SimProp#register(java.util.
	 * Observer)
	 */
	@Override
	public void register(Observer observer) {
		// System.err.println("REGISTER INTEGER OBSERVER");
		Observers.add(observer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * evaluation.simulator.annotations.property.SimProp#unregister(java.util
	 * .Observer)
	 */
	@Override
	public void unregister(Observer observer) {
		// System.err.println("UNREGISTER INTEGER OBSERVER");
		Observers.remove(observer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see evaluation.simulator.annotations.property.SimProp#changed()
	 */
	@Override
	public void changed() {
		// System.err.println("CHANGED");
		for (Observer observer : Observers) {
			observer.update((Observable) this, (Object) this.enabled);
		}
	}

}
