package copenhagenabm.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.gis.GISAdder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repastcity3.environment.Building;
import repastcity3.environment.Junction;
import repastcity3.environment.NetworkEdge;
import repastcity3.environment.contexts.BuildingContext;
import repastcity3.environment.contexts.RoadContext;
import repastcity3.exceptions.DuplicateRoadException;
import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.tools.SimpleDistance;
import copenhagenabm.tools.SnapTool;

public class TestSplitRoadOutside {
	
	private static final int kNearestEdgeDistance = 10000;
	private static final String TARGET_EPSG = "EPSG:2197";
	private boolean DEBUG_MODE = true;
	private SnapTool sTool;
	GeometryFactory fact = new GeometryFactory();
	private HashMap<Integer, Road> roadIndex;
	private static Junction j1;
	private static Junction j2;
	private Road road1;

	
	void setUpBasics() {
		ContextManager cm = new ContextManager();
		RunEnvironment re = RunEnvironment.getInstance();

		sTool = ContextManager.getSnapTool();

		Context<Building> buildingContext = new BuildingContext();

		Context<Road> roadContext = new RoadContext();


		try {
			ContextManager.readProperties();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		ContextManager.setSnapTool(new SnapTool());
		
		Geography<Road> roadProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY, roadContext,
				new GeographyParameters<Road>(new SimpleAdder<Road>()));

		ContextManager.roadProjection = roadProjection;
		ContextManager.roadContext = roadContext;

		CoordinateReferenceSystem crs = ContextManager.roadProjection.getCRS();

		ContextManager.setCrs(crs);

		ContextManager.simpleDistance = new SimpleDistance(ContextManager.getCrs(), TARGET_EPSG);
		
		roadIndex = new HashMap<Integer, Road>();
		
	}

	void setUp() {
		
		Coordinate c1 = new Coordinate(0,0);
		Coordinate c2 = new Coordinate(10,0);
		
		ArrayList<Coordinate> l1 = new ArrayList<Coordinate>();
		l1.add(c1);
		l1.add(c2);
		
		Coordinate[] coordinates = l1.toArray(new Coordinate[l1.size()]);
		LineString ls1 = fact.createLineString(coordinates);
		
		road1 = new Road();
		road1.setGeometry(ls1);
		try {
			road1.setIdentifier("1");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		j1 = new Junction();
		j1.setCoords(c1);
		
		j2 = new Junction();
		j2.setCoords(c2);
		
		road1.addJunction(j1);
		road1.addJunction(j2);
		
		NetworkEdge<Junction> nE1 = new NetworkEdge<Junction>(j1, j2, false, 1.0d);
		
		road1.setEdge(nE1);
		
		ContextManager.roadIndex = roadIndex;

		GISAdder<Road> adder = new SimpleAdder<Road>();
		ContextManager.roadProjection.setAdder(adder);
		adder.add(ContextManager.roadProjection, road1);
		
	}
	
	public Road splitRoad(Junction j, Coordinate coordinate) {
		
		return new Road(road1, coordinate, j, "-1");
		
	}

	public static void main(String[] args) {
		TestSplitRoadOutside tSRO = new TestSplitRoadOutside();
		tSRO.setUpBasics();
		tSRO.setUp();
		Road r1 = tSRO.splitRoad(j2, new Coordinate(12,0));
		Road r2 = tSRO.splitRoad(j1, new Coordinate(12,0));
		System.out.println(r1.getGeometry());
		System.out.println(r2.getGeometry());
	}

}
