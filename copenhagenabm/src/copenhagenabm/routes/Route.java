package copenhagenabm.routes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.environment.FixedGeography;
import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.tools.SimpleDistance;


/**
 * Route.java contains the Route taken buy an agent and provides with tools to cpompare to other routes. 
 * 
 * 
 * @author Bernhard Snizek <b@snizek.com>
 *
 */
public class Route implements FixedGeography {

	// TODO: throw this into the settings file
	private static final String TARGET_EPSG = "EPSG:2197";

	private static com.vividsolutions.jts.geom.GeometryFactory fact = new com.vividsolutions.jts.geom.GeometryFactory();

	public Geometry the_geom = null;
	private Geometry coord = null;

	private ArrayList<Geometry> edgeList = new ArrayList<Geometry>();

	private int agentID;

	// a list of roads for debug reasons
	// TODO: remove
	private ArrayList<Road> roadList = new ArrayList<Road>();

	private double pathSizeAttr = 0.0d;

	private boolean bestRoute = false;

	public CoordinateReferenceSystem getCRS() {
		return ContextManager.getCrs();
	}


	public Route(int uniqueID, int gPSID, Integer modelRun) {
		this.agentID = uniqueID;
		this.GPSID = gPSID;
		this.setModelRun(modelRun);
	}

	public boolean isBestRoute() {
		return bestRoute;
	}

	/*
	 * The ID of the GPS track
	 */
	private int GPSID;

	/**
	 * The ID of the model run in calibration mode
	 */
	private Integer modelRunID;

	public double getPathSizeAttr() {
		return pathSizeAttr;
	}


	public void setPathSizeAttr(double pathSizeAttr) {
		this.pathSizeAttr = pathSizeAttr;
	}


	public int getID() {
		return this.agentID;
	}



	public int getGPSID() {
		return GPSID;
	}


	public void setGPSID(int gPSID) {
		GPSID = gPSID;
	}


	public void addEdgeGeometry(Road r, Geometry g) {
		edgeList.add(g);

		//		try {
		//			System.out.println(g.getCoordinates()[0] + "\t" + r.getIdentifier() + "\t" + g.getCoordinates().length);
		//		} catch (NoIdentifierException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

		roadList.add(r);
	}

	public ArrayList<LineString> getRouteAsLineStringList() {
		ArrayList<LineString> a = new ArrayList<LineString>();
		for (Geometry m : this.edgeList) {
			a.add(fact.createLineString(m.getCoordinates()));
		}
		return a;
	}

	public double getLengthInMetres() {

		SimpleDistance sd = new SimpleDistance(this.getCRS(), TARGET_EPSG);

		return sd.geometryLengthInMetres(this.getGeometry());

	}

