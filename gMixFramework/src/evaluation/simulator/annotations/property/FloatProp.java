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
package evaluation.simulator.annotations.property;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class represents a float simprop
 * 
 * @author alex
 * 
 */
public class FloatProp extends SimProp {
	private static Logger logger = Logger.getLogger(FloatProp.class);

	private float maxValue;
	private float minValue;
	private float value;

	private boolean auto;
	private boolean enableAuto;
	private boolean unlimited;
	private boolean enableUnlimited;

	private String guiElement;

	private float stepSize;

	private List<Observer> Observers = new LinkedList<Observer>();

	/**
	 * @return status of auto checkbox
	 */
	public boolean getAuto() {
		return this.auto;
	}

	/**
	 * @param auto
	 *            status of auto checkbox
	 */
	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	/**
	 * @return visibility of auto checkbox
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
	public float getMaxValue() {
		return this.maxValue;
	}

	/**
	 * @return maximum value
	 */
	public float getMinValue() {
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
		return Float.class;
	}

	/**
	 * @param maxValue
	 *            maximum value
	 */
	public void setMaxValue(float maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @param minValue
	 *            minimum value
	 */
	public void setMinValue(float minValue) {
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
		float tmp;

		if (o instanceof Integer) {
			tmp = new Float((int) (o));
		} else if (o instanceof String) {
			tmp = Float.parseFloat((String) (o));
		} else {
			tmp = (float) (o);
		}

		if ((tmp <= this.getMaxValue()) && (tmp >= this.getMinValue())) {
			this.value = tmp;
			return;
		}

		logger.log(Level.ERROR, "For " + super.getPropertyID() + " Value not in rage! " + tmp + "(float) is not in ("
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
	 * @return the stepsize for the spinner gui element
	 */
	public float getStepSize() {
		return stepSize;
	}

	/**
	 * @param stepSize
	 *            the stepsize for the spinner gui element
	 */
	public void setStepSize(float stepSize) {
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
		// System.err.println("REGISTER FLOAT OBSERVER");
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
		// System.err.println("UNREGISTER FLOAT OBSERVER");
		Observers.remove(observer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see evaluation.simulator.annotations.property.SimProp#changed()
	 */
	@Override
	public void changed() {
		// System.err.println("CHANGED FLOAT");
		for (Observer observer : Observers) {
			observer.update((Observable) this, (Object) this.enabled);
		}
	}

}
