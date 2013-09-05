package copenhagenabm.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Edge;

import copenhagenabm.environment.Road;

/*
 * PathSizeSet 
 * 
 * original (c) Bernhard Barkow
 * 
 */
public class PathSizeSet {

	private ArrayList<Route> routes;
	private HashMap<String, Integer> edgeOccurrences = new HashMap<String, Integer>();
	
	public PathSizeSet(ArrayList<Route> routes) {
		this.routes = routes;
		calcPathSizeAttributes();
	}
	
	/**
	 * initialize the edge counters (in how many labels (routes) each edge is contained)
	 */
	private void initEdges() {
		for (Route route : routes) {
			List<Road> edges = route.getRouteAdsRoadList();
			for (Road road : edges) {
				// Edge e = de.getEdge();
				// use the edge ID as key for the HashMap 
				// (this is better than using the Edge itself, because these change between runs (due to SplitGraphAtPoint etc.)):
				
				String roadID = null;
				try {
					roadID = road.getIdentifier();
				} catch (NoIdentifierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (edgeOccurrences.containsKey(roadID)) edgeOccurrences.put(roadID, edgeOccurrences.get(roadID) + 1);
				else edgeOccurrences.put(roadID, 1);
			}
		}
		// now we have a HashMap containing the Edges as keys and the number of their occurrence as values
	}
	
	/**
	 * Calculate the Path Size Attribute for all routes 
	 */
	public void calcPathSizeAttributes() {
		initEdges();
		for (Route route : routes) {
			route.getPathSize_global(edgeOccurrences);
			// additionally, we could store the PS in a HashMap here?
		}
	}
	
	/*
	 * returns the Route with the maximum path size score
	 */
	public Route getBestRoute() {
		double score = 0.0;
		Route bestRoute = null;
		for (Route r : routes) {
			if (r.getPathSizeAttr()>score) {
				score = r.getPathSizeAttr();
				bestRoute = r;
			}
		}
		bestRoute.setBestRoute(true);
		return bestRoute;
		
	}
	

	
	
}
