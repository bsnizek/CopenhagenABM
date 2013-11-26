package copenhagenabm.agent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;



/**
 * A measurement is a snapshot of the agent's values at a certain 
 * time in a certain location
 * 
 * 
 * @author besn
 *
 */
public class Measurement {

	private int ID;
	private Coordinate position;
	private double good;
	private double bad;
	private double tick;
	private String roadID;
	private int absoluteCrowding;
	private double crowdingFactor;
	
	GeometryFactory geomFac = new GeometryFactory();
	private double speed;

	public Measurement(int id, double tick) {
		this.ID = id;
		this.tick = tick;
	}

	public void setPosition(Coordinate position) {
		this.position = position;
	}

	public void setGoodValue(double goodValueAt) {
		this.good = goodValueAt;
	}

	public void setBadValue(double badValueAt) {
		this.bad = badValueAt;
		
	}

	public void setRoadID(String identifier) {
		roadID = identifier;
		
	}

	public void setAbsoluteCrowding(int roadLoad) {
		absoluteCrowding = roadLoad;
	}

	public void setCrowdingFactor(double cf) {
		crowdingFactor = cf;
		
	}
	
	public Point getGeometry() {
		return geomFac.createPoint(this.position);
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}

	public double getGood() {
		return good;
	}

	public void setGood(double good) {
		this.good = good;
	}

	public double getBad() {
		return bad;
	}

	public void setBad(double bad) {
		this.bad = bad;
	}

	public double getTick() {
		return tick;
	}

	public void setTick(double tick) {
		this.tick = tick;
	}

	public GeometryFactory getGeomFac() {
		return geomFac;
	}

	public void setGeomFac(GeometryFactory geomFac) {
		this.geomFac = geomFac;
	}

	public Coordinate getPosition() {
		return position;
	}

	public String getRoadID() {
		return roadID;
	}

	public int getAbsoluteCrowding() {
		return absoluteCrowding;
	}

	public double getCrowdingFactor() {
		return crowdingFactor;
	}

	public double getSpeed() {
		return speed;
	}
	
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	

}
