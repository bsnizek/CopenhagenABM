package copenhagenabm.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

//import org.geotools.geometry.jts.JTS;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//import org.opengis.referencing.operation.TransformException;

//import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import repastcity3.environment.Junction;

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

	GeometryFactory fact = new GeometryFactory();
	private CPHAgent agent;
	private LineString currentLineSegment;
	double[] distAndAngle = new double[2];
	int currentLineSegmentID;;
	private ArrayList<LineString> polylineParts  = new ArrayList<LineString>();
	SnapTool sTool = null;
	private Road road;
	private Junction targetJunction;


	public PolyLineMover(CPHAgent agent, Road road, Junction targetJunction) {

		this.agent = agent;
		this.road = road;
		this.targetJunction = targetJunction;
		sTool = ContextManager.getSnapTool();


		// build an array of sub-linestrings
		// TODO : use ChopLineStringTool !!

		Geometry roadGeom = road.getGeometry();
		Coordinate[] roadCoords = roadGeom.getCoordinates();

		List<Coordinate> rc =  Arrays.asList(roadCoords);

		double SNAP_DISTANCE = GlobalVars.GEOGRAPHY_PARAMS.SNAP_DISTANCE;

		// check the direction
		if (rc.get(0).distance(targetJunction.getCoords()) > SNAP_DISTANCE) {
		} else {
			// the polyline is backwards
			Collections.reverse(rc);
		}

		for (int j=0; j<rc.size()-1;j++) {
			Coordinate[] cs = {rc.get(j),rc.get(j+1)};
			LineString l = fact.createLineString(cs);
			polylineParts.add(l);
		}

		// lets place the agent on the polyline

		double v = Double.MAX_VALUE;
		for (int i=0; i<polylineParts.size();i++) {

			Coordinate pPoint = sTool.getProjectedPointOnPolyline(polylineParts.get(i), agent.getPosition());

			// TODO: unhack
			if (pPoint == null) {
				currentLineSegmentID = i;
				currentLineSegment = polylineParts.get(i);
				break;
			} else {

				double d = getOrthodromicDistance(agent.getPosition(), pPoint);
				if ( d <v ) {
					v = d;
					currentLineSegmentID = i;
					currentLineSegment = polylineParts.get(i);
				}
			}
		}

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


	//	/**
	//	 *  getDistanceToEndOfPolyline()
	//	 *  
	//	 *  calculates the distance along a polyline from the current position of the 
	//	 *  agent to the end of the polyline
	//	 *  
	//	 * @return
	//	 */
	//	public double getDistanceToEndOfPolyline() {
	//		// first accumulate the lengths on the poly from the next segment on 
	//		double sum = 0.0d;
	//		for (int i=currentLineSegmentID+1; i< polylineParts.size(); i++) {
	//			sum = sum + polylineParts.get(i).getLength();
	//		}
	//
	//		// and then add the rest of the current poly to the sum
	//
	//		sum = sum + agent.getPosition().distance(currentLineSegment.getEndPoint().getCoordinate());
	//
	//		return sum;
	//	}

	/**
	 *  getDistanceToEndOfPolyline()
	 *  
	 *  calculates the distance along a polyline from the current position of the 
	 *  agent to the end of the polyline
	 *  
	 * @return
	 */
	public double getDistanceToEndOfPolyline() {

		double sum = 0.0d;
		for (int i=currentLineSegmentID+1; i< polylineParts.size(); i++) {
			// double od = polylineParts.get(i).getStartPoint().getCoordinate().distance(polylineParts.get(i).getEndPoint().getCoordinate());
			PolyLineMover.distance( polylineParts.get(i).getStartPoint().getCoordinate(),polylineParts.get(i).getEndPoint().getCoordinate(),distAndAngle);

			sum = sum + distAndAngle[0];
		}

		// and then add the rest of the current poly to the sum 
		Coordinate aPos = agent.getPosition();
		Coordinate lSEp = currentLineSegment.getEndPoint().getCoordinate();

		if (distAndAngle[0] < new Integer(ContextManager.getProperty(GlobalVars.distanceSnap))) {
//			System.out.println(this.agent.getID());
			return 0.0d;
		} else  {

			PolyLineMover.distance(aPos, lSEp, distAndAngle);



			sum = sum + distAndAngle[0];

			//		sum = sum + agent.getPosition().distance(currentLineSegment.getEndPoint().getCoordinate());

			return sum;

		}

	}


	public double getOrthodromicDistanceToEndOfPolyline() {
		// first accumulate the lengths on the poly from the next segment on 
		double sum = 0.0d;
		for (int i=currentLineSegmentID+1; i< polylineParts.size(); i++) {
			double od = getOrthodromicDistance(polylineParts.get(i).getStartPoint().getCoordinate(), polylineParts.get(i).getEndPoint().getCoordinate());
			sum = sum + od;
		}

		// and then add the rest of the current poly to the sum

		sum = sum + agent.getPosition().distance(currentLineSegment.getEndPoint().getCoordinate());

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

		double remainingDistanceOfStep = distance;

		double remainingDistanceOnPolyline = getDistanceToEndOfPolyline();

		double overshoot = remainingDistanceOfStep - remainingDistanceOnPolyline;

		if (overshoot >= 0.0d) {

			List<Road> roads = this.targetJunction.getRoads();
			EdgeSelector es = new EdgeSelector(roads, road, this.getAgent());
			Road newRoad = es.getRoad();

			// place the agent on the beginning of the road
			placeAgentOnRoad(road, this.targetJunction, 0);

			if (agent.isAtDestination()) {
				return null;
			}


			this.targetJunction = getOppositeJunction(newRoad, this.targetJunction);

			PolyLineMover plm = new PolyLineMover(agent, newRoad, this.targetJunction);
			plm.move(overshoot);

		} else { 		

			placeAgentOnRoad(road, getOppositeJunction(this.road, this.targetJunction), remainingDistanceOfStep);
			if (agent.isAtDestination()) {
				return null;
			}

		}
		return null;

	}

	/**
	 * 
	 * Places an agent on the given <road> towards the <targetJunction> in a distance of <dist>.
	 * 
	 * @param road
	 * @param targetJunction
	 */
	public void placeAgentOnRoad(Road road, Junction targetJunction, double distance) {

		// lets loop through the polyline parts

		double d2 = distance;

		for (int i=0; i<polylineParts.size();i++) {
			d2 = distance - getOrthodromicLineLength(polylineParts.get(i));
			if (d2<0) {
				// get the setoff base coordinate
				Coordinate setoffCoordinate = polylineParts.get(i).getCoordinateN(0);
				Coordinate targetCoordinate = polylineParts.get(i).getCoordinateN(1);

				distance(setoffCoordinate,
						targetCoordinate, distAndAngle);

				double angle = distAndAngle[1];

				Point setoffPoint = fact.createPoint(setoffCoordinate);

				ContextManager.moveAgent(agent, setoffPoint);

				ContextManager.moveAgentByVector(agent, d2 * (-1.0), angle);

			}
		}

		//		System.out.println("Agent placed at " + agent.getPosition());

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
		// TODO check this now, might be different way of getting distance in new Simphony
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

	public double getOrthodromicDistance(Coordinate c1, Coordinate c2) {

		ContextManager.orthodromicCounter++;

		//		CoordinateReferenceSystem crs = ContextManager.getCrs();
		//		double result = 0;

		//		GeodeticCalculator calculator = new GeodeticCalculator(crs);
		//		calculator.setStartingGeographicPoint(c1.x, c1.y);
		//		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		//		result = calculator.getOrthodromicDistance();

		return ContextManager.simpleDistance.distance(c1, c2);

	}

	public double getOrthodromicLineLength(LineString line) {
		return getOrthodromicDistance(line.getStartPoint().getCoordinate(), line.getEndPoint().getCoordinate());
	}

}
