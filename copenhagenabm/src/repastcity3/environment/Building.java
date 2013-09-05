/*�Copyright 2012 Nick Malleson
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
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.*/

package repastcity3.environment;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.agent.IAgent;
import copenhagenabm.environment.FixedGeography;
import copenhagenabm.main.ContextManager;

import repastcity3.exceptions.NoIdentifierException;

public class Building implements FixedGeography {

	/** A list of agents who live here */
	private List<IAgent> agents;

	/**
	 * A unique identifier for buildings, usually set from the 'identifier' column in a shapefile
	 */
	private String identifier;
	private Integer persons;
	private double kommunekod;
	private Integer OBJECTID;

	public Point getCentroid() {
		return ContextManager.buildingProjection.getGeometry(this).getCentroid();
	}
	
	/**
	 * The coordinates of the Building. This is also stored by the projection that contains this Building but it is
	 * useful to have it here too. As they will never change (buildings don't move) we don't need to worry about keeping
	 * them in sync with the projection.
	 */
	private Coordinate coords;

	public Building() {
		this.agents = new ArrayList<IAgent>();
	}

	// @Override
	public Coordinate getCoords() {
		return this.coords;
	}

//	@Override
	public void setCoords(Coordinate c) {
		this.coords = c;

	}

	public String getIdentifier() throws NoIdentifierException {
		if (this.identifier == null) {
			throw new NoIdentifierException("This building has no identifier. This can happen "
					+ "when roads are not initialised correctly (e.g. there is no attribute "
					+ "called 'identifier' present in the shapefile used to create this Road)");
		} else {
			return identifier;
		}
	}

	public void setIdentifier(String id) {
		this.identifier = id;
	}
	public void addAgent(IAgent a) {
		this.agents.add(a);
	}

	public List<IAgent> getAgents() {
		return this.agents;
	}

	@Override
	public String toString() {
		return "building: " + this.identifier;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Building))
			return false;
		Building b = (Building) obj;
		return this.identifier.equals(b.identifier);
	}

	/**
	 * Return this buildings unique id number.
	 */
	@Override
	public int hashCode() {
		return this.identifier.hashCode();
	}

	public double getKommunekod() {
		return kommunekod;
	}

	public void setKommunekod(double kommunekod) {
		this.kommunekod = kommunekod;
	}

	public Integer getOBJECTID() {
		return OBJECTID;
	}

	public void setOBJECTID(Integer oBJECTID) {
		OBJECTID = oBJECTID;
	}

	public Integer getPersons() {
		return persons;
	}

	public void setPersons(Integer persons) {
		this.persons = persons;
	}




}
