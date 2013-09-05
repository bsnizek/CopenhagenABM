package copenhagenabm.orm;

import javax.persistence.Entity; 
import javax.persistence.Table; 
import javax.persistence.Column;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

@Entity
@Table(name="dot")
public class Dot {
	
	private com.vividsolutions.jts.geom.GeometryFactory fact = new com.vividsolutions.jts.geom.GeometryFactory();
	
	private int tick;
	private int agentID;
	private int id;
	private String edgeID;
	

	@Column(name="edgeid")
	public String getEdgeID() {
		return edgeID;
	}

	public void setEdgeID(String edgeID) {
		this.edgeID = edgeID;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name="agentid")
	public int getAgentID() {
		return agentID;
	}


	private Point geom;
	
	public Dot(int tick, int agentID, Coordinate coordinate) {
		this.tick = tick;
		this.agentID = agentID;
		this.setGeom(fact.createPoint(coordinate));
	}

	@Column(name="id")
	@Id
	public int getId() {
		return id;
	}

	public void setAgentID(int id) {
		this.agentID = id;
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

	@Column(name="tick")
	public int getTick() {
		return tick;
	}


	public void setTick(int tick) {
		this.tick = tick;
	}
	
	
	
}