package copenhagenabm.routes;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.HashMap;

import com.infomatiq.jsi.SpatialIndex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;

public class PathSizeCalculator {

	/**
	 * A geometry factory we need to build small polylines
	 */
	GeometryFactory fact = new GeometryFactory();

	PathSizeSet pss = null;

	private ArrayList<Route> routes = new ArrayList<Route>();

	public void addSimulatedRoute(Route r) {
		this.routes.add(r);
	}

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

	public void addMatchedGPSRoute(MatchedGPSRoute resultRoute) {
		Coordinate[] coords = resultRoute.getGeometry().getCoordinates();

		SpatialIndex roadSpatialIndex = ContextManager.getRoadSpatialIndex();
		HashMap<Integer, Road> roadIndex = ContextManager.getRoadindex();


		int lastRodID = -1;
		ArrayList<Integer> roadIndexList = new ArrayList<Integer>();

		// we want a list of edges, which 

		// let us run through the coordinates
		for (int i=0; i<coords.length-1;i++) {
			Coordinate c1 = coords[i];
			Coordinate c2 = coords[i+1];
			Coordinate[] cs = new Coordinate[2];
			cs[0] = c1;
			cs[1] = c2;
			LineString lineString = fact.createLineString(cs);
			ReturnArray r = new ReturnArray();
			com.infomatiq.jsi.Point pp = new com.infomatiq.jsi.Point((float) lineString.getCentroid().getX(), (float) lineString.getCentroid().getY());
			roadSpatialIndex.nearest(pp, r, 0);

			int resultID = -1;
			ArrayList<Integer> result = r.getResult();
			if (result.size()>0) {
				// what do we do when it's zero ? 
				resultID = result.get(0);
				if (resultID != lastRodID) {
					roadIndexList.add(resultID);
				}
			}

		}

		// now build the Route
		Route nRoute = new Route(-1, -1, -1);
		for (int ii : roadIndexList) {
			nRoute.addEdgeGeometry(roadIndex.get(ii), roadIndex.get(ii).getGeometry());
		}

		addSimulatedRoute(nRoute);
	}

	/**
	 * calculates the path size of the average routes against the GPS route
	 */
	public double calculatePathSize() {

		

			pss = new PathSizeSet(this.routes);

			Route bestRoute = pss.getBestRoute();

			return bestRoute.getPathSizeAttr();



		//double avgAttr = calculateAverage(pAttrs);

		// this.averagePathSizeAttr .put(ID, avgAttr);



		//			Route bestRoute = pss.getBestRoute();
		//
		//			for (Route r : routes) {
		//				if (r.getID()==bestRoute.getID()) {
		//					r.setBestRoute(true);
		//					break;
		//				}
		//			}

	}


}
