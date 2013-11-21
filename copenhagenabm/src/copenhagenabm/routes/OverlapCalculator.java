package copenhagenabm.routes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;

public class OverlapCalculator {

	private MatchedGPSRoute matchedGPSRoute;
	private Route simulatedRoute;

	public void addMatchedGPSRoute(MatchedGPSRoute matchedGPSRoute) {
		this.matchedGPSRoute = matchedGPSRoute;
	}

	public void addSimulatedRoute(Route route) {
		this.simulatedRoute = route;
	}

	public int getFromHashMapByGeometry(Geometry g, HashMap<Geometry, Integer> hm) {

		Set<Geometry> keySet = hm.keySet();
		for (Geometry gg : keySet) {
			if (gg.equalsExact(g)) {
				return hm.get(gg);
			}
		}

		return 0;

	}

	/**
	 * Checks whether the HashMap hm contains the Geometry g
	 * It uses Geometry.equalsExact as lookup by Geometry within HashMaps doesn't work
	 * @param g
	 * @param hm
	 * @return
	 */
	public boolean containsValue(Geometry g, HashMap<Geometry, Integer> hm) { 
		Set<Geometry> keySet = hm.keySet();
		for (Geometry gg : keySet) {
			if (gg.equalsExact(g)) {
				return true;
			}
		}

		return false;
	}

	public double getOverlap() {
		//  overlap = 1- (1/length(route_simulated) * sum (edge_sim / (1 if no overlap) (2 if overlap))) * 2
		//  note: 0.5 is a 100% overlap, 1 is a 0% overlap, therefore (1-x)*2

		// 1. let us get the matched road IDS

		HashMap<String, Double> roadLengths = new HashMap<String, Double>();

		for (Road r : this.getSimulatedRoute().getRouteAdsRoadList()) {
			String rID = "";
			try {
				rID = r.getIdentifier();
			} catch (NoIdentifierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			roadLengths.put(rID, getOrthodromicLineLength(r.getGeometry()));
		}


		ArrayList<String> matchedEdgeIDs = this.getMatchedGPSRoute().getEdgeIDs();

		// 2. and the simulated edge IDs

		ArrayList<String> simulatedEdgeIDs = this.getSimulatedRoute().getEdgeIDs();

		double sum = 0;

		for (String simID : simulatedEdgeIDs) {
			if (matchedEdgeIDs.contains(simID)) {
				sum = sum + (roadLengths.get(simID)/2);
			} else {
				sum = sum + roadLengths.get(simID);
			}
		}

		double r = (1 - (1/simulatedRoute.getLength() * sum))*2;

		if (r==1.0) {
			System.out.println("1.0");
		}

		return r;

	}
	//
	//	public double _DEPRECATED_getOverlap() {
	//
	//
	//		//  overlap = 1- (1/length(route_simulated) * sum (edge_sim / (1 if no overlap) (2 if overlap))) * 2
	//		//  note: 0.5 is a 100% overlap, 1 is a 0% overlap, therefore (1-x)*2
	//		
	//		
	//		// 1. build a dict of overlaps, dict contains edges of the simulated route and shall receive values 
	//
	//		HashMap<Geometry, Integer> simulatedHashMap = new HashMap<Geometry, Integer>();
	//
	//		for (Geometry g: simulatedRoute.getRouteAsEdges()) {
	//			simulatedHashMap.put(g, 0);
	//		}
	//
	//		ArrayList<Geometry> edges = matchedGPSRoute.getRouteAsEdges();
	//		
	//		boolean contains_null = edges.contains(null);
	//		if (contains_null) {
	//			edges = matchedGPSRoute.getRouteAsEdges();
	//		}
	//		
	//		for (Geometry g : edges) {
	//			// containsvalue would not work - have to loop 
	//
	//			//if (simulatedHashMap.containsValue(g)) {
	//			if (containsValue(g, simulatedHashMap)) {
	//				//simulatedHashMap.put(g, simulatedHashMap.get(g)+1);
	//				simulatedHashMap.put(g, getFromHashMapByGeometry(g, simulatedHashMap) + 1);
	//
	//			} 
	//		}
	//		
	//		double s = 0.0d;
	//		
	//		Set<Geometry> sMKeySet = simulatedHashMap.keySet();
	//		
	//		for (Geometry g : sMKeySet) {
	//			int v = getFromHashMapByGeometry(g, simulatedHashMap);
	//			if (v == 0) {
	//				s = s + g.getLength() / 1;
	//			} else {
	//				s = s + g.getLength() / 2;
	//			}
	//		}
	//		double r = (1 - (1/simulatedRoute.getLength()*s))*2;
	//		
	//		if (r==1.0) {
	//			System.out.println("1.0");
	//		}
	//		
	//		return r;
	//
	//	}

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

			l = l + ContextManager.simpleDistance.distance(cA.get(i), cA.get(i+1));

		}

		return l;
	}

	public MatchedGPSRoute getMatchedGPSRoute() {
		return matchedGPSRoute;
	}

	public void setMatchedGPSRoute(MatchedGPSRoute matchedGPSRoute) {
		this.matchedGPSRoute = matchedGPSRoute;
	}

	public Route getSimulatedRoute() {
		return simulatedRoute;
	}

	public void setSimulatedRoute(Route simulatedRoute) {
		this.simulatedRoute = simulatedRoute;
	}

}
