package copenhagenabm.environment;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.HashMap;

import com.infomatiq.jsi.SpatialIndex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import repast.simphony.context.Context;
import repastcity3.environment.Building;
import copenhagenabm.main.ContextManager;
import copenhagenabm.tools.CopenhagenABMTools;
import copenhagenabm.tools.SnapTool;

public class SimpleNearestRoadCoordinateCache {
	
	/**
	 * Result container; looks like a bit of overkill, but is required by the SpatialIndex...
	 */
	
	class ReturnArray implements TIntProcedure {

		private ArrayList<Integer> result = new ArrayList<Integer>();
		
		public boolean execute(int value) {
	      this.result.add(value);
	      return true;
	    }
		
		ArrayList<Integer> getResult() {
			return this.result;
		}
	}

	// private GeometryFactory gfact = new GeometryFactory();  // the geometry factory
	
	// TODO: throw into property file
	private static final int kNearestEdgeDistance = 10000;

	//CopenhagenABMTools copenhagenABMTools = new CopenhagenABMTools();
	SnapTool sTool = null;
	
	HashMap<Building, Coordinate> coordinateCache = new HashMap<Building, Coordinate>();
	HashMap<Building, Road> roadCache = new HashMap<Building, Road>();

	private SpatialIndex si;

	public SimpleNearestRoadCoordinateCache(SpatialIndex si, HashMap<Integer, Road> roadIndex) {
		
		sTool = ContextManager.getSnapTool();
		
		this.si = si;
		
		// Context<Road> roadContext = ContextManager.roadContext;
		Context<Building> buildingContext = ContextManager.buildingContext;

		// IndexedIterable<Road> roads = roadContext.getObjects(Road.class);
		
		for (Building b: buildingContext.getObjects(Building.class)) {

			// 1. we need a jsi Point to perform the spatial query
			Point buildingCentroid = b.getCentroid();
			com.infomatiq.jsi.Point pp = new com.infomatiq.jsi.Point((float) buildingCentroid.getX(), (float) buildingCentroid.getY());
			ReturnArray r = new ReturnArray();
			
			this.si.nearestNUnsorted(pp, r, 8, kNearestEdgeDistance);
//			System.out.println(r.getResult().size() + " edges found within " + kNearestEdgeDistance + "m.");
			
			double distance = Double.MAX_VALUE;
			Coordinate cc = null;
			Road rr = null;
			
			for (Integer i : r.getResult()) {
				Road road = roadIndex.get(i);
				Coordinate c = sTool.getProjectedPointOnPolyline(road.getGeometry(),b.getCentroid().getCoordinate());
				double dist  = c.distance(b.getCoords());
				if (dist<distance) {
					distance = dist;
					cc = c;
					rr = road;
				}
			}
			coordinateCache.put(b, cc);
			roadCache.put(b, rr);
			
		}
		
	}

	

	public Coordinate getCoordinate(Building b) {
		return coordinateCache.get(b);
	}
	
	
	

	public Road getRoad(Building b) {
		return roadCache.get(b);
	}
	
}
