package copenhagenabm.orm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.main.CalibrationModeData.CalibrationRoute;
import copenhagenabm.main.ContextManager;

/**
 * 
 * CalibrationRouteDBObject 
 * 
 * The orm defnition module for the detailed calibration information.
 * See more doc in results.docx 
 * 
 * @author Bernhard Snizek, b@snizek.com, +45 2371 0046 
 *
 */


@Entity
@Table(name="calibrationroute")
public class CalibrationRouteDBObject {

	private LineString geom;

	private int ID;
	private int matchedRouteID;
	private int n_iter;
	private boolean success;
	private double overlap;
	private double edge_lngth_avg;
	private double route_gps_lngth;
	private double route_sim_lngth;
	private Point origin;
	private Point destination;
	private Point death;
	private double calctime;
	private double origo_dest_dist;
//	private int agentID;

	private GeometryFactory geomFact = ContextManager.getGeomFac();

	private CalibrationRoute calibrationRoute;

	private int agentid;


	/**
	 * CalibrationRouteDBObject
	 * 
	 * An object in the calibrationroute table.
	 * Instantiate it with a CalibrationRoute which is stored in the agents CalibrationModeData.
	 * 
	 * @param c
	 */
	public CalibrationRouteDBObject(CalibrationRoute c) {

		this.geom = c.getRoute();

		this.matchedRouteID = c.getGPSRouteID();
		this.n_iter = c.getnIter();
		this.success = c.isSuccessful();
		this.overlap = c.getOverlap();
		this.route_gps_lngth = c.getRoute_gps_lngth();
		this.route_sim_lngth = c.getAgent().getRoute().getLengthInMetres();
		this.edge_lngth_avg = c.getAgent().getRoute().getAverageEdgeLength();

		this.origin = geomFact.createPoint(c.getOrigin());
		this.destination = geomFact.createPoint(c.getDestination());
		this.death = geomFact.createPoint(c.getDeath());

		this.calctime = c.getCalctime();
		
		this.setCalibrationRoute(c);

		// TODO: *** more fields here

	}

	@Column(name="geom")
	@Type(type = "org.hibernatespatial.postgis.PGGeometryUserType")
	public LineString getGeom() {
		return geom;
	}

	public void setGeom(LineString geom) {
		this.geom = geom;
		this.geom.setSRID(4326);
	}

	@Column(name="id")
	@Id
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}

	@Column(name="matchedrouteid")
	public int getMatchedrouteid() {
		return matchedRouteID;
	}

	public void setMatchedrouteid(int matchedRouteID) {
		this.matchedRouteID = matchedRouteID;
	}

	@Column(name="n_iter")
	public int getN_iter() {
		return n_iter;
	}

	public void setN_iter(int n_iter) {
		this.n_iter = n_iter;
	}

	@Column(name="success")
	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Column(name="overlap")
	public double getOverlap() {
		return overlap;
	}

	public void setOverlap(double overlap) {
		this.overlap = overlap;
	}

	@Column(name="edge_lngth_avg")
	public double getEdge_lngth_avg() {
		return edge_lngth_avg;
	}

	public void setEdge_lngth_avg(double edge_lngth_avg) {
		this.edge_lngth_avg = edge_lngth_avg;
	}


	@Column(name="route_gps_lngth")
	public double getRoute_gps_lngth() {
		return route_gps_lngth;
	}

	public void setRoute_gps_lngth(double route_gps_lngth) {
		this.route_gps_lngth = route_gps_lngth;
	}

	@Column(name="route_sim_lngth")
	public double getRoute_sim_lngth() {
		return route_sim_lngth;
	}

	public void setRoute_sim_lngth(double route_sim_lngth) {
		this.route_sim_lngth = route_sim_lngth;
	}

	@Column(name="origin")
	public Point getOrigin() {
		return origin;
	}

	public void setOrigin(Point origin) {
		this.origin = origin;
	}

	@Column(name="destination")
	public Point getDestination() {
		return destination;
	}

	public void setDestination(Point destination) {
		this.destination = destination;
	}

	@Column(name="death")
	public Point getDeath() {
		return death;
	}

	public void setDeath(Point death) {
		this.death = death;
	}


	@Column(name="calctime")
	public double getCalctime() {
		return calctime;
	}

	public void setCalctime(double calctime) {
		this.calctime = calctime;
	}

	@Column(name="origo_dest_dist")
	public double getOrigo_dest_dist() {
		return ContextManager.simpleDistance.distance(this.getOrigin(), this.getDestination());
	}

	public void setOrigo_dest_dist(double origo_dest_distance) {
		this.origo_dest_dist = origo_dest_distance;
	}

	@Column(name="agentid")
	public int getAgentid() {
		return this.getCalibrationRoute().getAgent().getID();
	}
	
	public void setAgentid(int id) {
		agentid = id;
	}

	public CalibrationRoute getCalibrationRoute() {
		return calibrationRoute;
	}

	public void setCalibrationRoute(CalibrationRoute calibrationRoute) {
		this.calibrationRoute = calibrationRoute;
	}

}
