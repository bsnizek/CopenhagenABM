package copenhagenabm.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;

import repastcity3.environment.Junction;

import copenhagenabm.routes.Route;

import copenhagenabm.tools.SnapTool;
import copenhagenabm.tools.geodetic.GeodeticCalculator;

/**
 * A helper class to move agents along polylines
 * 
 * © 2012 
 * 
 * @author Bernhard Snizek, bs@metascapes.org
 * 
 * http://www.bikeability.dk
 * 
 */
public class PolyLineMover {

	private static final boolean DEBUG_MODE = false;
	GeometryFactory fact = new GeometryFactory();
	private CPHAgent agent;
	double[] distAndAngle = new double[2];

	int currentLineSegmentID;
	private LineString currentLineSegment;
	double currentPositionOnPolyline = 0.0d;		// distance from the source junction to the current position

	private ArrayList<LineString> polylineParts  = new ArrayList<LineString>();
	SnapTool sTool = ContextManager.getSnapTool();
	private Road road;
	private Junction targetJunction;
	private double lineLength = 0.0d;

	public PolyLineMover(CPHAgent agent, Road road, Junction targetJunction) {

		this.agent = agent;
		this.road = road;
		this.targetJunction = targetJunction;

		Geometry roadGeom = road.getGeometry();

		if (DEBUG_MODE) {
			lineLength = roadGeom.getLength();
		} else {
			lineLength = getOrthodromicLineLength(roadGeom);
		}


		Coordinate[] roadCoords = roadGeom.getCoordinates();

		List<Coordinate> rc =  Arrays.asList(roadCoords);

		if (DEBUG_MODE) {

			if (rc.get(0).distance(agent.getPosition()) > rc.get(rc.size()-1).distance(agent.getPosition())) {
				Collections.reverse(rc);
			} 
		} else {
			if (getOrthodromicDistance(rc.get(0), agent.getPosition()) > getOrthodromicDistance(rc.get(rc.size()-1), agent.getPosition())) {
				Collections.reverse(rc);
			}

		}

		for (int j=0; j<rc.size()-1;j++) {
			Coordinate[] cs = {rc.get(j),rc.get(j+1)};
			LineString l = fact.createLineString(cs);
			polylineParts.add(l);
		}

		// lets place the agent on the polyline and figure out the currentLineSegmentID

		getPositionCurrentLineSegmentAndID();

	}

	/**
	 * 
	 * Figures out where one the polyline the agents current position is on the polyline and sets 
	 * currentLineSegmentID, currentLineSegment and currentPositionOnPolyline
	 * 
	 */
	public void getPositionCurrentLineSegmentAndID() {

		double d = Double.MAX_VALUE;
		double cp = 0.0d;

		Coordinate currPos = agent.getPosition();

		// lets find the segment which is closest to our currPos

		int i = 0;
		for (LineString pLPart : polylineParts) {
			double dd = this.sTool.getDistanceToLineString(pLPart, currPos);
			if (dd<d) {
				d = dd;
				currentLineSegmentID = i;
			}
			
			i++;
		}
		
		// now add each segment until we reach the current segment. 
		for (int j=0; j<currentLineSegmentID;j++) {
			if (DEBUG_MODE) {
				cp = cp + polylineParts.get(j).getLength();
			} else {
				cp = cp + getOrthodromicLineLength(polylineParts.get(j));
			}
		}
		

//		// lets take the last one off again
//		if (DEBUG_MODE) {
//			cp = cp - polylineParts.get(currentLineSegmentID).getLength();
//		} else {
//			cp = cp - getOrthodromicLineLength(polylineParts.get(currentLineSegmentID));
//		}

		currentLineSegment = polylineParts.get(currentLineSegmentID);

		if (DEBUG_MODE) {
			cp = cp + currPos.distance(polylineParts.get(currentLineSegmentID).getCoordinateN(0));
		} else {
			cp = cp + getOrthodromicDistance(currPos, polylineParts.get(currentLineSegmentID).getCoordinateN(0));
		}
		this.currentPositionOnPolyline = cp;
	}


