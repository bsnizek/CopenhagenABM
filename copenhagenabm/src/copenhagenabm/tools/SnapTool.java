package copenhagenabm.tools;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import repastcity3.environment.SpatialIndexManager;

import com.infomatiq.jsi.SpatialIndex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars.GEOGRAPHY_PARAMS.BUFFER_DISTANCE;

public class SnapTool {

	public class ReturnArray implements TIntProcedure {

		private ArrayList<Integer> result = new ArrayList<Integer>();

		public boolean execute(int value) {
			this.result.add(value);
			return true;
		}

		ArrayList<Integer> getResult() {
			return this.result;
		}
	}

	private String minNodeEntries;
	private String maxNodeEntries;


	/**
	 * Constructor - gets the min and max settings for the nodes found in the near search from the property file 
	 */
	public SnapTool() {
		minNodeEntries = ContextManager.getProperty("MinNodeEntries");
		maxNodeEntries = ContextManager.getProperty("MaxNodeEntries");
	}

	/*
	 * This is used by the explicative model and is not really cached but as the explicative routes are rather few we can
	 * get away with it, otherwise TODO: change the cache to coordinateCache
	 */
	public Road getRoadByCoordinate(Coordinate c) {
		
//		SpatialIndexManager.findNearestObject(ContextManager.roadProjection, c, null, 1000000);
		return SpatialIndexManager.findNearestObject(ContextManager.roadProjection, c, null, BUFFER_DISTANCE.LARGE);

//		HashMap<Integer, Road> roadIndex = ContextManager.getRoadindex();
//
//		// TODO: throw into property file
//		final int kNearestEdgeDistance = 1000000;

		// TODO: throw into property file
//		Properties p = new Properties();
//		p.setProperty("MinNodeEntries", minNodeEntries);
//		p.setProperty("MaxNodeEntries", maxNodeEntries);

//		com.infomatiq.jsi.Point pp = new com.infomatiq.jsi.Point((float) c.x, (float) c.y);
//		ReturnArray r = new ReturnArray();
//
//		SpatialIndex si = ContextManager.getRoadSpatialIndex();
//
//		try {
//		
////			si.nearestNUnsorted(pp, r, 8, kNearestEdgeDistance);
////			si.nearestNUnsorted(pp, r, 4, Float.MAX_VALUE);
//			si.nearest(pp, r, Float.MAX_VALUE);
//		
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
////		System.out.println(r.getResult().size() + " edges found within " + kNearestEdgeDistance + "m.");
//
//		double distance = Double.MAX_VALUE;
//		//		Coordinate cc = null;
//		Road rr = null;
//
//		for (Integer i : r.getResult()) {
//			Road road = roadIndex.get(i);
//			Geometry roadGeometry = road.getGeometry();
//			Coordinate coord = getProjectedPointOnPolyline(roadGeometry, c);
//			double dist  = coord.distance(c);
//			if (dist < distance) {
//				distance = dist;
//				//				cc = coord;
//				rr = road;
//			}
//		}

//		return rr;

	}

	public Coordinate getProjectedPointOnPolyline(Geometry linestring, Coordinate c) {

		PointPairDistance ppd = new PointPairDistance(); 

		EuclideanDistanceToPoint.computeDistance(linestring, c, ppd); 

		Coordinate resultcoord = null;

		for (Coordinate cc : ppd.getCoordinates()) { 
			// System.out.println(cc); 
			if (cc.equals(c) == false) {
				resultcoord = cc;
			}
		}

		if (resultcoord == null) {
			return c;
		}
		
		return resultcoord;

	}

}