	public LineString getRouteAsLineString() {

		
		// This is an evil hack but now way around. getR...List sometimes does rerturn null
		
		ArrayList<LineString> edges = null;
		
		int loopCounter = 0;
		while (edges == null) {
			edges = getRouteAsLineStringList();
			loopCounter++;
			if (loopCounter == 5) {
				try {
					throw new RouteAsLineStringListError();
				} catch (RouteAsLineStringListError e) {
					e.printStackTrace();
				}
				break;
			}
		}
		
		
		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();

		if (edges.size()>1) {

			LineString firstSegment = edges.get(0);
			LineString secondSegment = edges.get(1);

			Coordinate firstSegmentFirstCoord = firstSegment.getCoordinates()[0];
			Coordinate[] firstSegCoords = firstSegment.getCoordinates();
			Coordinate firstSegmentLastCoord = firstSegment.getCoordinates()[firstSegCoords.length-1];

			Coordinate secondSegmentFirstCoord = secondSegment.getCoordinates()[0];
			Coordinate secondSegmentLastCoord = secondSegment.getCoordinates()[secondSegment.getCoordinates().length-1];
			Coordinate lastLastCoordinate;

			//		double kSnapDistance = 0.5;
			//		boolean isFirstForward = (firstSegmentLastCoord.distance(secondSegmentFirstCoord) < kSnapDistance ||
			//								  firstSegmentLastCoord.distance(secondSegmentLastCoord)  < kSnapDistance);
			// boolean isFirstForward = (firstSegmentLastCoord.distance(secondSegmentFirstCoord) < firstSegmentLastCoord.distance(secondSegmentLastCoord));

			double d11 = firstSegmentFirstCoord.distance(secondSegmentFirstCoord);
			double d12 = firstSegmentFirstCoord.distance(secondSegmentLastCoord);
			double d21 = firstSegmentLastCoord.distance(secondSegmentFirstCoord);
			double d22 = firstSegmentLastCoord.distance(secondSegmentLastCoord);

			double minDist1 = Math.min(d11, d12);
			double minDist2 = Math.min(d21, d22);
			double minDist = Math.min(minDist1, minDist2);

			boolean isFirstForward = false;
			if (d11 == minDist) isFirstForward = false;
			if (d12 == minDist) isFirstForward = false;
			if (d21 == minDist) isFirstForward = true;
			if (d22 == minDist) isFirstForward = true;

			/*if (isFirstForward) {	// let us add all the coordinates
			coords.add(firstSegCoords[0]);
			coords.add(firstSegCoords[1]);
			coords.add(firstSegCoords[2]);
		} else {
			coords.add(firstSegCoords[firstSegCoords.length-1]);
			coords.add(firstSegCoords[firstSegCoords.length-2]);
			coords.add(firstSegCoords[firstSegCoords.length-3]);
		}
			 */

			if (isFirstForward) {	// let us add all the coordinates
				/*for (int k=0; k < firstSegCoords.length; k++) {
				// System.out.println(k + "/t" + coords.size());
				coords.add(firstSegCoords[k]);
			}*/
				coords.addAll(Arrays.asList(firstSegCoords));
			} else {	// we have to reverse the first segment
				//List<Coordinate> x = Arrays.asList(firstSegCoords);
				//Collections.reverse(x);
				// coords.addAll(x);
				for (int j = firstSegCoords.length-1; j>0; j--) {
					coords.add(firstSegCoords[j]);
				}
			}

			lastLastCoordinate = coords.get(coords.size()-1);

			// now we loop from the second segment towards the last
			for (int i = 1; i < edges.size(); i++) {

				Geometry m = edges.get(i);
				Coordinate[] mCoordinates = m.getCoordinates();
				int nmc = mCoordinates.length;

				// now check whether this segment is oriented backward or forward

				Coordinate firstCoordinate = mCoordinates[0];
				Coordinate lastCoordinate  = mCoordinates[nmc-1];

				double d = firstCoordinate.distance(lastLastCoordinate);
				double dLast = lastCoordinate.distance(lastLastCoordinate); // just for debug reasons
				// System.out.println("i,n,d,dLast="+i+"\t"+nmc+"\t"+d+"\t"+dLast);

				boolean bReverse = (dLast < d);

				if (!bReverse) {
					//			if (d < kSnapDistance) {	// forward, no reversing
					// we do not take the first one as it is already covered from the last one
					//coords.addAll(Arrays.asList(mCoordinates));
					for (int j = 1; j < nmc; j++) {
						coords.add(mCoordinates[j]);
					}
				} else {	
					//			} else if (dLast < kSnapDistance) {	// backwards, reverse
					for (int j = nmc-2; j >= 0; j--) {
						coords.add(mCoordinates[j]);
					}
					//			} else {
					//				System.err.println("We've got a problem! d,dLast="+d+"\t"+dLast);
				}
				// do it only once:
				lastLastCoordinate = coords.get(coords.size()-1);
			}

			//		Coordinate[] cc = (Coordinate[]) coords.toArray(new Coordinate[coords.size()]);
			Coordinate[] cc = new Coordinate[coords.size()];
			int i = 0;
			for (Coordinate c : coords) {
				cc[i] = c;
				i++;
			}
			return fact.createLineString(cc);
		} else {
			
			if (edges.size()==0) {
				edges = getRouteAsLineStringList();
			}
			
			return edges.get(0);
		}
	}



	public double getLength() {
		LineString ras = this.getRouteAsLineString();
		return ras.getLength();
	}

	/**
	 * Calculate the overlap factor, a value in [0...1]: 0 means no overlap, 1 means the whole route is contained in lbl0.
	 * See also: E. Frejinger, M. Bierlaire, M. Ben-Akiva: Expanded Path Size Attribute, March 2009 
	 * @param lbl0 the other label (route) to compare this one to
	 * @param useDir if true, the edge direction is considered, if not, the overlap is computed independent of the edge direction 
	 * @return the overlap factor [0...1]
	 */
	public double getOverlap(Route overlapRoute, boolean useDir) {

		double ps = 0;
		for (Geometry g : this.getRouteAsEdges()) {
			if (overlapRoute.getOccurrencesOfEdge_2(g, useDir) != 0) {
				ps += g.getLength();
			}
		}

		return ps;
	}

