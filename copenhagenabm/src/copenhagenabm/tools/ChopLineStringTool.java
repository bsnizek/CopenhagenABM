package copenhagenabm.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class ChopLineStringTool {

	private Geometry lineString;
	private ArrayList<LineString> polylineParts  = new ArrayList<LineString>();

	GeometryFactory fact = new GeometryFactory();
	SnapTool sTool = new SnapTool();

	public ChopLineStringTool(Geometry g) {
		this.lineString = g;

	}

	public LineString chop(Coordinate chopCoord, Coordinate endCoord) {

		if (chopCoord.equals(endCoord)) {
			
//			System.out.println("endCoord = chopCoord");
			
			return null;
		}

		// 1. build an array of line strings, each representing a section from vertex to vertex

		Coordinate[] roadCoords = lineString.getCoordinates();

		List<Coordinate> rc =  Arrays.asList(roadCoords);

		//		double SNAP_DISTANCE = GlobalVars.GEOGRAPHY_PARAMS.SNAP_DISTANCE;
		//
		//		// check the direction
		//		if (rc.get(0).distance(endCoord) > SNAP_DISTANCE) {
		//		} else {
		//			// the polyline is backwards; reverse !
		//			Collections.reverse(rc);
		//		}


		for (int j=0; j<rc.size()-1;j++) {
			Coordinate[] cs = {rc.get(j),rc.get(j+1)};
			LineString l = fact.createLineString(cs);
			polylineParts.add(l);
		}

		Point theChopPoint = fact.createPoint(chopCoord);

		// now get the segment where endCoord is on
		int segmentIDOfChop = 0;
		for (LineString l : polylineParts) {


			//			int comp = l.compareTo(theChopPoint);
			//			boolean touches = l.touches(theChopPoint);
			//			boolean covers = theChopPoint.covers(l);
			//			boolean contains = l.contains(theChopPoint);
			boolean covers = l.covers(theChopPoint);
			//			Geometry intersection = l.intersection(theChopPoint);
			//			
			//			Coordinate c = sTool.getProjectedPointOnPolyline(l, chopCoord);

			if (covers) {
				break;
			}

			segmentIDOfChop++;
		}

		if (segmentIDOfChop > polylineParts.size()-1) {
			// theChopPoint is not on one of the polyline segments -> we have to improvise

			// 0. Tie my to the gunnery chair.

			LineString nearestLineString = null;

			// 1. we find the polyline segment (LineString) that is closest

			double max = Double.MAX_VALUE;

			int sIDOc = 0;

			for (LineString l : polylineParts) {
				double d = l.distance(theChopPoint);
				if (d < max) {
					max = d;
					nearestLineString = l;
					segmentIDOfChop = sIDOc;
				}
				sIDOc++;
			}

			// 2. now, as we have got the polyline segment we figure out the projection of our coordinate onto the line segment
			Coordinate theProjectedChopCoord = sTool.getProjectedPointOnPolyline(nearestLineString, chopCoord);

			chopCoord = theProjectedChopCoord;

		}


		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();

		coords.add(chopCoord);

		LineString nullPart = polylineParts.get(0);

		if (polylineParts.size()==1) {

			if (endCoord == nullPart.getCoordinateN(0)) {

				coords.add(nullPart.getCoordinateN(1));

			} else {

				coords.add(nullPart.getCoordinateN(0));

			}

		} else {

			// get the first and the second coordinate of the first polylinePart
			Coordinate c00 = nullPart.getCoordinateN(0);
			Coordinate c01 = nullPart.getCoordinateN(1);

			// get the first an the second coordinate of the last polylinePart
			Coordinate c10 = polylineParts.get(polylineParts.size()-1).getCoordinateN(0);
			Coordinate c11 = polylineParts.get(polylineParts.size()-1).getCoordinateN(1);


			// now check how the goal coordinate is related to the first and the last coodinates of the first and the last part of the polyline

			if (c00.equals(endCoord)) {

//				System.out.println("c00");
				// c00 is the end coordinate - we move from the chop point from c01 towards c00

				for (int i=segmentIDOfChop-1; i>0; i--) {
					//					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(0));
				}
				coords.add(endCoord);

			}

			if (c01.equals(endCoord)) {

//				System.out.println("c01");

				for (int i=0; i<segmentIDOfChop; i++) {
					//					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(0));

				}
			}

			if (c10.equals(endCoord)) {

//				System.out.println("c10");

				for (int i= polylineParts.size(); i > segmentIDOfChop; i--) {
					//					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(0));
				}
			}

			if (c11.equals(endCoord)) {

//				System.out.println("c11");

				for (int i=segmentIDOfChop; i< polylineParts.size(); i++) {
					//					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(1));
				}
			}	

		}






		if (coords.size()<2) {
			// only one coordinate, dammit 
			System.out.println(" DAMMIT ! Only one coordinate " );
			this.chop(chopCoord, endCoord);
		}

		if (coords.size()>2 && coords.get(0).equals(coords.get(1))) {
			// first and second coordinates are the same, happens when you cut at the first node ... 
			coords.remove(0);
		}

		Coordinate[] cc = coords.toArray(new Coordinate[coords.size()]);

		return fact.createLineString(cc);

	}

}

