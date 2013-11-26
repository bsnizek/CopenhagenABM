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
import com.vividsolutions.jts.geom.GeometryFactory;
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

	private int birthTick = 0;

	RoadLoadLogger roadLoadLogger = ContextManager.getRoadLoadLogger();

	// private BasicAgentLogger basicAgentLogger = new BasicAgentLogger();

	/**
	 * Is the agent a calibration agent ? 
	 */
	private boolean isCalibrationAgent = false;


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

	private boolean didNotFindDestination = false;

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

	// a history of measurements, see more in the Measurement class
	private ArrayList<Measurement> history = new ArrayList<Measurement>();


	public ArrayList<Measurement> getHistory() {
		return history;
	}

	public void setHistory(ArrayList<Measurement> history) {
		this.history = history;
	}

	GeometryFactory fact = new GeometryFactory();

	private static int uniqueID = 0;
	private int id;

	// newborn is true right after the agent is spawned, before she takes the first move
	private boolean newborn = true;

	// agent has reached its goal, terminated set to true
//	private boolean terminated = false;

	/*
	 * The coordinate of the trip destination. The Centroid of the destination building 
	 * projected onto the road network
	 */
	private Coordinate destinationCoordinate;

	// currentRoad is the road the agent resides at the moment
	private Road currentRoad;

	// the originPoint is the projection of the building onto the roadnetwork and the point
	// the agent starts walking from
	// private Coordinate originPoint;

	private Junction targetJunction;

	double[] distAndAngle = new double[2];

	//	double distToTravel = (GlobalVars.GEOGRAPHY_PARAMS.AGENT_SPEED) * 
	//			GlobalVars.GEOGRAPHY_PARAMS.TICK_LENGTH;

	private PolyLineMover plm;
	private Building originBuilding;


	// the route the agent has taken until the current point in time
	private Route route = null;

	//	private HashMap<String, Boolean> visitedRoads = new HashMap<String, Boolean>();

	// private static volatile NearestRoadCoordCache nearestRoadCoordCache;
	/*
	 * Store which road every building is closest to. This is used to efficiently add buildings to the agent's awareness
	 * space
	 */
	// private static volatile BuildingsOnRoadCache buildingsOnRoadCache;
	// To stop threads competing for the cache:
	//	private static Object buildingsOnRoadCacheLock = new Object();

	private Coordinate sourceCoord;

	private int gpsRouteID;

	private MatchedGPSRoute matchedGPSRoute;

	public MatchedGPSRoute getMatchedGPSRoute() {
		return matchedGPSRoute;
	}

	public void setMatchedGPSRoute(MatchedGPSRoute matchedGPSRoute) {
		this.matchedGPSRoute = matchedGPSRoute;
	}

	private boolean toBeKilled = false;

	Person destinationPerson = null;

	private Zone birthZone = null;
	private Zone destinationZone;


	// a list of IDs of the roads visited
	private ArrayList<String> RoadHistory = new ArrayList<String>();

	private CalibrationModeData.CalibrationRoute calibrationRoute;

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

		double distance = getOrthodromicDistance(this.getPosition(), this.getDestinationCoordinate());
		int snapDistance = ContextManager.getDistanceSnap();
		if (distance < snapDistance) {
			// System.out.println("Agent " + this.getID() + " on edge " + this.getRoute().getGPSID() + " is at destination");
			ContextManager.resetNumberOfKills();
			this.toBeKilled = true;
			return true;
		}
		return false;
	}

	/**
	 * Returns true when the simulated route exceeds more than 50% of the GPS route
	 */
	public boolean isMoreThan50PercentOverGPSRouteDistance() {
		Route r = this.getRoute();
		if (r.getGeometry() != null) {
			double matchedGPSRouteLength = this.matchedGPSRoute.getLengthInMetres();
			double routeLength = r.getLengthInMetres();
			double moreThan50Percent = matchedGPSRouteLength * 1.5;
			if (ContextManager.getDEBUG_MODE()) {
				System.out.println("(" + ContextManager.getCurrentTick() + ") + A(" + this.getID() + ") <50% " + routeLength + " < " + moreThan50Percent);
			}
			return (routeLength >= moreThan50Percent);
		} else {
			return false;
		}
	}

	public double getOrthodromicDistance(Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(ContextManager.roadProjection.getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		// System.out.println(c2.distance(c1) + " - " + calculator.getOrthodromicDistance());
		return calculator.getOrthodromicDistance();
	}

	/*
	 * Constructor for an agent given 2 coordinates. 
	 *
	 * We use this one for the calibration model
	 * 
	 * GPSID is the ID of the GPS track
	 *
	 */ 
	public CPHAgent(Coordinate sourceCoord, 
			Coordinate destinationCoord, 
			int GPSID,
			MatchedGPSRoute matchedGPSRoute) {

		this.setDestinationCoordinate(destinationCoord);

		// this is an unclean workaround
		this.setSourceCoord(sourceCoord);
		this.id = uniqueID++;
		this.gpsRouteID = GPSID;
		this.setRoute(new Route(this.id, GPSID, ContextManager.getModelRunID()));		// initialize the route and set the GPS ID = the ID of the route tracked by GPS
		this.isCalibrationAgent=true;	// yes, we have a calibration agent
		this.matchedGPSRoute = matchedGPSRoute;
		double totLengthGPSRoute = matchedGPSRoute.getLengthInMetres();
		CalibrationModeData cmd = new CalibrationModeData() ;
		this.calibrationRoute = cmd.new CalibrationRoute(sourceCoord, destinationCoord, GPSID, totLengthGPSRoute);
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

		//		this.setRoute(new Route(this.id, this.getGpsRouteID(), ContextManager.getModelRunID()));	// this is a little hacked - we set the GPS ID to the agent ID as well.

		try {
			destinationPerson = zone.getRandomPerson();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {

			this.setDestinationCoordinate(ContextManager.getRoadCoordinateForBuilding(destinationPerson.getBuilding()));

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
		return ContextManager.getCopenhagenABMTools().getSnapTool().getRoadByCoordinate(c);	
	}


	public void snapAgentToRoad() {

		Coordinate c = null;

		if (ContextManager.inCalibrationMode()) {

			c = this.getSourceCoord();

			ContextManager.moveAgent(this, fact.createPoint(c)); // move the agent to the projection on the road network*/
			Road r = getRoadByCoordinate(c);

			if (r==null) {
				if (ContextManager.getDEBUG_MODE()) {
					System.out.println("No road returned for Coordinate " + c);
				}
//				int foo=1;
			}

			setCurrentRoad(r);
			// build the geometry from the point of entry to the end of the 
			this.addToRoute(r, r.getGeometry());

		} else {

			// project the coordinate from the building onto the road 
			c = ContextManager.getRoadCoordinateForBuilding(this.originBuilding);

			ContextManager.moveAgent(this, fact.createPoint(c)); // move the agent to the projection on the road network*/

			Road road = ContextManager.getRoadForBuilding(this.originBuilding);
			setCurrentRoad(road);
			// build the geometry from the point of entry to the end of the 
			//			this.addToRoute(road, road.getGeometry());

			this.sourceCoord = c;

		}

	}

	/**
	 * Adds an edge to a route
	 * @param r
	 * @param g 
	 */
	private void addToRoute(Road r, Geometry g) {
		this.getRoute().addEdgeGeometry(r, g);
	}

	@Override
	//	@SuppressWarnings("unchecked")
	public void step() {

		boolean isKilled=false;

		if (isNewborn()) {

			// let us jump onto the network

			Coordinate firstCoordinateOnRoad = null;
			if (this.sourceCoord != null) {
				firstCoordinateOnRoad = this.sourceCoord;
			} else {
				firstCoordinateOnRoad = ContextManager.getNearestRoadCoordinateCache().getCoordinate(this.originBuilding);
			}

			Road cr = getCurrentRoad();

			if (roadLoadLogger != null) {
				try {
					roadLoadLogger.addEntry(cr.getIdentifier());
				} catch (NoIdentifierException e) {
					e.printStackTrace();
				}
			}

			// build two dummy roads, one from current position to junction 1 one to 2 

			ArrayList<Junction> junctions = cr.getJunctions();

			//			Junction junction1 = junctions.get(0);
			//			Junction junction2 = junctions.get(1);

			//			double d1 = firstCoordinateOnRoad.distance(junction1.getCoords());
			//			double d2 = firstCoordinateOnRoad.distance(junction2.getCoords());

			// TODO: remove again, only for debug reason

			//			Geometry ggg = cr.getGeometry();

			Road r1 = new Road(cr, firstCoordinateOnRoad, junctions.get(0), "-1");
			Road r2 = new Road(cr, firstCoordinateOnRoad, junctions.get(1), "-2");

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
				//				r2.getGeometry();
			}

			if (((r1.getGeometry()==null) && (r2.getGeometry()==null))) {
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

			if (newRoad==null) {
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
			Point newTargetPoint = fact.createPoint(tJ.getCoords());
			Point crSourcePoint = fact.createPoint(cr.getEdge().getSource().getCoords());

			double d = ContextManager.simpleDistance.distance(crSourcePoint, newTargetPoint);


			if (d == 0.0d) {
				this.setTargetJunction(cr.getEdge().getSource());
			} else {
				this.setTargetJunction(cr.getEdge().getTarget());
			}

			ContextManager.moveAgent(this, fact.createPoint(firstCoordinateOnRoad));

			// double x = ContextManager.roadProjection.getGeometry(currentRoad).distance(fact.createPoint(currentCoord));

			setNewborn(false);

			this.plm = new PolyLineMover(this, currentRoad, targetJunction);


		} else {

			// not newborn

			// step length in seconds
			double stepLength = ContextManager.getStepLength(); // in seconds
			double speed = this.getSpeed(); //in m/s

			double distance = speed * stepLength;

			// TODO : remove following line
			if (ContextManager.getDEBUG_MODE()) {
				System.out.println(this.getPosition());
			}
			OvershootData overshoot = plm.move(distance);

			// TODO : remove following line
			if (ContextManager.getDEBUG_MODE()) {
				System.out.println(this.getPosition());
			}
			if (this.getOrthodromicDistance(this.getPosition(), destinationCoordinate) < ContextManager.getDistanceSnap()) {
				plm.terminate(destinationCoordinate);
			}

			if (overshoot != null) {
				this.plm = new PolyLineMover(this, overshoot.getRoad(), overshoot.getTargetJunction());
			}

		}


		if (!isKilled) {
			Measurement m = new Measurement(this.getID(),
					RunEnvironment.getInstance().getCurrentSchedule().getTickCount());

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

		//		Integer currentTick = new Integer((int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount());

		//		ContextManager.getPostgresLogger().log(currentTick, this.age);

		if (this.isAtDestination()) {
			ContextManager.removeAgent(this);
			ContextManager.setLaunchNextCalibrationAgent(true);
		}

//		if (this.isCalibrationAgent() && this.isMoreThan50PercentOverGPSRouteDistance()) {
//			this.setTerminated(true);
//		}


	} // step()

	//	private double getOrthodromicMulitLineString(MultiLineString geometry) {
	//		double sum = 0.0;
	//		int numGeometries = geometry.getNumGeometries();
	//		for (int i=0; i<numGeometries; i++) {
	//			LineString l = (LineString) geometry.getGeometryN(i);
	//			sum = sum + getOrthodromicDistance(l.getStartPoint().getCoordinate(), l.getEndPoint().getCoordinate());
	//		}
	//		return sum;
	//	}

	public double getSpeed() {
		if (ContextManager.getAgentSpeedMode() == AGENT_SPEED_MODES.STATIC) {
			return ContextManager.getAgentSpeed();
		} else {
			return 1.0;
		}
	}

	public void writeHistory(int modelRun) {

		if (ContextManager.inCalibrationMode()) {

			int matchedEdges = this.matchedGPSRoute.getNumberOfEdges();

			if (matchedEdges == 0) {

				ContextManager.getCalibrationModeData().incrementNumberOfCanceledAgents();

			} else {

				OverlapCalculator oc = new OverlapCalculator();
				oc.addMatchedGPSRoute(this.matchedGPSRoute);
				oc.addSimulatedRoute(this.getRoute());
				double overLap = oc.getOverlap();

				//			PathSizeCalculator psc = new PathSizeCalculator();
				//			psc.addMatchedGPSRoute(this.matchedGPSRoute);
				//			psc.addSimulatedRoute(this.getRoute());
				//			// System.out.println("****" + psc.calculatePathSize());
				//			double pathSize = psc.calculatePathSize();

				String[] xx = new Double(overLap).toString().split("\\.");
				String newPathSizeString=xx[0] + "," + xx[1];

				// ContextManager.getCalibrationLogger().logLine(this.getID() + ";" + this.getGpsRouteID() + ";" + newPathSizeString);

				// do the logging if there are more than 5 edges, otherwise increase the canceled agent value
				//				if (this.matchedGPSRoute.getNumberOfEdges()>5) {
				ContextManager.getCalibrationLogger().logLine(newPathSizeString +  ";" + ContextManager.getAngleToDestination() + ";" + this.matchedGPSRoute.getOBJECTID() + ";" + this.matchedGPSRoute.getNumberOfEdges());
				ContextManager.getCalibrationLogger().pathSizes.add(overLap);
				//				} else {
				//					ContextManager.canceledAgents = ContextManager.canceledAgents + 1;
				//				}

			}



		}

		if (ContextManager.dumpAgentHistoryIntoDotFile()) {

			AgentDotLogger cnl = new AgentDotLogger(ContextManager.getProperty("AgentHistoryDirectory"), this.getID(), modelRun);
			try {
				cnl.writeHistory(history);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (ContextManager.dumpIntoKMLFile()) {

			AgentHistoryKMZWriter ahw = new AgentHistoryKMZWriter(ContextManager.getProperty("AgentHistoryDirectory"), this.getID(), modelRun);

			try {
				ahw.writeHistory(history);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if (isExplicative()) {
			//			ContextManager.getRouteStore().addSimulatedRoute(this.getRoute());
			//			System.out.println("Routes added to route store for agentID=" + this.getID());


			// add the route to the route context
			//			ContextManager.getRouteContext().add(this.getRoute());
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
	 *            An optional List that will be populated with the closest points to x (i.e. the results of
	 *            <code>distanceOp.closestPoints()</code>.
	 * @param searchDist
	 *            The maximum distance to search for objects in. Small distances are more efficient but larger ones are
	 *            less likely to find no objects.
	 * @return The nearest object.
	 * @throws RoutingException
	 *             If an object cannot be found.
	 */
	public static synchronized <T> T findNearestObject(Coordinate x, Geography<T> geography,
			List<Coordinate> closestPoints, GlobalVars.GEOGRAPHY_PARAMS.BUFFER_DISTANCE searchDist)
					throws RoutingException {
		if (x == null) {
			throw new RoutingException("The input coordinate is null, cannot find the nearest object");
		}

		T nearestObject = SpatialIndexManager.findNearestObject(geography, x, closestPoints, searchDist);

		if (nearestObject == null) {
			throw new RoutingException("Couldn't find an object close to these coordinates:\n\t" + x.toString());
		} else {
			return nearestObject;
		}
	}

	/**
	 * There will be no inter-agent communication so these agents can be executed simulataneously in separate threads.
	 */
	//	@Override
	public final boolean isThreadable() {
		return true;
	}

	//	@Override
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
	}

	//	//	@Override
	//	public List<String> getTransportAvailable() {
	//		return null;
	//	}

	@Override
	public String toString() {

		Coordinate position = this.getPosition();

		String roadID="";

		try {
			roadID = " R=" + this.getCurrentRoad().getIdentifier();
		} catch (NoIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return "Agent " + this.id +  " (" + position.x + "/" + position.y + ")" + roadID;
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


//			String idtfr = currentRoad.getIdentifier();

			//			System.out.println("(" + ContextManager.getCurrentTick() + ") currentroad = " + idtfr);

			if ((currentRoad.getIdentifier().equals("-1")) || (currentRoad.getIdentifier().equals("-2"))) {
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
	 * 
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
			if (ContextManager.isCrowdingLoggerOn()) {

				RoadNetwork rn = ContextManager.getCrowdingNetwork();

				if (this.currentRoad != null) {

					// get the crowding network

					if (rn.hasRoad(this.currentRoad)) {
						rn.removeAgentFromRoad(this, this.currentRoad);
					}

					this.currentRoad = currentRoad;

					rn.addAgentToRoad(this, this.currentRoad);
				} else {
					// first entry onto an edge
					rn.addAgentToRoad(this, currentRoad);
					this.currentRoad = currentRoad;

				}
				}
			 */
		}

	}

	public Junction getTargetJunction() {
		return targetJunction;
	}

	public void setTargetJunction(Junction targetJunction) {
		this.targetJunction = targetJunction;
	}

	//	public boolean isTerminated() {
	//		return terminated;
	//	}

//	public void setTerminated(boolean terminated) {
//		this.terminated = terminated;
//	}

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
			this.getBasicAgentLogger().doLog(this.getID(), this.getBirthTick(), ContextManager.getCurrentTick(), bZID, dZID, this.getSourceCoord(), this.getDestinationCoordinate());
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

	@Override
	public void finishCalibrationRoute(boolean b) {
		if (b) {
			this.calibrationRoute.setSuccessful(true);
		} 
			
		ContextManager.getCalibrationModeData().addCalibrationRoute(this.getCalibrationRoute());

	}

	private CalibrationRoute getCalibrationRoute() {
		return this.calibrationRoute;
	}

	//	// returns a list of edge IDs the agent has run on 
	//	public ArrayList<String> getEdgeIDHistory() {
	//		
	//		ArrayList<String> eh = new ArrayList<String>();
	//		for (Measurement m : getHistory()) {
	//			eh.add(m.getRoadID());
	//		}
	//		
	//		return eh;
	//		
	//	}

}
