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
package staticContent.framework.observer;


// must be implemented by "Subjects" (cf. Observer Pattern), i.e. instances 
// that are supposed to notify other instances ("Observers") about changes of
// of the subject
// the types of changes to notify about must be specified in a interface of 
// type <T extends LocalEventListener>
public interface ObservationSubject<T extends LocalEventListener> {

	public void registerEventListener(T eventListener);
	public boolean unregisterEventListener(T eventListener);
	
}
