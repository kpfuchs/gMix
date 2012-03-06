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

package framework;

public interface ThreePhaseStart {

	// Pseudokonstruktor - wird aufgerufen sobald Klasse von ClassLoader instanziiert wurde
	public abstract void constructor();
	
	// referenzen auf andere Komponenten sind gesetzt; ggf. Daten an andere Komponenten schicken/
	// von anderen holen
	// bei "return" muss Komponente bereit sein ihren Betreib aufzunehmen
	public abstract void initialize();
	
	// bei Aufruf muss Komponente mit ihrer Nutzfunktion beginnen
	public abstract void begin();
}
