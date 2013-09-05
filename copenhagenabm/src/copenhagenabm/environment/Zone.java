package copenhagenabm.environment;

/*
 * Zone.java is part of copenhagenABM
 * 
 * (c) Manuel Claeys Bouuaert & Bernhard Snizek  
 * 
 * 
 * 
 */

import java.util.ArrayList;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.environment.FixedGeography;

import repastcity3.environment.Building;
import repastcity3.exceptions.NoIdentifierException;

public class Zone implements FixedGeography 

{

	/**
	 * A unique identifier for buildings, usually set from the 
	 * 'identifier' column in a shapefile
	 */
	private String identifier;
	
	private ArrayList<Building> buildings = new ArrayList<Building>();
	
	/*
	 * A person is a person living in an EntryPoint,
	 */
	private ArrayList<Person> persons = new ArrayList<Person>();
	
	
	/**
	 * The centroid coordinates of the Zone. This is also stored by the 
	 * projection that contains this Building but it is
	 * useful to have it here too. As they will never change 
	 * (buildings don't move) we don't need to worry about keeping
	 * them in sync with the projection.
	 */
	private Coordinate coords;

	private String ID;

	public Zone() {

	}

//	@Override
	public Coordinate getCoords() {
		return this.coords;
	}

//	@Override
	public void setCoords(Coordinate c) {
		this.coords = c;

	}

	public String getIdentifier() throws NoIdentifierException {
		if (this.identifier == null) {
			throw new NoIdentifierException("This zone has no identifier. This can happen "
					+ "when roads are not initialised correctly (e.g. there is no attribute "
					+ "called 'identifier' present in the shapefile used to create this Road)");
		} else {
			return identifier;
		}
	}

	public void setIdentifier(String id) {
		this.identifier = id;
	}


	@Override
	public String toString() {
		return "zone: " + this.identifier;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Zone))
			return false;
		Zone b = (Zone) obj;
		return this.identifier.equals(b.identifier);
	}

	/**
	 * Return this buildings unique id number.
	 */
	@Override
	public int hashCode() {
		return this.identifier.hashCode();
	}

	/*
	 * Adds a building to the zone and creates people slots at the same time
	 */

	public void addBuilding(Building building) {
		// add this building to the zone
				this.buildings.add(building);
				// create persons in buildings and add them to the zone
				
				for (int i=0; i<building.getPersons(); i++) {
					Person p = new Person(building, this);
					this.persons.add(p);
				}
		
	}

	/*
	 * Returns a random person within this zone
	 */
	public Person getRandomPerson() throws Exception {
		int nPersons = persons.size();
		if (nPersons==0) {
			throw new Exception("Zone " + this.getIdentifier() + " has no persons.");
		}
		return persons.get(new Random().nextInt(nPersons));
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}
	
}
