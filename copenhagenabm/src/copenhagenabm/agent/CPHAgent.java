/*
 * (c) Bernhard Snizek, b@snizek.com
 * © original copyright 2012 Nick Malleson
 * 
 * This file is part of Gertrude.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
 */

package copenhagenabm.agent;

import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

// import copenhagenabm.environment.NearestRoadCoordCache;
import copenhagenabm.environment.Person;
import copenhagenabm.environment.Road;
import copenhagenabm.environment.Zone;
import copenhagenabm.loggers.AgentHistoryKMZWriter;
import copenhagenabm.loggers.AgentDotLogger;
import copenhagenabm.loggers.BasicAgentLogger;
import copenhagenabm.loggers.RoadLoadLogger;
import copenhagenabm.main.AGENT_SPEED_MODES;
import copenhagenabm.main.CalibrationModeData;
import copenhagenabm.main.CalibrationModeData.CalibrationRoute;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.routes.MatchedGPSRoute;
import copenhagenabm.routes.OverlapCalculator;

import copenhagenabm.routes.Route;
import copenhagenabm.agent.PolyLineMover;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.gis.Geography;
import repastcity3.environment.Building;
import repastcity3.environment.Junction;
import repastcity3.environment.SpatialIndexManager;
import repastcity3.exceptions.NoIdentifierException;
import repastcity3.exceptions.RoutingException;

public class CPHAgent implements IAgent {

	private double instantiationTime = ContextManager.getModelRunSeconds();

	private int birthTick = 0;

	RoadLoadLogger roadLoadLogger = ContextManager.getRoadLoadLogger();

	/**
	 * Is the agent a calibration agent ?
	 */
	private boolean isCalibrationAgent = false;

	/**
	 * set to true when the agent terminates early
	 */
	private boolean didNotFindDestination = false;

	// a history of measurements, see more in the Measurement class
	private ArrayList<Measurement> history = new ArrayList<Measurement>();

	private static int uniqueID = 0;
	private int id;

	// newborn is true right after the agent is spawned, before she takes the
	// first move
	private boolean newborn = true;

	// agent has reached its goal, terminated set to true
	// private boolean terminated = false;

	/*
	 * The coordinate of the trip destination. The Centroid of the destination
	 * building projected onto the road network
	 */
	private Coordinate destinationCoordinate;

	// currentRoad is the road the agent resides at the moment
	private Road currentRoad;

	// the originPoint is the projection of the building onto the roadnetwork
	// and the point
	// the agent starts walking from
	// private Coordinate originPoint;

	private Junction targetJunction;

	double[] distAndAngle = new double[2];

	// double distToTravel = (GlobalVars.GEOGRAPHY_PARAMS.AGENT_SPEED) *
	// GlobalVars.GEOGRAPHY_PARAMS.TICK_LENGTH;

	private PolyLineMover plm;
	private Building originBuilding;

	// the route the agent has taken until the current point in time
	private Route route = null;

	// private HashMap<String, Boolean> visitedRoads = new HashMap<String,
	// Boolean>();

	// private static volatile NearestRoadCoordCache nearestRoadCoordCache;
	/*
	 * Store which road every building is closest to. This is used to
	 * efficiently add buildings to the agent's awareness space
	 */
	// private static volatile BuildingsOnRoadCache buildingsOnRoadCache;
	// To stop threads competing for the cache:
	// private static Object buildingsOnRoadCacheLock = new Object();

	private Coordinate sourceCoord;

	private int gpsRouteID;

	private MatchedGPSRoute matchedGPSRoute;



	private boolean toBeKilled = false;

	Person destinationPerson = null;

	private Zone birthZone = null;
	private Zone destinationZone;

	// a list of IDs of the roads visited
	private ArrayList<String> RoadHistory = new ArrayList<String>();

	private CalibrationModeData.CalibrationRoute calibrationRoute;

	private boolean isSuccessful = false;

	// the overlap between the GPS route and the simulated one (calibration agents only)
	private double overlap;

	private Coordinate deathLocation;

	public ArrayList<String> getRoadHistory() {
		return RoadHistory;
	}

