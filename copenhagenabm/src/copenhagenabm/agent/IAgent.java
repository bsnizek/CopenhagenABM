/*
©Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
*/

package copenhagenabm.agent;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.environment.Road;
import copenhagenabm.routes.MatchedGPSRoute;
import copenhagenabm.routes.Route;

/**
 * All agents must implement this interface so that it the simulation knows how
 * to step them.
 * 
 * @author Nick Malleson
 * 
 */
public interface IAgent {
	
	
	/**
	 * Returns the route the agent has walked on. 
	 * 
	 * @return
	 */
	public Route getRoute();
	
	/**
	 * Controls the agent. This method will be called by the scheduler once per
	 * iteration.
	 */
	 void step() ;

	/**
	 * Used by Agents as a means of stating whether or not they can be
	 * run in parallel (i.e. there is no inter-agent communication which will
	 * make parallelisation non-trivial). If all the agents in a simulation
	 * return true, and the computer running the simulation has
	 * more than one core, it is possible to step agents simultaneously.
	 * 
	 * @author Nick Malleson
	 * @authot Bernhard Snizek, bs@metascapes.org
	 */
	boolean isThreadable();
	
	/**
	 * (Optional). Add objects to the agents memory. Used to keep a record of all the
	 * buildings that they have passed.
	 * @param <T>
	 * @param objects The objects to add to the memory.
	 * @param clazz The type of object.
	 */
	<T> void addToMemory(List<T> objects, Class<T> clazz);
	
//	/**
//	 * (Optional). Get the transport options available to this agent. E.g.
//	 * an agent with a car who also could use public transport would return
//	 * <code>{"bus", "car"}</code>. If null then it is assumed that the agent
//	 * walks (the slowest of all transport methods). 
//	 */
//	List<String> getTransportAvailable();
	
	public Coordinate getPosition();
	
	public Coordinate getDestinationCoordinate();
	
	public Road getCurrentRoad();

	public boolean isAtDestination();
	
//	public boolean isTerminated();
	
//	public void setTerminated(boolean b);

	void writeHistory(int modelRun);
	
	public int getID(); // the unique ID

	public boolean isToBeKilled();

	public boolean isMoreThan50PercentOverGPSRouteDistance();

	public void setDidNotFindDestination(boolean b);

	public void snapAgentToRoad();

	void logBasics();
	
	public boolean isCalibrationAgent();
	
	public MatchedGPSRoute getMatchedGPSRoute();

	public void finishCalibrationRoute(boolean b);
}