	public CPHAgent getAgent() {
		return agent;
	}

	public void setAgent(CPHAgent agent) {
		this.agent = agent;
	}

	public HashMap<Integer, Double> buildRestDistances() {
		double acc=0;
		HashMap<Integer, Double> h = new HashMap<Integer, Double>();
		for (int i=polylineParts.size()-2;i>0; i--) {
			acc = acc + polylineParts.get(i).getLength();
			h.put(i, acc);
		}
		return h;
	}

	public HashMap<Integer, Double> buildDistancesFromNextSegment() {
		double acc=0;
		HashMap<Integer, Double> h = new HashMap<Integer, Double>();
		for (int i=1; i<polylineParts.size(); i++) {
			acc = acc + polylineParts.get(i).getLength();
			h.put(i,acc);
		}

		return h;
	}

	/**
	 *  getDistanceToEndOfPolyline()
	 *  
	 *  calculates the distance along a polyline from the current position of the 
	 *  agent to the end of the polyline
	 *  
	 * @return
	 */
	public double getDistanceToEndOfPolyline() {
		return this.lineLength - this.currentPositionOnPolyline;
	}

	public double getOrthodromicDistanceToEndOfPolyline() {
		// first accumulate the lengths on the poly from the next segment on 
		double sum = 0.0d;
		for (int i=currentLineSegmentID+1; i< polylineParts.size(); i++) {

			double od;

			if (DEBUG_MODE) {

				od = polylineParts.get(i).getStartPoint().getCoordinate().distance(polylineParts.get(i).getEndPoint().getCoordinate());

			} else {

				od = getOrthodromicDistance(polylineParts.get(i).getStartPoint().getCoordinate(), polylineParts.get(i).getEndPoint().getCoordinate());
			}
			sum = sum + od;
		}

		// and then add the rest of the current poly to the sum

		if (DEBUG_MODE) {
			sum = sum + agent.getPosition().distance(currentLineSegment.getEndPoint().getCoordinate());
		} else {
			sum = sum + getOrthodromicDistance(agent.getPosition(), currentLineSegment.getEndPoint().getCoordinate());
		}

		return sum;
	}


	public Junction getOppositeJunction(Road road, Junction sourceJunction) {
		ArrayList<Junction> jS = road.getJunctions();
		if (jS.get(0) == sourceJunction) {
			return jS.get(1);
		} else {
			return jS.get(0);
		}

	}

	public OvershootData move(double distance)  {

		// 1. set remaining step distance to step length -> RemainingDistanceOfStep
		// 2. get the remaining distance towards the target node (remainingDistanceOnEdge)
		// 3. subtract RemainingDistanceOfStep-remainingDistanceOnPolyline -> overshoot
		// 4. make a road selection
		// 5. if (the length of the next road < overshoot) go back to 4

		getPositionCurrentLineSegmentAndID();

		double remainingDistanceOfStep = distance;

		double remainingDistanceOnPolyline = getDistanceToEndOfPolyline();

		double overshoot = remainingDistanceOfStep - remainingDistanceOnPolyline;

		if (overshoot > 0.0d) {

			List<Road> roads = this.targetJunction.getRoads();

			EdgeSelector es = new EdgeSelector(roads, road, this.getAgent());
			Road newRoad = es.getRoad();

			// let us add the new road to 

			Route theRoute = this.agent.getRoute();

			if (theRoute != null) {

				theRoute.addEdgeGeometry(newRoad, newRoad.getGeometry());

			}

			// place the agent at the end  of the road

			ContextManager.moveAgent(agent, fact.createPoint(this.targetJunction.getCoords()));

			if (agent.isAtDestination()) {
				ContextManager.moveAgent(agent, fact.createPoint(agent.getDestinationCoordinate()));
				this.logToPostgres();
				
				ContextManager.removeAgent(agent);
				
				return null;
			}

			this.targetJunction = getOppositeJunction(newRoad, this.targetJunction);

			PolyLineMover plm = new PolyLineMover(agent, newRoad, this.targetJunction);

			OvershootData oD = plm.move(overshoot);

			if (oD != null) {

				return oD;

			} else {

				return new OvershootData(newRoad, targetJunction, overshoot);
			}

			//			this.logToPostgres();

		} else { 		

			double nextPosition = currentPositionOnPolyline + distance;

			if (overshoot == 0.0d) {

				List<Road> roads = this.targetJunction.getRoads();
				EdgeSelector es = new EdgeSelector(roads, road, this.getAgent());
				Road newRoad = es.getRoad();

				this.currentPositionOnPolyline = 0.0d;
				this.road = newRoad;

				// add the road to the route

				Route theRoute = this.agent.getRoute();

				if (theRoute != null) {

					theRoute.addEdgeGeometry(newRoad, newRoad.getGeometry());
				}

				Coordinate tjC = road.getTargetJunction().getCoords();
				Coordinate aP = agent.getPosition();

				if (tjC.equals(aP)) {
					this.targetJunction = this.getOppositeJunction(road, road.getTargetJunction());
				} else {
					this.targetJunction = road.getTargetJunction();
				}

				nextPosition = 0.0d;

				placeAgentOnRoad(road, nextPosition);
				logToPostgres();

//				return new OvershootData(road, targetJunction, nextPosition);
				
				return null;

			} 

			placeAgentOnRoad(road, nextPosition);

			this.logToPostgres();


			if (agent.isAtDestination()) {

				ContextManager.moveAgent(agent, fact.createPoint(agent.getDestinationCoordinate()));

//				ContextManager.removeAgent(agent);

				return null;
			}



			//			return new OvershootData(distance - remainingDistanceOnPolyline, currentLineSegment.getEndPoint());


		}
		return null;

	}


