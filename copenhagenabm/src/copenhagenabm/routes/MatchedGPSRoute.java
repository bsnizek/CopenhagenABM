package copenhagenabm.routes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.environment.FixedGeography;
import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.MatchedRouteCache;
import copenhagenabm.tools.SimpleDistance;
import copenhagenabm.tools.SnapTool;

import com.vividsolutions.jts.geom.GeometryFactory;


public class MatchedGPSRoute implements FixedGeography {

	// TODO: throw this into the settings file
	private static final String TARGET_EPSG = "EPSG:2197";

	private static GeometryFactory fact = new GeometryFactory();
	
	ArrayList<String> edgeIDs = new ArrayList<String>();

	private SnapTool snapTool;

	private Coordinate coord;
	private int OBJECTID;

	//	private ArrayList<Geometry> edgeList = new ArrayList<Geometry>();

	public MatchedGPSRoute() {
		snapTool = ContextManager.getCopenhagenABMTools().getSnapTool();
	}


	public int getOBJECTID() {
		return OBJECTID;
	}

	public void setOBJECTID(int oBJECTID) {
		OBJECTID = oBJECTID;
	}

	public Coordinate getCoords() {
		return coord;
	}

	public void setCoords(Coordinate c) {
		coord = c;

	}

	public CoordinateReferenceSystem getCRS() {
		return ContextManager.getCrs();
	}

	public double getLengthInMetres() {

		SimpleDistance sd = new SimpleDistance(this.getCRS(), TARGET_EPSG);

		return sd.geometryLengthInMetres(this.getGeometry());

	}
	
	public Coordinate[] getCoordinates() {
		return ContextManager.matchedGPSRouteProjection.getGeometry(this).getCoordinates();
	}
	
	public ArrayList<Coordinate> getCoordinatesAsArrayList() {
		return new ArrayList<Coordinate>(Arrays.asList(getCoordinates()));
	}
	
	public Coordinate getOrigin() {
		return getCoordinatesAsArrayList().get(0);
	}
	
	public Coordinate getDestination() {
		return getCoordinatesAsArrayList().get(getCoordinatesAsArrayList().size()-1);
	}

	/**
	 * returns the Geometry of the route as a LineString
	 * @return
	 */
	public LineString getGeometry() {
		return fact.createLineString(getCoordinates());

	}

	public int getOccurrencesOfEdge_2(Geometry geometry, boolean useDir) {
		int n = 0;
		ArrayList<Geometry> edgeList = this.getRouteAsEdges();
		for (Geometry e : edgeList) {
			if (useDir) {	// consider direction
				if (geometry == e) n++;
			} else {	// direction independent
				if (geometry == e) n++;	// important: compare the undirected edges!
			}
		}
		return n;
	}

	/**
	 * getRouteAsEdges()
	 * 
	 * Takes the route and pulls out a corresponding route based on edges of the road network
	 * which belongs to the road context. 
	 * 
	 * We need that for the comparison if the modeled route to the actually taken route
	 * 
	 * @return
	 */
	public ArrayList<Geometry> getRouteAsEdges() {
		MatchedRouteCache mRC = ContextManager.getMatchedRouteCache();

		if (mRC.hasRoute(this.getOBJECTID())) 
			return mRC.get(this.getOBJECTID());
		else {
			// we put the edge IDs into a list as well for debug reasons. 		
			

			ArrayList<Geometry> edges = new ArrayList<Geometry>();

			// we have to get the edges from the road network, right now we only have only one long LineString.

			// 1. We convert the LineString to a list of Coordinates
			Coordinate[] lineStringCoordinates = this.getGeometry().getCoordinates();

			// 2. We build an ArrayList of centers of substrings, i.e. centers of every substring of f LineString

			ArrayList<Coordinate> centroids = new ArrayList<Coordinate>();

			List<Coordinate> rc =  Arrays.asList(lineStringCoordinates);
			for (int j=0; j<rc.size()-1;j++) {
				Coordinate[] cs = {rc.get(j),rc.get(j+1)};

				if (rc.get(j).equals(rc.get(j+1))) {

					if (ContextManager.getDEBUG_MODE()) {
						System.out.println("NULL linestring for route=" + this.getOBJECTID() + ".");
					}

				} else {

					LineString l = fact.createLineString(cs);
					Coordinate c = l.getCentroid().getCoordinate();


					centroids.add(c);
				}

			}

			// 3. Now loop through the centroids and retrieve the roads
			// 4. and add them to edges 

			Road lastRoad = null;
			for (Coordinate c : centroids) {
				Road theRoad = snapTool.getRoadByCoordinate(c);
				// we get all the small road segments for every vertex->vertex relation; check therefore whether we have already added the road.
				if (lastRoad != theRoad) {
					edges.add(theRoad.getGeometry());

					try {
						edgeIDs.add(theRoad.getIdentifier());
					} catch (NoIdentifierException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					lastRoad = theRoad;
				}

			}

			// 5.This here for debug reasons
			if (ContextManager.getDEBUG_MODE()) {
				String s = "\"identifier\" IN (";
				for (String eI : edgeIDs) {
					s = s + "'" + eI + "'" + ",";
				}

				s = s.substring(0, s.length()-1) + ")";

				System.out.println("Route ID=" + this.getOBJECTID() + " " + s); 
			}
			mRC.put(this.getOBJECTID(), edges);
			return edges;
		}
	}

	//		Coordinate firstC = lineString[0];

	//		// get the Road
	//
	//		Road firstRoad = snapTool.getRoadByCoordinate(firstC);
	//
	//		Geometry currentEdge = firstRoad.getGeometry();
	//		if (currentEdge == null) {
	//			currentEdge = firstRoad.getGeometry();
	//		}
	//
	//		edges.add(currentEdge);
	//
	//		try {
	//			edgeIDs.add(firstRoad.getIdentifier());
	//		} catch (NoIdentifierException e1) {
	//			// TODO Auto-generated catch block
	//			e1.printStackTrace();
	//		}
	//
	//		for (int i=1; i<lineString.length; i++) {
	//			Road theRoad = snapTool.getRoadByCoordinate(lineString[i]);
	//			if (theRoad.getGeometry() != currentEdge) {
	//				currentEdge = theRoad.getGeometry();
	//				if (currentEdge==null) {
	//					Geometry geom = theRoad.getGeometry();
	//					try {
	//						System.out.println(theRoad.getIdentifier() + " has no edge  " + geom);
	//					} catch (NoIdentifierException e) {
	//						// TODO Auto-generated catch block
	//						e.printStackTrace();
	//
	//					}
	//				}
	//				edges.add(currentEdge);
	//
	//				try {
	//					edgeIDs.add(theRoad.getIdentifier());
	//				} catch (NoIdentifierException e) {
	//					// TODO Auto-generated catch block
	//					e.printStackTrace();
	//				}
	//			}
	//		}

	/*
	 * 
	 * Returns the number of edges of this route. The edges are taken from the 
	 * road theme. 
	 * 
	 */
	public int getNumberOfEdges() {
		return this.getRouteAsEdges().size();
	}
	
	public String edgeIDasString() {
		String s = "\"identifier\" IN (";
		for (String f : edgeIDs) {
			s = s + "'" + f + "'" + ",";
		}
		return s.substring(0,s.length()-1) + ")";
	}

	public String toString() {
		return "ID=" + this.getOBJECTID() + " n=" + this.getNumberOfEdges() + " l=" + getLengthInMetres() + " " + edgeIDasString();
	}


	public ArrayList<String> getEdgeIDs() {
		return edgeIDs;
	}

}
