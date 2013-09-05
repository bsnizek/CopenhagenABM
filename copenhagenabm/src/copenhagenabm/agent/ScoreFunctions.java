package copenhagenabm.agent;

import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.environment.Road;

public class ScoreFunctions {

	static GeometryFactory gf = new GeometryFactory();

	/*
	 * scoreAngleToDestination()
	 * 
	 * scores the angle to the destination
	 * The smaller the angle between edge and direct line from current position to the destination point 
	 * of the tour the higher the value.
	 * 
	 * The value can be between 0 .. 1, 0 being 180 degrees (backwards) and 1 0 degrees or straight ahead
	 * 
	 */
	public static double scoreAngleToDestination(
			Road road, 
			Coordinate currentPosition, 
			Coordinate destinationPoint,
			double subjectiveValue) {

		Point currpos = gf.createPoint(currentPosition);

		// now get the point on the edge that is farthest away from the current position

		Point edge_to_point = road.getEdge().getTarget().getPoint();
		Point edge_from_point = road.getEdge().getSource().getPoint();

		double d1 = edge_to_point.distance(currpos);
		double d2 = edge_from_point.distance(currpos);

		Point farpoint;

		if (d1>d2) {
			farpoint = edge_to_point;
		} else {
			farpoint = edge_from_point;
		}

		double ux = destinationPoint.x - currentPosition.x;
		double uy = destinationPoint.y - currentPosition.y;

		double vx = farpoint.getX() - currentPosition.x;
		double vy = farpoint.getY() - currentPosition.y;

		double u = ux * vx + uy * vy;
		double l = Math.sqrt(ux*ux + uy*uy) * Math.sqrt(vx*vx + vy*vy);
		double u_l = u/l;
		double u_d = u_l-1;
		double xxx = (Math.toDegrees(Math.acos(u_l)));

		double xx;

		if (xxx>0) {
			xx = 1 - xxx/ 180;
		} else {
			xx =1;
		}
		
		return xx*subjectiveValue;
	}

	public static double avoidUturn(Road r, Road currentRoad, double avoidUTurn) {
		try {
			if (r.getIdentifier() == currentRoad.getIdentifier()) {
				return 0.0;
			} else {
				return 1.0;
			}
		} catch (NoIdentifierException e) {
			e.printStackTrace();
		}

		return 1d;
	}

	public static double scoreTurnRight(Road rr, Coordinate currentPosition,
			Road currentRoad, double subjectiveValue) {
		
		Point currpos = gf.createPoint(currentPosition);
		
		Point edge_to_point = currentRoad.getEdge().getTarget().getPoint();
		Point edge_from_point = currentRoad.getEdge().getSource().getPoint();
		
		double d1 = edge_to_point.distance(currpos);
		double d2 = edge_from_point.distance(currpos);

		Point current_farpoint;

		if (d1>d2) {
			current_farpoint = edge_to_point;
		} else {
			current_farpoint = edge_from_point;
		}
		
		Point rr_edge_to_point = rr.getEdge().getTarget().getPoint();
		Point rr_edge_from_point = rr.getEdge().getSource().getPoint();
		
		double d3 = rr_edge_to_point.distance(currpos);
		double d4 = rr_edge_from_point.distance(currpos);
		
		Point edge_farpoint;
		
		if (d3>d4) {
			edge_farpoint = rr_edge_to_point;
		} else {
			edge_farpoint = rr_edge_from_point;
		}
		
		double ux = edge_farpoint.getX() - currentPosition.x;
		double uy = edge_farpoint.getY() - currentPosition.y;
		
		double vx = current_farpoint.getX() - currentPosition.x;
		double vy = current_farpoint.getY() - currentPosition.y;
		
		double dot = ux*vx + uy*vy;
				
		double u = ux * vx + uy * vy;
		double l = Math.sqrt(ux*ux + uy*uy) * Math.sqrt(vx*vx + vy*vy);
		double u_l = u/l;
		double xxx = (Math.toDegrees(Math.acos(u_l)));

		double xx = 180-xxx;
		
		if (dot<0 && xx>45) {
			return subjectiveValue;
		} else {
			return 0.0;
		}
		
	}

	public static double scoreTurnLeft(Road rr, Coordinate currentPosition,
			Road currentRoad, double subjectiveValue) {
		
		Point currpos = gf.createPoint(currentPosition);
		
		Point edge_to_point = currentRoad.getEdge().getTarget().getPoint();
		Point edge_from_point = currentRoad.getEdge().getSource().getPoint();
		
		double d1 = edge_to_point.distance(currpos);
		double d2 = edge_from_point.distance(currpos);

		Point current_farpoint;

		if (d1>d2) {
			current_farpoint = edge_to_point;
		} else {
			current_farpoint = edge_from_point;
		}
		
		Point rr_edge_to_point = rr.getEdge().getTarget().getPoint();
		Point rr_edge_from_point = rr.getEdge().getSource().getPoint();
		
		double d3 = rr_edge_to_point.distance(currpos);
		double d4 = rr_edge_from_point.distance(currpos);
		
		Point edge_farpoint;
		
		if (d3>d4) {
			edge_farpoint = rr_edge_to_point;
		} else {
			edge_farpoint = rr_edge_from_point;
		}
		
		double ux = edge_farpoint.getX() - currentPosition.x;
		double uy = edge_farpoint.getY() - currentPosition.y;
		
		double vx = current_farpoint.getX() - currentPosition.x;
		double vy = current_farpoint.getY() - currentPosition.y;
		
		double dot = ux*vx + uy*vy;
				
		double u = ux * vx + uy * vy;
		double l = Math.sqrt(ux*ux + uy*uy) * Math.sqrt(vx*vx + vy*vy);
		double u_l = u/l;
		double xxx = (Math.toDegrees(Math.acos(u_l)));

		double xx = 180-xxx;
		
		if (dot>0 && xx>45) {
			return subjectiveValue;
		} else {
			return 0.0;
		}
		
	}

}
