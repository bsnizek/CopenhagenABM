package copenhagenabm.main;

import java.util.ArrayList;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Geometry;

public class MatchedRouteCache {
	
	private HashMap<Integer, ArrayList<Geometry>> matchedRoutes = new HashMap<Integer, ArrayList<Geometry>>();
	
	public MatchedRouteCache() {
		
	}

	public boolean hasRoute(int objectid) {
		return matchedRoutes.containsKey(objectid);
	}
	
	public ArrayList<Geometry> get(int objectid) {
		return matchedRoutes.get(objectid);
	}
	
	public void put(int objectid, ArrayList<Geometry> g) {
		matchedRoutes.put(objectid, g);
	}

}
