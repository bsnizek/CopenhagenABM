package copenhagenabm.routes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.geotools.feature.SchemaException;

import com.infomatiq.jsi.SpatialIndex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.tools.SnapTool.ReturnArray;
import gnu.trove.TIntProcedure;

/**
 * The RouteStore's purpose is to contain 
 * @author besn
 *
 */

public class RouteStore {

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

	/**
	 * A geometry factory we need to build small polylines
	 */
	GeometryFactory fact = new GeometryFactory();

	/**
	 * here we store the GPS routes
	 */
	HashMap<Integer, MatchedGPSRoute> matchedGPSRoutes = new HashMap<Integer, MatchedGPSRoute>();


	/**
	 * here we store the GPS Routes as Routes with same IDs as the ones from 
	 */
	HashMap<Integer, Route> matchedAdaptedRoutes = new HashMap<Integer, Route>();


	/**
	 * Simulated routes, keyed by GPS route ID, then by 
	 */
	HashMap<Integer, ArrayList<Route>> simulatedRoutes = new HashMap<Integer, ArrayList<Route>>();

	PathSizeSet pss = null;

	private HashMap<Integer, Double> averagePathSizeAttr = new HashMap<Integer, Double>();

	public HashMap<Integer, Double> getAveragePathSizeAttr() {
		return averagePathSizeAttr;
	}

	public void setAveragePathSizeAttr(HashMap<Integer, Double> averagePathSizeAttr) {
		this.averagePathSizeAttr = averagePathSizeAttr;
	}

	public void addMatchedGPSRoute(MatchedGPSRoute resultRoute) {
		int ID = resultRoute.getOBJECTID();
		matchedGPSRoutes.put(ID, resultRoute);

		// now we run the adaptation algorithm

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

	public void addSimulatedRoute(Route r) {
		ArrayList<Route> aID = simulatedRoutes.get(r.getGPSID());
		if (aID == null || aID.size() == 0) {

			simulatedRoutes.put(r.getGPSID(), new ArrayList<Route>());
		}
		simulatedRoutes.get(r.getGPSID()).add(r);
	}

	/**
	 * calculates the path size of the average routes against the GPS route
	 */
	public void calculatePathSize() {

		Set<Integer> matchedGPSIDs = matchedGPSRoutes.keySet();

		for (Integer matchedGPSID : matchedGPSIDs) {
			MatchedGPSRoute mGPSRoute = matchedGPSRoutes.get(matchedGPSID);
			int ID = mGPSRoute.getOBJECTID();

			// get all routes with ID
			ArrayList<Route> routes = simulatedRoutes.get(ID);

			ArrayList<Double> pAttrs = new ArrayList<Double>();

			for (Route r : routes) {

				ArrayList<Route> pRoutes = new ArrayList<Route>();
				pRoutes.add(r);

				pss = new PathSizeSet(pRoutes);

				Route bestRoute = pss.getBestRoute();

				double pAttr = bestRoute.getPathSizeAttr();
				pAttrs.add(pAttr);

				r.setPathSizeAttr(pAttr);

			}

			double avgAttr = calculateAverage(pAttrs);

			this.averagePathSizeAttr .put(ID, avgAttr);



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


	private double calculateAverage(ArrayList <Double> marks) {
		Double sum = 0d;
		for (Double mark : marks) {
			sum += mark;
		}
		return sum.doubleValue() / marks.size();
	}


	public HashMap<Integer, MatchedGPSRoute> getMatchedGPSRoutes() {
		return matchedGPSRoutes;
	}

	public HashMap<Integer, ArrayList<Route>> getSimulatedRoutes() {
		return simulatedRoutes;
	}

	public void writeResultRoutes() {

		if (ContextManager.inCalibrationMode()) {
			try {
				RouteStoreWriter rsw = new RouteStoreWriter(this, ContextManager.getPathSizeSetFile());
				rsw.write();
				rsw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}

}