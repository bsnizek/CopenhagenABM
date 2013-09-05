/*
©Copyright 2012 Bernhard Snizek
This file is part of CopenhagenABM.

CopenhagenABM is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CopenhagenABM is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
*/

package copenhagenabm.environment.contexts;

import copenhagenabm.environment.Zone;
import copenhagenabm.main.GlobalVars;
import repast.simphony.context.DefaultContext;

public class ZoneContext extends DefaultContext<Zone>{
	
	public ZoneContext() {
		super(GlobalVars.CONTEXT_NAMES.ZONE_CONTEXT);
	}
	
}