	/**
	 * 
	 * Places an agent on the given <road> towards the <targetJunction> in a distance of <dist> from the origin junction
	 * 
	 * @param road
	 * @param sourceJunction 
	 * @param targetJunction
	 */
	public void placeAgentOnRoad(Road road, double distance) {

		// we assume the direction of the polyline is right

		// lets loop through the polyline parts

		if (distance==0.0d) {

			ContextManager.moveAgent(agent, fact.createPoint(road.getSourceJunction().getCoords()));

		} else {

			//			double dsum = 0.0d;
			double d2 = distance;

			int i = 0;
			boolean RUN = true;
			while (RUN) {

				double currentSegmentLength = getOrthodromicLineLength(polylineParts.get(i));

				d2 = d2 - currentSegmentLength;
				if (d2<=0) {
					Coordinate setoffCoordinate = polylineParts.get(i).getCoordinateN(0);
					Coordinate targetCoordinate = polylineParts.get(i).getCoordinateN(1);

					if (DEBUG_MODE) {

						double deltaX = targetCoordinate.x - setoffCoordinate.x;
						double deltaY = targetCoordinate.y - setoffCoordinate.y;

						double l = getOrthodromicDistance(setoffCoordinate, targetCoordinate);
						double coeff = (l + d2) / l;
						Coordinate cNew = new Coordinate(setoffCoordinate.x + coeff*deltaX, setoffCoordinate.y + coeff*deltaY);
						ContextManager.moveAgent(agent, fact.createPoint(cNew));
						RUN = false;

					} else {


						distance(setoffCoordinate,
								targetCoordinate, distAndAngle);
						Point setoffPoint = fact.createPoint(setoffCoordinate);
						ContextManager.moveAgent(agent, setoffPoint);
						double angle = distAndAngle[1];

						ContextManager.moveAgentByVector(agent, (currentSegmentLength + d2), angle);

					}

					RUN=false;

				}
				i++;
			}
		}
	}


