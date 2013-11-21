package copenhagenabm.orm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.agent.IAgent;

@Entity
@Table(name="dot")
public class Route {

	private LineString geom;
	
	private int id;
	
	private int agentID;
	private int matchedRouteID;

	@Column(name="geom")
	@Type(type = "org.hibernatespatial.postgis.PGGeometryUserType")
	public LineString getGeom() {
		return geom;
	}

	public void setGeom(LineString geom) {
		this.geom = geom;
		this.geom.setSRID(4326);
	}

	@Column(name="agentid")
	public int getAgentID() {
		return agentID;
	}

	public void setAgentID(int agentID) {
		this.agentID = agentID;
	}
	
	@Column(name="matchedrouteid")
	public int getMatchedRouteID() {
		return matchedRouteID;
	}

	public void setMatchedRouteID(int matchedRouteID) {
		this.matchedRouteID = matchedRouteID;
	}


	
	public Route(int tick, IAgent agent, LineString line) {
		this.agentID = agent.getID();
		this.setGeom(line);
		
	}
	
	@Column(name="id")
	@Id
	public int getId() {
		return id;
	}

}
