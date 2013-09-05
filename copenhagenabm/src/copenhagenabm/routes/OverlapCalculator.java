package copenhagenabm.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;

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
		
		
		// 1. build a dict of overlaps, dict contains edges of the simulated route and shall receive values 

		HashMap<Geometry, Integer> simulatedHashMap = new HashMap<Geometry, Integer>();

		for (Geometry g: simulatedRoute.getRouteAsEdges()) {
			simulatedHashMap.put(g, 0);
		}

		for (Geometry g : matchedGPSRoute.getRouteAsEdges()) {
			// containsvalue would not work - have to loop 

			//if (simulatedHashMap.containsValue(g)) {
			if (containsValue(g, simulatedHashMap)) {
				//simulatedHashMap.put(g, simulatedHashMap.get(g)+1);
				simulatedHashMap.put(g, getFromHashMapByGeometry(g, simulatedHashMap) + 1);

			} 
		}
		
		double s = 0.0d;
		
		Set<Geometry> sMKeySet = simulatedHashMap.keySet();
		
		for (Geometry g : sMKeySet) {
			int v = getFromHashMapByGeometry(g, simulatedHashMap);
			if (v == 0) {
				s = s + g.getLength() / 1;
			} else {
				s = s + g.getLength() / 2;
			}
		}
		double r = (1 - (1/simulatedRoute.getLength()*s))*2;
		
		if (r==1.0) {
			System.out.println("1.0");
		}
		
		return r;



	}

}
