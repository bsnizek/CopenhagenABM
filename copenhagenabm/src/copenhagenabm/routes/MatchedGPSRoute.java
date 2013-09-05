package copenhagenabm.routes;

import java.util.ArrayList;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.environment.FixedGeography;
import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.tools.SimpleDistance;
import copenhagenabm.tools.SnapTool;

import com.vividsolutions.jts.geom.GeometryFactory;


public class MatchedGPSRoute implements FixedGeography {

	// TODO: throw this into the settings file
	private static final String TARGET_EPSG = "EPSG:2197";

	private static GeometryFactory fact = new GeometryFactory();

	private Coordinate coord;
	private int OBJECTID;

	private ArrayList<Geometry> edgeList = new ArrayList<Geometry>();

	private SnapTool snapTool;

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

	/**
	 * returns the Geometry of the route as a LineString
	 * @return
	 */
	public LineString getGeometry() {
		Coordinate[] cc = ContextManager.matchedGPSRouteProjection.getGeometry(this).getCoordinates();
		return fact.createLineString(cc);

	}

	public int getOccurrencesOfEdge_2(Geometry geometry, boolean useDir) {
		int n = 0;
		edgeList = this.getRouteAsEdges();
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
	 * 
	 * Takes the route and pulls out a corresponding route which belongs to the road context. 
	 * Nice, isn't it ? 
	 * 
	 * Kinda identity in GIS terms
	 * 
	 * @return
	 */
	public ArrayList<Geometry> getRouteAsEdges() {

		ArrayList<Geometry> edges = new ArrayList<Geometry>();

		// we have to get the edges from the road network, right now we only have 
		Coordinate[] lineString = this.getGeometry().getCoordinates();

		Coordinate firstC = lineString[0];

		// get the Road

		Road firstRoad = snapTool.getRoadByCoordinate(firstC);

		Geometry currentEdge = firstRoad.getGeometry();
		edges.add(currentEdge);

		for (int i=1; i<lineString.length; i++) {
			Road theRoad = snapTool.getRoadByCoordinate(lineString[i]);
			if (theRoad.getGeometry() != currentEdge) {
				currentEdge = theRoad.getGeometry();
				edges.add(currentEdge);
			}
		}
		return edges;

	}

	/*
	 * Returns the number of edges of this route.
	 */
	public int getNumberOfEdges() {
		return this.getRouteAsEdges().size();
	}

}