	public void setRoadHistory(ArrayList<String> roadHistory) {
		RoadHistory = roadHistory;
	}

	public Zone getBirthZone() {
		return birthZone;
	}

	public void setBirthZone(Zone birthZone) {
		this.birthZone = birthZone;
	}

	public Zone getDestinationZone() {
		return destinationZone;
	}

	public void setDestinationZone(Zone destinationZone) {
		this.destinationZone = destinationZone;
	}

	public Person getDestinationPerson() {
		return destinationPerson;
	}

	public void setDestinationPerson(Person destinationPerson) {
		this.destinationPerson = destinationPerson;
	}

	public Coordinate getSourceCoord() {
		return sourceCoord;
	}

	/**
	 * Returns the unique ID of the agent
	 * 
	 * @return
	 */
	public int getID() {
		return this.id;
	}

	public boolean isAtDestination() {

		double distance = getOrthodromicDistance(this.getPosition(),
				this.getDestinationCoordinate());
		int snapDistance = ContextManager.getDistanceSnap();
		if (distance < snapDistance) {
			// System.out.println("Agent " + this.getID() + " on edge " +
			// this.getRoute().getGPSID() + " is at destination");
			ContextManager.resetNumberOfKills();
			this.toBeKilled = true;
			return true;
		}
		return false;
	}

	/**
	 * Returns true when the simulated route exceeds more than 50% of the GPS
	 * route
	 */
	public boolean isMoreThan50PercentOverGPSRouteDistance() {
		Route r = this.getRoute();
		if (r.getGeometry() != null) {
			double matchedGPSRouteLength = this.matchedGPSRoute
					.getLengthInMetres();
			double routeLength = r.getLengthInMetres();
			double moreThan50Percent = matchedGPSRouteLength * 1.5;
			if (ContextManager.getPOSITION_DEBUG_MODE()) {
				System.out.println("(" + ContextManager.getCurrentTick()
						+ ") + A(" + this.getID() + ") <50% " + routeLength
						+ " < " + moreThan50Percent);
			}
			return (routeLength >= moreThan50Percent);
		} else {
			return false;
		}
	}

	public double getOrthodromicDistance(Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(
				ContextManager.roadProjection.getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		// System.out.println(c2.distance(c1) + " - " +
		// calculator.getOrthodromicDistance());
		return calculator.getOrthodromicDistance();
	}

	/**
	 * Constructor for an agent given 2 coordinates.
	 * 
	 * We use this one for the calibration model
	 * 
	 * GPSID is the ID of the GPS track
	 * 
	 * @param sourceCoord
	 *            the coordinate of birth
	 * @param destinationCoord
	 *            the coordinate of the destination
	 * @param GPSID
	 *            the original GPS route ID
	 * @param matchedGPSRoute
	 *            the matchedGPS Route
	 * @param nIter
	 *            the number of the current iteration for the current route (
	 *            0..max number of iterations -1 )
	 */
	public CPHAgent(
			Coordinate sourceCoord,
			Coordinate destinationCoord,
			MatchedGPSRoute matchedGPSRoute,
			int nIter) {

		this.setDestinationCoordinate(destinationCoord);

		this.setSourceCoord(sourceCoord);
		this.id = uniqueID++;
		this.gpsRouteID = matchedGPSRoute.getOBJECTID();
		this.setRoute(new Route(this.id, this.gpsRouteID, ContextManager.getModelRunID())); // initialize

		this.isCalibrationAgent = true; // yes, we have a calibration agent
		this.matchedGPSRoute = matchedGPSRoute;
		double totLengthGPSRoute = matchedGPSRoute.getLengthInMetres();

		System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + 
				this.getID() + ") GPS(" + matchedGPSRoute.getOBJECTID() + 
				") CPHAgent instantiated, " + ContextManager.getCalibrationLets().size() + " to go.");

		this.calibrationRoute = new CalibrationModeData().new CalibrationRoute(this, 
				sourceCoord,
				destinationCoord, 
				this.gpsRouteID, 
				totLengthGPSRoute, 
				nIter);
	}

