package copenhagenabm.agent;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.environment.FixedGeography;

public class Decision implements FixedGeography {

	private int agentID;
	private Coordinate coords;
	private String roadID;

	public Decision(int id, Coordinate position, String identifier) {
		this.setAgentID(id);
		this.coords=position;
		this.setRoadID(identifier);
	}

	public Coordinate getCoords() {
		
		return coords;
	}

	public void setCoords(Coordinate c) {
		coords = c;
		
	}

	public int getAgentID() {
		return agentID;
	}

	public void setAgentID(int agentID) {
		this.agentID = agentID;
	}

	public String getRoadID() {
		return roadID;
	}

	public void setRoadID(String roadID) {
		this.roadID = roadID;
	}

}