	/**
	 * Calculate the distance (in meters) between two Coordinates, using the coordinate reference system that the
	 * roadGeography is using. For efficiency it can return the angle as well (in the range -0 to 2PI) if returnVals
	 * passed in as a double[2] (the distance is stored in index 0 and angle stored in index 1).
	 * 
	 * @param c1
	 * @param c2
	 * @param returnVals
	 *            Used to return both the distance and the angle between the two Coordinates. If null then the distance
	 *            is just returned, otherwise this array is populated with the distance at index 0 and the angle at
	 *            index 1.
	 * @return The distance between Coordinates c1 and c2.
	 */
	public static synchronized double distance(Coordinate c1, Coordinate c2, double[] returnVals) {
		GeodeticCalculator calculator = new GeodeticCalculator(ContextManager.roadProjection.getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		double distance = calculator.getOrthodromicDistance();
		if (returnVals != null && returnVals.length == 2) {
			returnVals[0] = distance;
			double angle = Math.toRadians(calculator.getAzimuth()); // Angle in range -PI to PI
			// Need to transform azimuth (in range -180 -> 180 and where 0 points north)
			// to standard mathematical (range 0 -> 360 and 90 points north)
			if (angle > 0 && angle < 0.5 * Math.PI) { // NE Quadrant
				angle = 0.5 * Math.PI - angle;
			} else if (angle >= 0.5 * Math.PI) { // SE Quadrant
				angle = (-angle) + 2.5 * Math.PI;
			} else if (angle < 0 && angle > -0.5 * Math.PI) { // NW Quadrant
				angle = (-1 * angle) + 0.5 * Math.PI;
			} else { // SW Quadrant
				angle = -angle + 0.5 * Math.PI;
			}
			returnVals[1] = angle;
		}

		return c1.distance(c2);

	}

	/**
	 * 
	 * returns the orthodromic distance between 
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public double getOrthodromicDistance(Coordinate c1, Coordinate c2) {

		if (DEBUG_MODE)
			return c1.distance(c2);
		else
			return ContextManager.simpleDistance.distance(c1, c2);

	}

	/**
	 * returns the orthodromic 
	 * @param line
	 * @return
	 */
	public double getOrthodromicLineLength(Geometry line) {
		Coordinate[] coords = line.getCoordinates();
		ArrayList<Coordinate> cA = new ArrayList<Coordinate>(Arrays.asList(coords));
		double l = 0.0d;

		for (int i=0; i<cA.size()-1; i++) {
			if (DEBUG_MODE) {
				l = l + cA.get(i).distance(cA.get(i+1));
			} else {
				l = l + ContextManager.simpleDistance.distance(cA.get(i), cA.get(i+1));
			}
		}

		return l;
	}

	/**
	 * returns the orthodromic 
	 * @param line
	 * @return
	 */
	public double getOrthodromicLineLength(LineString line) {
		Coordinate[] coords = line.getCoordinates();
		ArrayList<Coordinate> cA = new ArrayList<Coordinate>(Arrays.asList(coords));
		double l = 0.0d;

		for (int i=0; i<cA.size()-1; i++) {
			if (DEBUG_MODE) {
				l = l + cA.get(i).distance(cA.get(i+1));
			} else {
				l = l + ContextManager.simpleDistance.distance(cA.get(i), cA.get(i+1));
			}
		}

		return l;
	}

	/**
	 * returns the orthodromic 
	 * @param line
	 * @return
	 */
	public double getOrthodromicLineLength(MultiLineString line) {
		Coordinate[] coords = line.getCoordinates();
		ArrayList<Coordinate> cA = new ArrayList<Coordinate>(Arrays.asList(coords));
		double l = 0.0d;

		for (int i=0; i<cA.size()-1; i++) {
			if (DEBUG_MODE) {
				l = l + cA.get(i).distance(cA.get(i+1));
			} else {
				l = l + ContextManager.simpleDistance.distance(cA.get(i), cA.get(i+1));
			}
		}

		return l;
	}

	public void terminate(Coordinate destinationCoordinate) {
		ContextManager.moveAgent(agent, fact.createPoint(destinationCoordinate));
		logToPostgres();
		this.agent.setToBeKilled(true);

	}

	
	public void logToPostgres() {
		if (DEBUG_MODE) {
			System.out.println(">> logging to postreSQL" + agent.getPosition());
		} else {
			if (ContextManager.isPostgreSQLLoggerOn()) {
				ContextManager.getPostgresLogger().log(ContextManager.getCurrentTick(), agent);
			}
		}
	}


}
