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
package evaluation.simulator.gui.service;

import evaluation.simulator.gui.customElements.ProgressWorker;

public class ProgressRegistry {
	private static ProgressRegistry instance = null;
	
	private ProgressWorker progressWorker;
	
	private ProgressRegistry() {}
	
	public static ProgressRegistry getInstance() {
        if (instance == null) {
            instance = new ProgressRegistry();
        }
        return instance;
    }

	public ProgressWorker getProgressWorker() {
		// TODO Auto-generated method stub
		return progressWorker;
	}
	
	public ProgressWorker setProgressWorker( ProgressWorker progress ) {
		return progressWorker = progress;
	}
}
