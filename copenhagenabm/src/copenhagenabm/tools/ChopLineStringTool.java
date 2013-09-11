package copenhagenabm.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.main.GlobalVars;

public class ChopLineStringTool {

	private Geometry lineString;
	private ArrayList<LineString> polylineParts  = new ArrayList<LineString>();
	GeometryFactory fact = new GeometryFactory();

	public ChopLineStringTool(Geometry lineString) {
		this.lineString = lineString;

	}

	public LineString chop(Coordinate chopCoord, Coordinate endCoord) {

		// 1. build an array of line strings, each representing a section from vertex to vertex

		Coordinate[] roadCoords = lineString.getCoordinates();

		List<Coordinate> rc =  Arrays.asList(roadCoords);

		double SNAP_DISTANCE = GlobalVars.GEOGRAPHY_PARAMS.SNAP_DISTANCE;
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

		// now get the segment where endCoord is on
		int segmentIDOfChop = 0;
		for (LineString l : polylineParts) {
			double dist = l.distance(fact.createPoint(chopCoord));
			if (dist<SNAP_DISTANCE) {
				break;
			}
			segmentIDOfChop++;
		}

		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		
		coords.add(chopCoord);
		
		LineString nullPart = polylineParts.get(0);
		
		if (polylineParts.size()==1) {
			
			if (endCoord == nullPart.getCoordinateN(0)) {
				
				coords.add(nullPart.getCoordinateN(0));
			
			} else {
			
				coords.add(nullPart.getCoordinateN(1));
				
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
				// c00 is the end coordinate - we move from the chop point from c01 towards c00

				for (int i=segmentIDOfChop; i>0; i--) {
					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(0));
				}
				coords.add(endCoord);

			}

			if (c01.equals(endCoord)) {
				for (int i=0; i<segmentIDOfChop; i++) {
					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(0));

				}
			}

			if (c10.equals(endCoord)) {
				for (int i= polylineParts.size(); i > segmentIDOfChop; i--) {
					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(0));
				}
			}

			if (c11.equals(endCoord)) {
				for (int i=segmentIDOfChop; i< polylineParts.size(); i++) {
					LineString pp = polylineParts.get(i);
					coords.add(polylineParts.get(i).getCoordinateN(1));
				}
			}	

		}
		
		Coordinate[] cc = coords.toArray(new Coordinate[coords.size()]);

		return fact.createLineString(cc);
		
	}

}
