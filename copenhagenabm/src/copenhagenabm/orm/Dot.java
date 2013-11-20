package copenhagenabm.orm;

import javax.persistence.Entity; 
import javax.persistence.Table; 
import javax.persistence.Column;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.agent.IAgent;

@Entity
@Table(name="dot")
public class Dot {
	
	private com.vividsolutions.jts.geom.GeometryFactory fact = new com.vividsolutions.jts.geom.GeometryFactory();
	
	private Point geom;
	private int tick;
	private int agentID;
	private int id;
	private String roadID;
	
	// the ID of the route the agent tried to model within the calibration model
	private int routeid;
	


	public Dot(int tick, IAgent agent, Coordinate coordinate) {
		this.tick = tick;
		this.agentID = agent.getID();
		this.setGeom(fact.createPoint(coordinate));
		this.routeid = agent.getMatchedGPSRoute().getOBJECTID();
		try {
			this.roadID = agent.getCurrentRoad().getIdentifier();
		} catch (NoIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Column(name="geom")
	@Type(type = "org.hibernatespatial.postgis.PGGeometryUserType")
	public Point getGeom() {
		return geom;
	}
	
	public void setGeom(Point geometry) {
		this.geom = geometry;
		this.geom.setSRID(4326);
	}
	

	@Column(name="roadid")
	public String getRoadID() {
		return roadID;
	}

	public void setRoadID(String roadID) {
		this.roadID = roadID;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name="agentid")
	public int getAgentID() {
		return agentID;
	}

	@Column(name="id")
	@Id
	public int getId() {
		return id;
	}

	public void setAgentID(int id) {
		this.agentID = id;
	}

	@Column(name="tick")
	public int getTick() {
		return tick;
	}

	public void setTick(int tick) {
		this.tick = tick;
	}
	
	@Column(name="routeid")
	public int getRouteid() {
		return routeid;
	}

	public void setRouteid(int routeid) {
		this.routeid = routeid;
	}
	
}