	/**
	 * 
	 * Constructor given a source building and a destination zone.
	 * 
	 * @param b
	 * @param zone
	 */
	public CPHAgent(Building b, Zone zone) {

		this.originBuilding = b;
		this.id = uniqueID++;
		this.destinationZone = zone;
		this.birthZone = b.getZone();

		// this.setRoute(new Route(this.id, this.getGpsRouteID(),
		// ContextManager.getModelRunID())); // this is a little hacked - we set
		// the GPS ID to the agent ID as well.

		try {
			destinationPerson = zone.getRandomPerson();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {

			this.setDestinationCoordinate(ContextManager
					.getRoadCoordinateForBuilding(destinationPerson
							.getBuilding()));

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			birthTick = ContextManager.getCurrentTick();
		} catch (Exception e) {
			birthTick = 0;
		}

	}

	public Building getOriginBuilding() {
		return originBuilding;
	}

	public void setOriginBuilding(Building originBuilding) {
		this.originBuilding = originBuilding;
	}

	private void setSourceCoord(Coordinate sourceCoord) {
		this.sourceCoord = sourceCoord;

	}

	public Road getRoadByCoordinate(Coordinate c) {
		return ContextManager.getCopenhagenABMTools().getSnapTool()
				.getRoadByCoordinate(c);
	}

	public void snapAgentToRoad() {

		Coordinate c = null;

		if (ContextManager.inCalibrationMode()) {

			c = this.getSourceCoord();

			ContextManager.moveAgent(this, ContextManager.getGeomFac().createPoint(c)); // move the
			// agent to
			// the
			// projection
			// on the
			// road
			// network*/
			Road r = getRoadByCoordinate(c);

			if (r == null) {
				if (ContextManager.getDEBUG_MODE()) {
					System.out.println("No road returned for Coordinate " + c);
				}
				// int foo=1;
			}

			setCurrentRoad(r);
			// build the geometry from the point of entry to the end of the
			this.addToRoute(r, r.getGeometry());

		} else {

			// project the coordinate from the building onto the road
			c = ContextManager
					.getRoadCoordinateForBuilding(this.originBuilding);

			ContextManager.moveAgent(this, ContextManager.getGeomFac().createPoint(c)); // move the
			// agent to
			// the
			// projection
			// on the
			// road
			// network*/

			Road road = ContextManager.getRoadForBuilding(this.originBuilding);
			setCurrentRoad(road);
			// build the geometry from the point of entry to the end of the
			// this.addToRoute(road, road.getGeometry());

			this.sourceCoord = c;

		}

	}

	/**
	 * Adds an edge to a route
	 * 
	 * @param r
	 * @param g
	 */
	private void addToRoute(Road r, Geometry g) {
		this.getRoute().addEdgeGeometry(r, g);
	}

	@Override
	// @SuppressWarnings("unchecked")
	public void step() {

		boolean isKilled = false;

		if (isNewborn()) {

			// let us jump onto the network

			Coordinate firstCoordinateOnRoad = null;
			if (this.sourceCoord != null) {
				firstCoordinateOnRoad = this.sourceCoord;
			} else {
				firstCoordinateOnRoad = ContextManager
						.getNearestRoadCoordinateCache().getCoordinate(
								this.originBuilding);
			}

			Road cr = getCurrentRoad();

			if (roadLoadLogger != null) {
				try {
					roadLoadLogger.addEntry(cr.getIdentifier());
				} catch (NoIdentifierException e) {
					e.printStackTrace();
				}
			}

			// build two dummy roads, one from current position to junction 1
			// one to 2

			ArrayList<Junction> junctions = cr.getJunctions();

			Road r1 = new Road(cr, firstCoordinateOnRoad, junctions.get(0),
					"-1");
			Road r2 = new Road(cr, firstCoordinateOnRoad, junctions.get(1),
					"-2");

			ArrayList<Road> roads = new ArrayList<Road>();

			// debug stuff - remove when it's working
			if (r1.getGeometry() == null && r2.getGeometry() == null) {
				r1 = new Road(cr, firstCoordinateOnRoad, junctions.get(0), "-1");
				r2 = new Road(cr, firstCoordinateOnRoad, junctions.get(1), "-1");
			}

			Road newRoad;

			if (r1.getGeometry() != null) {
				roads.add(r1);
			}

			if (r2.getGeometry() != null) {
				roads.add(r2);
				// r2.getGeometry();
			}

			if (((r1.getGeometry() == null) && (r2.getGeometry() == null))) {
				if (ContextManager.getDEBUG_MODE()) {
					System.out.println("BOTH r1 and r2 = null -> PROBLEM");
				}
			}

			if (roads.size() == 1) {
				newRoad = roads.get(0);
			} else {
				EdgeSelector es = new EdgeSelector(roads, null, this);

				if (ContextManager.isDecisionLoggerOn()) {

					es.getDecisionMatrix().printMatrix();

				}

				newRoad = es.getRoad();
			}

			if (newRoad == null) {
				if (ContextManager.getDEBUG_MODE()) {
					System.out.println("new Road = null");
				}
			}

			Junction tJ = newRoad.getTargetJunction();
			if (tJ == null) {
				if (ContextManager.getDEBUG_MODE()) {
					System.out.println("STOP");
				}
			}
			Point newTargetPoint = ContextManager.getGeomFac().createPoint(tJ.getCoords());
			Point crSourcePoint = ContextManager.getGeomFac().createPoint(cr.getEdge().getSource()
					.getCoords());

			double d = ContextManager.simpleDistance.distance(crSourcePoint,
					newTargetPoint);

			if (d == 0.0d) {
				this.setTargetJunction(cr.getEdge().getSource());
			} else {
				this.setTargetJunction(cr.getEdge().getTarget());
			}

			ContextManager.moveAgent(this,
					ContextManager.getGeomFac().createPoint(firstCoordinateOnRoad));

			// double x =
			// ContextManager.roadProjection.getGeometry(currentRoad).distance(fact.createPoint(currentCoord));

			setNewborn(false);

			this.plm = new PolyLineMover(this, currentRoad, targetJunction);

		} else {

			// not newborn

			// step length in seconds
			double stepLength = ContextManager.getStepLength(); // in seconds
			double speed = this.getSpeed(); // in m/s

			double distance = speed * stepLength;

			if (ContextManager.getPOSITION_DEBUG_MODE()) {
				System.out.println(this.getPosition());
			}
			OvershootData overshoot = plm.move(distance);

			if (ContextManager.getPOSITION_DEBUG_MODE()) {
				System.out.println(this.getPosition());
			}
			if (this.getOrthodromicDistance(this.getPosition(),
					destinationCoordinate) < ContextManager.getDistanceSnap()) {
				plm.terminate(destinationCoordinate);
			}

			if (overshoot != null) {
				this.plm = new PolyLineMover(this, overshoot.getRoad(),
						overshoot.getTargetJunction());
			}

		}

		if (!isKilled) {
			Measurement m = new Measurement(this.getID(), RunEnvironment
					.getInstance().getCurrentSchedule().getTickCount());

			Coordinate pos = this.getPosition();
			m.setPosition(pos);

			m.setSpeed(this.getSpeed());

			if (ContextManager.useExperiences()) {

				m.setGoodValue(ContextManager.getGoodValueAt(pos));
				m.setBadValue(ContextManager.getBadValueAt(pos));

			}

			String roadID = "";

			try {
				roadID = this.getCurrentRoad().getIdentifier();
			} catch (NoIdentifierException e) {
				e.printStackTrace();
			}

			m.setRoadID(roadID);

			history.add(m);
		}

		if (this.isAtDestination()) {
			this.prepareForRemoval(true);
		}

	}

	public double getSpeed() {
		if (ContextManager.getAgentSpeedMode() == AGENT_SPEED_MODES.STATIC) {
			return ContextManager.getAgentSpeed();
		} else {
			return 1.0;
		}
	}

	public void calcOverlap() {

		OverlapCalculator oc = new OverlapCalculator();
		oc.addMatchedGPSRoute(this.matchedGPSRoute);
		oc.addSimulatedRoute(this.getRoute());
		this.setOverlap(oc.getOverlap());

	}



	public void setDeathLocation(Coordinate position) {
		this.deathLocation = position;
	}

	public void writeHistory(int modelRun) {

		if (ContextManager.inCalibrationMode()) {

			int matchedEdges = this.matchedGPSRoute.getNumberOfEdges();

			if (matchedEdges == 0) {

				ContextManager.getCalibrationModeData()
				.incrementNumberOfCanceledAgents();

			} else {

				String[] xx = new Double(this.getOverlap()).toString().split(
						"\\.");
				String newPathSizeString = xx[0] + "," + xx[1];

				// log into the calibration logger
				ContextManager.getCalibrationLogger().logLine(
						newPathSizeString + ";"
								+ ContextManager.getAngleToDestination() + ";"
								+ this.matchedGPSRoute.getOBJECTID() + ";"
								+ this.matchedGPSRoute.getNumberOfEdges());
				ContextManager.getCalibrationLogger().pathSizes.add(this
						.getOverlap());

			}

		}

		if (ContextManager.dumpAgentHistoryIntoDotFile()) {

			AgentDotLogger cnl = new AgentDotLogger(
					ContextManager.getProperty("AgentHistoryDirectory"),
					this.getID(), modelRun);
			try {
				cnl.writeHistory(history);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (ContextManager.dumpIntoKMLFile()) {

			AgentHistoryKMZWriter ahw = new AgentHistoryKMZWriter(
					ContextManager.getProperty("AgentHistoryDirectory"),
					this.getID(), modelRun);

			try {
				ahw.writeHistory(history);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if (isExplicative()) {
			// ContextManager.getRouteStore().addSimulatedRoute(this.getRoute());
			// System.out.println("Routes added to route store for agentID=" +
			// this.getID());

			// add the route to the route context
			// ContextManager.getRouteContext().add(this.getRoute());
		}

	}

	/**
	 * Find the nearest object in the given geography to the coordinate.
	 * 
	 * @param <T>
	 * @param x
	 *            The coordinate to search from
	 * @param geography
	 *            The given geography to look through
	 * @param closestPoints
	 *            An optional List that will be populated with the closest
	 *            points to x (i.e. the results of
	 *            <code>distanceOp.closestPoints()</code>.
	 * @param searchDist
	 *            The maximum distance to search for objects in. Small distances
	 *            are more efficient but larger ones are less likely to find no
	 *            objects.
	 * @return The nearest object.
	 * @throws RoutingException
	 *             If an object cannot be found.
	 */
	public static synchronized <T> T findNearestObject(Coordinate x,
			Geography<T> geography, List<Coordinate> closestPoints,
			GlobalVars.GEOGRAPHY_PARAMS.BUFFER_DISTANCE searchDist)
					throws RoutingException {
		if (x == null) {
			throw new RoutingException(
					"The input coordinate is null, cannot find the nearest object");
		}

		T nearestObject = SpatialIndexManager.findNearestObject(geography, x,
				closestPoints, searchDist);

		if (nearestObject == null) {
			throw new RoutingException(
					"Couldn't find an object close to these coordinates:\n\t"
							+ x.toString());
		} else {
			return nearestObject;
		}
	}

	/**
	 * There will be no inter-agent communication so these agents can be
	 * executed simulataneously in separate threads.
	 */
	// @Override
	public final boolean isThreadable() {
		return true;
	}

	// @Override
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
	}

	// // @Override
	// public List<String> getTransportAvailable() {
	// return null;
	// }

	@Override
	public String toString() {

		Coordinate position = this.getPosition();

		String roadID = "";

		try {
			roadID = " R=" + this.getCurrentRoad().getIdentifier();
		} catch (NoIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "Agent " + this.id + " (" + position.x + "/" + position.y + ")"
		+ roadID;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CPHAgent))
			return false;
		CPHAgent b = (CPHAgent) obj;
		return this.id == b.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public boolean isNewborn() {
		return newborn;
	}

	public void setNewborn(boolean newborn) {
		this.newborn = newborn;
	}

	/*
	 * Returns the current position or location of the agent
	 */
	public Coordinate getPosition() {
		Coordinate xx = ContextManager.getAgentGeometry(this).getCoordinate();
		return xx;
	}

	public Coordinate getDestinationCoordinate() {
		return destinationCoordinate;
	}

	public void setDestinationCoordinate(Coordinate destinationCoordinate) {
		this.destinationCoordinate = destinationCoordinate;
	}

	public Road getCurrentRoad() {

		if (currentRoad == null) {
			int id = this.getID();
			if (ContextManager.getDEBUG_MODE()) {
				System.out.println("currentRoad=zero Agent ID=" + id);
			}
		}

		try {

			// String idtfr = currentRoad.getIdentifier();

			// System.out.println("(" + ContextManager.getCurrentTick() +
			// ") currentroad = " + idtfr);

			if ((currentRoad.getIdentifier().equals("-1"))
					|| (currentRoad.getIdentifier().equals("-2"))) {
				currentRoad = currentRoad.getParentRoad();
			}
		} catch (NoIdentifierException e) {
			e.printStackTrace();
		}

		return currentRoad;
	}

	/*
	 * 
	 * sets the agent onto the crowding network.
	 */
	public void setCurrentRoad(Road cR) {

		if (cR == null) {
			if (ContextManager.getDEBUG_MODE()) {
				System.out.println("setCurrentRoad(null)");
			}
		}

		if (ContextManager.inCalibrationMode()) {

			// RoadNetwork rn = ContextManager.getTheRoadNetwork();
			this.currentRoad = cR;

		} else {

			this.currentRoad = cR;

			/*
			 * if (ContextManager.isCrowdingLoggerOn()) {
			 * 
			 * RoadNetwork rn = ContextManager.getCrowdingNetwork();
			 * 
			 * if (this.currentRoad != null) {
			 * 
			 * // get the crowding network
			 * 
			 * if (rn.hasRoad(this.currentRoad)) { rn.removeAgentFromRoad(this,
			 * this.currentRoad); }
			 * 
			 * this.currentRoad = currentRoad;
			 * 
			 * rn.addAgentToRoad(this, this.currentRoad); } else { // first
			 * entry onto an edge rn.addAgentToRoad(this, currentRoad);
			 * this.currentRoad = currentRoad;
			 * 
			 * } }
			 */
		}

	}

	public Junction getTargetJunction() {
		return targetJunction;
	}

	public void setTargetJunction(Junction targetJunction) {
		this.targetJunction = targetJunction;
	}

	// public boolean isTerminated() {
	// return terminated;
	// }

	// public void setTerminated(boolean terminated) {
	// this.terminated = terminated;
	// }

	public int getGpsRouteID() {
		return gpsRouteID;
	}

	public void setGpsRouteID(int gpsRouteID) {
		this.gpsRouteID = gpsRouteID;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public boolean isToBeKilled() {
		return this.toBeKilled;
	}

	@Override
	public void logBasics() {

		if (this.getBasicAgentLogger() != null) {

			Zone bZ = getBirthZone();
			String bZID = null;
			String dZID = null;

			if (bZ == null) {
				bZID = "null";
				dZID = "null";
			} else {

				try {
					bZID = getBirthZone().getIdentifier();
				} catch (NoIdentifierException e) {
					e.printStackTrace();
				}

				try {
					dZID = getDestinationZone().getIdentifier();
				} catch (NoIdentifierException e) {
					e.printStackTrace();
				}
			}
			this.getBasicAgentLogger().doLog(this.getID(), this.getBirthTick(),
					ContextManager.getCurrentTick(), bZID, dZID,
					this.getSourceCoord(), this.getDestinationCoordinate());
		}
	}

	public void setToBeKilled(boolean toBeKilled) {
		this.toBeKilled = toBeKilled;
	}

	// adds the current road to the road history
	public void addRoadToRoadHistory() {

		try {
			this.RoadHistory.add(this.currentRoad.getIdentifier());
		} catch (NoIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	/* (non-Javadoc)
	 * @see copenhagenabm.agent.IAgent#finishCalibrationData()
	 * 
	 * Collects the calibration route data and throws it over into the CalibrationRoute object.
	 * 
	 */
	public void finishCalibrationData() {
		this.calibrationRoute.setRoute(this.getRoute().getRouteAsLineString());

		if (this.getDeathLocation() != null) {
			this.calibrationRoute.setDeath(this.getDeathLocation());
		}

		this.calibrationRoute.setAgent(this);

		ContextManager.getCalibrationModeData().addCalibrationRoute(
				this.getCalibrationRoute());

		this.calibrationRoute.setEdge_lngth_avg(this.getRoute().getAverageEdgeLength());

		this.calibrationRoute.setCalctime(ContextManager.getModelRunSeconds() - this.getInstantiationTime());

		this.calibrationRoute.setOverlap(getOverlap());

		this.calibrationRoute.setSuccessful(this.isSuccessful());
		
		ContextManager.getCalibrationModeData().addCalibrationRoute(this.calibrationRoute);

	}

	public CalibrationRoute getCalibrationRoute() {
		return this.calibrationRoute;
	}

	public void setCalibrationRoute(
			CalibrationModeData.CalibrationRoute calibrationRoute) {
		this.calibrationRoute = calibrationRoute;
	}

	@Override
	public void setSuccessful(boolean b) {
		this.isSuccessful = b;
		this.getCalibrationRoute().setSuccessful(b);

	}

	public boolean isSuccessful() {
		return isSuccessful;
	}

	public double getOverlap() {
		return overlap;
	}

	public void setOverlap(double overlap) {
		this.overlap = overlap;
	}

	public boolean isCalibrationAgent() {
		return isCalibrationAgent;
	}

	public int getBirthTick() {
		return birthTick;
	}

	public void setBirthTick(int birthTick) {
		this.birthTick = birthTick;
	}

	public BasicAgentLogger getBasicAgentLogger() {
		return ContextManager.getBasicAgentLogger();
	}

	public boolean isDidNotFindDestination() {
		return didNotFindDestination;
	}

	public void setDidNotFindDestination(boolean didNotFindDestination) {
		this.didNotFindDestination = didNotFindDestination;
	}

	public boolean isExplicative() {
		return isCalibrationAgent;
	}

	public void setCalibrationAgent(boolean explicative) {
		this.isCalibrationAgent = explicative;
	}

	public ArrayList<Measurement> getHistory() {
		return history;
	}

	public void setHistory(ArrayList<Measurement> history) {
		this.history = history;
	}

	public RoadLoadLogger getRoadLoadLogger() {
		return roadLoadLogger;
	}

	public void setRoadLoadLogger(RoadLoadLogger roadLoadLogger) {
		this.roadLoadLogger = roadLoadLogger;
	}

	public static int getUniqueID() {
		return uniqueID;
	}

	public static void setUniqueID(int uniqueID) {
		CPHAgent.uniqueID = uniqueID;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double[] getDistAndAngle() {
		return distAndAngle;
	}

	public void setDistAndAngle(double[] distAndAngle) {
		this.distAndAngle = distAndAngle;
	}

	public PolyLineMover getPlm() {
		return plm;
	}

	public void setPlm(PolyLineMover plm) {
		this.plm = plm;
	}

	public MatchedGPSRoute getMatchedGPSRoute() {
		return matchedGPSRoute;
	}

	public void setMatchedGPSRoute(MatchedGPSRoute matchedGPSRoute) {
		this.matchedGPSRoute = matchedGPSRoute;
	}

	public Coordinate getDeathLocation() {
		return deathLocation;
	}

	public double getInstantiationTime() {
		return instantiationTime;
	}

	public void setInstantiationTime(double instantiationTime) {
		this.instantiationTime = instantiationTime;
	}

	public void prepareForRemoval(boolean isSuccessful) {

		if (isSuccessful) {
			ContextManager.getCalibrationModeData().incrementSuccessfullyModeledRoutes();
			System.out.println("(" + ContextManager.getCurrentTick() + ") " +  "A(" + getID() + ") at destination.");
		}

		this.writeHistory(ContextManager.getModelRunID());

		//		ContextManager.moveAgent(this, ContextManager.getGeomFac().createPoint(this.getDestinationCoordinate()));

		ContextManager.logToPostgres(this);

		this.setSuccessful(isSuccessful);

		ContextManager.removeAgent(this);

	}


}