	public double getOverlap(MatchedGPSRoute overlapRoute, boolean useDir) {

		double ps = 0;
		for (Geometry g : this.getRouteAsEdges()) {
			if (overlapRoute.getOccurrencesOfEdge_2(g, useDir) != 0) {
				ps += g.getLength();
			}
		}

		return ps;
	}


	private int getOccurrencesOfEdge_2(Geometry geometry, boolean useDir) {
		int n = 0;
		edgeList = getRouteAsEdges();
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
	 * Returns a route as an ordered list of edges
	 * @return
	 */
	public ArrayList<Geometry> getRouteAsEdges() {
		return this.edgeList;
	}

	public ArrayList<Road> getRouteAdsRoadList() {
		return this.roadList;
	}

	/**
	 * 
	 * (c) Bernhard Barkow
	 * 
	 * Calculate the Path Size in a global context.
	 * See also: E. Frejinger, M. Bierlaire, M. Ben-Akiva: Expanded Path Size Attribute, March 2009 
	 * @param weights a HashMap containing the number of occurrences of all Edges 
	 * @return the Path Size Attribute
	 */
	public double getPathSize_global(HashMap<String, Integer> weights) {
		double ps = 0;
		for (Road r : getRouteAdsRoadList() ) {

			String edgeID = null;
			try {
				edgeID = r.getIdentifier();
			} catch (NoIdentifierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Integer ne = (weights.containsKey(edgeID) ? weights.get(edgeID) : 1);
			//System.out.print(ne + "\t");
			ps += r.getGeometry().getLength() / (double) ne;	// add length of this edge, divided by number of uses
		}
		pathSizeAttr  = ps / getLength();	// store in class variable
		// System.out.println("\n" + getLength() + " ps: " + ps);
		return pathSizeAttr;
	}


	public void setBestRoute(boolean b) {
		this.bestRoute = true;

	}


	public Integer getModelRun() {
		return modelRunID;
	}


	public void setModelRun(Integer modelRun) {
		this.modelRunID = modelRun;
	}

	public Coordinate getCoords() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setCoords(Coordinate c) {
		// TODO Auto-generated method stub

	}

	public Geometry getThe_geom() {
		return getRouteAsLineString();
	}

	public void setThe_geom(Geometry the_geom) {
		this.the_geom = the_geom;
	}

	public Geometry getGeometry() {
		return getRouteAsLineString();
	}

	public Geometry getCoord() {
		return coord;
	}

	public void setCoord(Geometry coord) {
		this.coord = coord;
	}

	/**
	 * isInALoop() checks whether an edge appears at least 'occurence' times in a windows of 'window' size of the agents roadList i.e. the list over visited edges.
	 * @param occurence : number of occurences in road list
	 * @param window : search window
	 * @return
	 */
	public boolean isInALoop(int window, int occurence) {
		ArrayList<Road> rl = (ArrayList<Road>) this.roadList.clone();
		Collections.reverse(rl);
		if (window <= rl.size()) {
			List<Road> sublist = rl.subList(0, window);
			HashMap<String, Integer> hm = new HashMap<String,Integer>();
			for (Road r : sublist) {
				try {
					if (hm.containsKey(r.getIdentifier())) {
						int d = hm.get(r.getIdentifier());
						hm.put(r.getIdentifier(), d+1);
					} else {
						hm.put(r.getIdentifier(), 1);
					}
				} catch (NoIdentifierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			for (Integer ii : hm.values()) {
				if (ii>=occurence) {
					return true;
				}
			}

		}

		return false;
	}


	/**
	 * removes last rode and edge
	 */
	public void removeLastRoad() {
		//		try {
		//			System.out.println("Removing edge with ID=" + this.roadList.get(this.roadList.size()-1).getIdentifier());
		//		} catch (NoIdentifierException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		this.roadList.remove(this.roadList.size()-1);
		this.edgeList.remove(this.edgeList.size()-1);

	}

}
