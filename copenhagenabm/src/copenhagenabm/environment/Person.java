package copenhagenabm.environment;

import repastcity3.environment.Building;

public class Person {
	
	private Building building;

	public Person(Building building2, Zone zone2) {
		
		this.building = building2;
	}
	
	public Building getBuilding() {
		return this.building;
	}

	

}
