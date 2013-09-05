package copenhagenabm.tests;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.environment.FixedGeography;
import copenhagenabm.main.ContextManager;

public class PPoint implements FixedGeography {

	private Coordinate coord;
	
	public Coordinate getCoords() {
		// TODO Auto-generated method stub
		return coord;
	}

	public void setCoords(Coordinate c) {
		coord =c;
	}
	
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	private String identifier;
	
	public Geometry getGeometry() {
		return DistanceTest.pointProjection.getGeometry(this);
	}

}
