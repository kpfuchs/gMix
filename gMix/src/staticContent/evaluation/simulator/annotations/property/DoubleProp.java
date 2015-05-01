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
 * This class represents double simprops
 * 
 * @author alex
 * 
 */
public class DoubleProp extends SimProp {

	private static Logger logger = Logger.getLogger(DoubleProp.class);

	private double maxValue;
	private double minValue;
	private double value;

	private boolean auto;
	private boolean enableAuto;
	private boolean unlimited;
	private boolean enableUnlimited;

	private String guiElement;

	private double stepSize;

	private List<Observer> Observers = new LinkedList<Observer>();

	/**
	 * @return boolean that indicates if auto checkbox is checked or not
	 */
	public boolean getAuto() {
		return this.auto;
	}

	/**
	 * @param auto
	 *            true if auto checkbox should be enabled otherwise false
	 */
	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	/**
	 * @return true if auto is allowed otherwise false (then checkbox is
	 *         invisible)
	 */
	public boolean getEnableAuto() {
		return this.enableAuto;
	}

	/**
	 * @param auto
	 *            true if auto is allowed otherwise false (then checkbox is
	 *            invisible)
	 */
	public void setEnableAuto(boolean auto) {
		this.enableAuto = auto;
	}

	/**
	 * @return boolean that indicates if unlimited checkbox is checked or not
	 */
	public boolean getUnlimited() {
		return this.unlimited;
	}

	/**
	 * @param unlimited
	 *            true if unlimited checkbox should be enabled otherwise false
	 */
	public void setUnlimited(boolean unlimited) {
		this.unlimited = unlimited;
	}

	/**
	 * @return true if unlimited is allowed otherwise false (then checkbox is
	 *         invisible)
	 */
	public boolean getEnableUnlimited() {
		return this.enableUnlimited;
	}

	/**
	 * @param unlimited
	 *            true if unlimited is allowed otherwise false (then checkbox is
	 *            invisible)
	 */
	public void setEnableUnlimited(boolean unlimited) {
		this.enableUnlimited = unlimited;
	}

	/**
	 * @return maximum value
	 */
	public double getMaxValue() {
		return this.maxValue;
	}

	/**
	 * @return minimum value
	 */
	public double getMinValue() {
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
		return Double.class;
	}

	/**
	 * @param maxValue
	 */
	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @param minValue
	 */
	public void setMinValue(double minValue) {
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
		double tmp;

		if (o instanceof Integer) {
			tmp = new Double((int) (o));
		} else if (o instanceof String) {
			tmp = Double.parseDouble((String) (o));
		} else {
			tmp = (double) (o);
		}

		if ((tmp <= this.getMaxValue()) && (tmp >= this.getMinValue())) {
			this.value = tmp;
			return;
		}

		logger.log(Level.ERROR, "For " + super.getPropertyID() + " Value not in range! " + tmp + "(double) is not in ("
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
	 * @return
	 */
	public double getStepSize() {
		return stepSize;
	}

	/**
	 * @param stepSize
	 */
	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

	/**
	 * @return gui element identifying string ("spinner" or "slider")
	 */
	public String getGuiElement() {
		return guiElement;
	}

	/**
	 * Possible values: "spinner" or "slider"
	 * 
	 * @param guiElement
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
		// System.err.println("REGISTER DOUBLE OBSERVER");
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
		// System.err.println("UNREGISTER DOUBLE OBSERVER");
		Observers.remove(observer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see evaluation.simulator.annotations.property.SimProp#changed()
	 */
	@Override
	public void changed() {
		// System.err.println("CHANGED DOUBLE");
		for (Observer observer : Observers) {
			observer.update((Observable) this, (Object) this.enabled);
		}
	}

}