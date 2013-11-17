package copenhagenabm.tests;

import gnu.trove.TIntProcedure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.test.SpatialIndexFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.agent.CPHAgent;
import copenhagenabm.agent.IAgent;
import copenhagenabm.agent.OvershootData;
import copenhagenabm.agent.PolyLineMover;
import copenhagenabm.environment.Person;
import copenhagenabm.environment.Road;
import copenhagenabm.environment.SimpleNearestRoadCoordinateCache;
import copenhagenabm.environment.Zone;
import copenhagenabm.environment.contexts.AgentContext;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.tools.SimpleDistance;
import copenhagenabm.tools.SnapTool;



/***
 * 
 * This testcase tests the polyline mover.
 * 
 * 
 * @author besn
 *
 */
public class TestPolyLineMover {

	private static final int kNearestEdgeDistance = 10000;
	private static final String TARGET_EPSG = "EPSG:2197";
	private boolean DEBUG_MODE = true;

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

	GeometryFactory fact = new GeometryFactory();
	private Point entryPoint;
	private static SnapTool sTool;

	static HashMap<Building, Coordinate> coordinateCache = new HashMap<Building, Coordinate>();
	static HashMap<Building, Road> roadCache = new HashMap<Building, Road>();
	private PolyLineMover plm;
	private ContextManager cm;
	private RunEnvironment re;
	private static BuildingContext buildingContext;
	private RoadContext roadContext;
	private Geography<Road> roadProjection;
	private Road road1;
	private Coordinate destinationCoordinate;
	private static HashMap<Integer, Road> roadIndex;
	private static Building building;
	private static Zone zone;

	private static Geography<IAgent> agentGeography;


	public void setup_1() {

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


		entryPoint = fact.createPoint(new Coordinate(4,-2));

		// the first polyline

		Coordinate c1 = new Coordinate(0,0);
		Coordinate c2 = new Coordinate(10,0);
		Coordinate c3 = new Coordinate(10,15);

		ArrayList<Coordinate> l1 = new ArrayList<Coordinate>();
		l1.add(c1);
		l1.add(c2);
		l1.add(c3);

		Coordinate[] coordinates = l1.toArray(new Coordinate[l1.size()]);
		LineString ls1 = fact.createLineString(coordinates);

		// the second polyline

		Coordinate c4 = new Coordinate(5,15);
		Coordinate c5 = new Coordinate(5,20);

		ArrayList<Coordinate> l2 = new ArrayList<Coordinate>();
		l2.add(c5);
		l2.add(c4);
		l2.add(c3);

		Coordinate[] coordinates2 = l2.toArray(new Coordinate[l2.size()]);
		LineString ls2 = fact.createLineString(coordinates2);


		// lets create a Building
		Building building = new Building();
		building.setIdentifier("0");
		building.setCoords(new Coordinate(4,-2));
		building.setPersons(1);

		buildingContext.add(building);

		ContextManager.buildingContext = buildingContext;


		// a zone
		Zone zone = new Zone();
		zone.setIdentifier("0");
		zone.addBuilding(building);

		// lets create a Person
		Person person = new Person(building, zone);

		Properties p = new Properties();
		p.setProperty("MinNodeEntries", "1");
		p.setProperty("MaxNodeEntries", Integer.toString(10));
		ContextManager.setRoadSpatialIndex(SpatialIndexFactory.newInstance("rtree.RTree", p));

		final HashMap<Integer, Road> roadIndex = new HashMap<Integer, Road>();


		addBuildingToCaches(building);

		// lets try to build a road 
		Road road1 = new Road();
		road1.setGeometry(ls1);
		try {
			road1.setIdentifier("1");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road 
		Road road2 = new Road();
		road2.setGeometry(ls2);
		try {
			road2.setIdentifier("2");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Junction j1 = new Junction();
		j1.setCoords(c1);

		Junction j3 = new Junction();
		j3.setCoords(c3);

		Junction j5 = new Junction();
		j5.setCoords(c5);


		road1.addJunction(j1);
		road1.addJunction(j3);

		road2.addJunction(j3);
		road2.addJunction(j5);

		roadIndex.put(1, road1);
		roadIndex.put(2, road2);

		NetworkEdge<Junction> nE1 = new NetworkEdge<Junction>(j1, j3, false, 1.0d);
		NetworkEdge<Junction> nE2 = new NetworkEdge<Junction>(j3, j5, false, 1.0d);
		road1.setEdge(nE1);
		road2.setEdge(nE2);

		nE1.setRoad(road1);
		nE2.setRoad(road2);

		j1.addRoad(road1);
		j3.addRoad(road1);
		j3.addRoad(road2);
		j5.addRoad(road2);

		ContextManager.roadIndex = roadIndex;

		GISAdder<Road> adder = new SimpleAdder<Road>();
		roadProjection.setAdder(adder);
		adder.add(roadProjection, road1);
		adder.add(roadProjection, road2);


		SimpleNearestRoadCoordinateCache sRCC = new SimpleNearestRoadCoordinateCache(ContextManager.getRoadSpatialIndex(), ContextManager.getRoadindex());
		ContextManager.setNearestRoadCoordinateCache(sRCC);

		final Geography<Building> buildingProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.BUILDING_GEOGRAPHY, buildingContext,
				new GeographyParameters<Building>(new SimpleAdder<Building>()));

		// build an Agent 
		CPHAgent agent = new CPHAgent(building, zone);

		ContextManager.setAgentContext(new AgentContext());

		agentGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.AGENT_GEOGRAPHY, ContextManager.getAgentContext(),
				new GeographyParameters<IAgent>(new SimpleAdder<IAgent>()));

		GISAdder<IAgent> agentAdder = new SimpleAdder<IAgent>();
		agentGeography.setAdder(agentAdder);
		agentAdder.add(agentGeography, agent);

		ContextManager.setAgentGeography(agentGeography);

		Coordinate firstCoordinateOnRoad = new Coordinate(4,0);
		Coordinate destinationCoordinate = new Coordinate(6,15);

		agent.setDestinationCoordinate(destinationCoordinate);

		Road cr = road1;
		agent.setCurrentRoad(cr);

		ArrayList<Junction> junctions = cr.getJunctions();

		Road r1 = new Road(cr, firstCoordinateOnRoad, junctions.get(0), "-1");
		Road r2 = new Road(cr, firstCoordinateOnRoad, junctions.get(1), "-2");

		// OK, split done perfectly

		Road newRoad = r2;

		Point newTargetPoint = fact.createPoint(newRoad.getTargetJunction().getCoords());
		Point crSourcePoint = fact.createPoint(cr.getEdge().getSource().getCoords());


		double d;
		if (DEBUG_MODE) {
			d = crSourcePoint.distance(newTargetPoint);
		} else {
			d = ContextManager.simpleDistance.distance(crSourcePoint, newTargetPoint);
		}

		if (d == 0.0d) {
			agent.setTargetJunction(cr.getEdge().getSource());
		} else {
			agent.setTargetJunction(cr.getEdge().getTarget());
		}

		ContextManager.moveAgent(agent, fact.createPoint(firstCoordinateOnRoad));

		// double x = ContextManager.roadProjection.getGeometry(currentRoad).distance(fact.createPoint(currentCoord));

		agent.setNewborn(false);

		this.plm = new PolyLineMover(agent, cr, agent.getTargetJunction());

		double stepLength = ContextManager.getStepLength(); // in seconds
		double speed = agent.getSpeed(); //in m/s

		double distance = speed * stepLength;

		distance = 5.0d;

		boolean GO_GO = true;	

		//		plm.logToPostgres();

		while (GO_GO) {
			OvershootData overshoot = plm.move(distance);
			if (agent.getPosition().distance(destinationCoordinate) < 3) {
				GO_GO = false;
				plm.terminate(destinationCoordinate);
			} else {
				if (overshoot != null) {
					this.plm = new PolyLineMover(agent, overshoot.getRoad(), overshoot.getTargetJunction());
				}
			}

		}


	}

	public void setupBase() {
		cm = new ContextManager();
		re = RunEnvironment.getInstance();
		sTool = ContextManager.getSnapTool();
		buildingContext = new BuildingContext();
		roadContext = new RoadContext();

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


		roadProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY, roadContext,
				new GeographyParameters<Road>(new SimpleAdder<Road>()));

		ContextManager.roadProjection = roadProjection;
		ContextManager.roadContext = roadContext;

		CoordinateReferenceSystem crs = ContextManager.roadProjection.getCRS();

		ContextManager.setCrs(crs);

		ContextManager.simpleDistance = new SimpleDistance(ContextManager.getCrs(), TARGET_EPSG);

	}

	public void setEntryPoint(double x, double y) {
		entryPoint = fact.createPoint(new Coordinate(x,y));
	}

	public static void setBuildingAndZone(double x, double y) {
		// lets create a Building
		building = new Building();
		building.setIdentifier("0");
		building.setCoords(new Coordinate(x,y));
		building.setPersons(1);

		buildingContext.add(building);

		ContextManager.buildingContext = buildingContext;


		// a zone
		zone = new Zone();
		zone.setIdentifier("0");
		zone.addBuilding(building);

		// lets create a Person
		Person person = new Person(building, zone);

		Properties p = new Properties();
		p.setProperty("MinNodeEntries", "1");
		p.setProperty("MaxNodeEntries", Integer.toString(10));
		ContextManager.setRoadSpatialIndex(SpatialIndexFactory.newInstance("rtree.RTree", p));

		roadIndex = new HashMap<Integer, Road>();


		addBuildingToCaches(building);

	}


	public void setup_2() {

		setEntryPoint(4,-2);

		// the first polyline

		Coordinate c1 = new Coordinate(0,0);
		Coordinate c2 = new Coordinate(10,0);


		ArrayList<Coordinate> l1 = new ArrayList<Coordinate>();
		l1.add(c1);
		l1.add(c2);

		Coordinate[] coordinates = l1.toArray(new Coordinate[l1.size()]);
		LineString ls1 = fact.createLineString(coordinates);

		// the second polyline

		Coordinate c3 = new Coordinate(10,2);

		ArrayList<Coordinate> l2 = new ArrayList<Coordinate>();
		l2.add(c2);
		l2.add(c3);

		Coordinate[] coordinates2 = l2.toArray(new Coordinate[l2.size()]);
		LineString ls2 = fact.createLineString(coordinates2);

		// pl 4
		Coordinate c4 = new Coordinate(10,3);

		ArrayList<Coordinate> l3 = new ArrayList<Coordinate>();
		l3.add(c3);
		l3.add(c4);

		Coordinate[] coordinates3 = l3.toArray(new Coordinate[l3.size()]);
		LineString ls3 = fact.createLineString(coordinates3);


		Coordinate c5 = new Coordinate(10,4);

		ArrayList<Coordinate> l4 = new ArrayList<Coordinate>();
		l4.add(c4);
		l4.add(c5);

		Coordinate[] coordinates4 = l4.toArray(new Coordinate[l4.size()]);
		LineString ls4 = fact.createLineString(coordinates4);

		// pl5
		Coordinate c6 = new Coordinate(10,15);

		ArrayList<Coordinate> l5 = new ArrayList<Coordinate>();
		l5.add(c6);
		l5.add(c5);

		Coordinate[] coordinates5 = l5.toArray(new Coordinate[l5.size()]);
		LineString ls5 = fact.createLineString(coordinates5);

		//pl 6

		Coordinate c7 = new Coordinate(5,15);
		Coordinate c8 = new Coordinate(5,20);

		ArrayList<Coordinate> l6 = new ArrayList<Coordinate>();
		l6.add(c6);
		l6.add(c7);
		l6.add(c8);

		Coordinate[] coordinates6 = l6.toArray(new Coordinate[l6.size()]);
		LineString ls6 = fact.createLineString(coordinates6);

		// lets try to build a road  1
		road1 = new Road();
		road1.setGeometry(ls1);
		try {
			road1.setIdentifier("1");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road  2
		Road road2 = new Road();
		road2.setGeometry(ls2);
		try {
			road2.setIdentifier("2");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road 
		Road road3 = new Road();
		road3.setGeometry(ls3);
		try {
			road3.setIdentifier("3");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road 
		Road road4 = new Road();
		road4.setGeometry(ls4);
		try {
			road4.setIdentifier("4");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road 
		Road road5 = new Road();
		road5.setGeometry(ls5);
		try {
			road5.setIdentifier("5");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road 
		Road road6 = new Road();
		road6.setGeometry(ls6);
		try {
			road6.setIdentifier("6");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		Junction j1 = new Junction();
		j1.setCoords(c1);

		Junction j2 = new Junction();
		j2.setCoords(c2);

		Junction j3 = new Junction();
		j3.setCoords(c3);

		Junction j4 = new Junction();
		j4.setCoords(c4);

		Junction j5 = new Junction();
		j5.setCoords(c5);

		Junction j6 = new Junction();
		j6.setCoords(c6);

		Junction j8 = new Junction();
		j8.setCoords(c8);

		road1.addJunction(j1);
		road1.addJunction(j2);

		road2.addJunction(j2);
		road2.addJunction(j3);

		road3.addJunction(j3);
		road3.addJunction(j4);

		road4.addJunction(j4);
		road4.addJunction(j5);

		road5.addJunction(j5);
		road5.addJunction(j6);

		road6.addJunction(j6);
		road6.addJunction(j8);		


		roadIndex.put(1, road1);
		roadIndex.put(2, road2);
		roadIndex.put(3, road3);
		roadIndex.put(4, road4);
		roadIndex.put(5, road5);
		roadIndex.put(6, road6);


		NetworkEdge<Junction> nE1 = new NetworkEdge<Junction>(j1, j2, false, 1.0d);
		NetworkEdge<Junction> nE2 = new NetworkEdge<Junction>(j2, j3, false, 1.0d);
		NetworkEdge<Junction> nE3 = new NetworkEdge<Junction>(j3, j4, false, 1.0d);
		NetworkEdge<Junction> nE4 = new NetworkEdge<Junction>(j4, j5, false, 1.0d);
		NetworkEdge<Junction> nE5 = new NetworkEdge<Junction>(j5, j6, false, 1.0d);
		NetworkEdge<Junction> nE6 = new NetworkEdge<Junction>(j6, j8, false, 1.0d);


		road1.setEdge(nE1);
		road2.setEdge(nE2);
		road3.setEdge(nE3);
		road4.setEdge(nE4);
		road5.setEdge(nE5);
		road6.setEdge(nE6);

		nE1.setRoad(road1);
		nE2.setRoad(road2);
		nE3.setRoad(road3);
		nE4.setRoad(road4);
		nE5.setRoad(road5);
		nE6.setRoad(road6);

		j1.addRoad(road1);

		j2.addRoad(road1);
		j2.addRoad(road2);

		j3.addRoad(road2);
		j3.addRoad(road3);

		j4.addRoad(road3);
		j4.addRoad(road4);

		j5.addRoad(road4);
		j5.addRoad(road5);

		j6.addRoad(road5);
		j6.addRoad(road6);

		j8.addRoad(road6);

		ContextManager.roadIndex = roadIndex;

		GISAdder<Road> adder = new SimpleAdder<Road>();
		roadProjection.setAdder(adder);
		adder.add(roadProjection, road1);
		adder.add(roadProjection, road2);
		adder.add(roadProjection, road3);
		adder.add(roadProjection, road4);
		adder.add(roadProjection, road5);
		adder.add(roadProjection, road6);

	}

	/**
	 * @param x : x coordinate of destination 
	 * @param y : y coordinate of destination
	 */
	public void setup_3(double x, double y) {

		this.setDestinationCoordinate(x, y);
		
		setEntryPoint(4,-2);

		Coordinate c1 = new Coordinate(0,0);
		Coordinate c2 = new Coordinate(3,0);
		Coordinate c3 = new Coordinate(6,0);
		Coordinate c4 = new Coordinate(9,0);

		Coordinate c5 = new Coordinate(9,10);

		ArrayList<Coordinate> l1 = new ArrayList<Coordinate>();
		l1.add(c1);
		l1.add(c2);
		l1.add(c3);
		l1.add(c4);

		ArrayList<Coordinate> l2 = new ArrayList<Coordinate>();
		l2.add(c4);
		l2.add(c5);

		Coordinate[] coordinates1 = l1.toArray(new Coordinate[l1.size()]);
		LineString ls1 = fact.createLineString(coordinates1);

		Coordinate[] coordinates2 = l2.toArray(new Coordinate[l2.size()]);
		LineString ls2 = fact.createLineString(coordinates2);

		// lets try to build a road  1
		road1 = new Road();
		road1.setGeometry(ls1);
		try {
			road1.setIdentifier("1");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets try to build a road  2

		Road road2 = new Road();
		road2.setGeometry(ls2);
		try {
			road2.setIdentifier("2");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Junction j1 = new Junction();
		j1.setCoords(c1);

		Junction j4 = new Junction();
		j4.setCoords(c4);

		Junction j5 = new Junction();
		j5.setCoords(c5);


		road1.addJunction(j1);
		road1.addJunction(j4);

		road2.addJunction(j4);
		road2.addJunction(j5);

		roadIndex.put(1, road1);
		roadIndex.put(2, road2);

		NetworkEdge<Junction> nE1 = new NetworkEdge<Junction>(j1, j4, false, 1.0d);
		NetworkEdge<Junction> nE2 = new NetworkEdge<Junction>(j4, j5, false, 1.0d);

		road1.setEdge(nE1);
		road2.setEdge(nE2);

		nE1.setRoad(road1);
		nE2.setRoad(road2);

		j1.addRoad(road1);
		j4.addRoad(road1);

		j4.addRoad(road2);
		j5.addRoad(road2);

		ContextManager.roadIndex = roadIndex;

		GISAdder<Road> adder = new SimpleAdder<Road>();
		roadProjection.setAdder(adder);
		adder.add(roadProjection, road1);
		adder.add(roadProjection, road2);

	}

	private void setDestinationCoordinate(double x, double y) {

		destinationCoordinate = new Coordinate(x,y);

	}

	public void simulate() {

		SimpleNearestRoadCoordinateCache sRCC = new SimpleNearestRoadCoordinateCache(ContextManager.getRoadSpatialIndex(), ContextManager.getRoadindex());
		ContextManager.setNearestRoadCoordinateCache(sRCC);

		final Geography<Building> buildingProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.BUILDING_GEOGRAPHY, buildingContext,
				new GeographyParameters<Building>(new SimpleAdder<Building>()));

		// build an Agent 
		CPHAgent agent = new CPHAgent(building, zone);

		ContextManager.setAgentContext(new AgentContext());

		agentGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.AGENT_GEOGRAPHY, ContextManager.getAgentContext(),
				new GeographyParameters<IAgent>(new SimpleAdder<IAgent>()));

		GISAdder<IAgent> agentAdder = new SimpleAdder<IAgent>();
		agentGeography.setAdder(agentAdder);
		agentAdder.add(agentGeography, agent);

		ContextManager.setAgentGeography(agentGeography);

		// get the agent on the polyline

		Coordinate firstCoordinateOnRoad = new Coordinate(4,0);


		agent.setDestinationCoordinate(destinationCoordinate);

		Road cr = road1;
		agent.setCurrentRoad(cr);

		ArrayList<Junction> junctions = cr.getJunctions();

		Road r1 = new Road(cr, firstCoordinateOnRoad, junctions.get(0), "-1");
		Road r2 = new Road(cr, firstCoordinateOnRoad, junctions.get(1), "-2");

		//		System.out.println(r1.getGeometry());
		//		System.out.println(r2.getGeometry());

		// OK, split done perfectly

		Road newRoad = r2;

		Point newTargetPoint = fact.createPoint(newRoad.getTargetJunction().getCoords());
		Point crSourcePoint = fact.createPoint(cr.getEdge().getSource().getCoords());


		double d;
		if (DEBUG_MODE) {
			d = crSourcePoint.distance(newTargetPoint);
		} else {
			d = ContextManager.simpleDistance.distance(crSourcePoint, newTargetPoint);
		}

		if (d == 0.0d) {
			agent.setTargetJunction(cr.getEdge().getSource());
		} else {
			agent.setTargetJunction(cr.getEdge().getTarget());
		}

		ContextManager.moveAgent(agent, fact.createPoint(firstCoordinateOnRoad));

		// double x = ContextManager.roadProjection.getGeometry(currentRoad).distance(fact.createPoint(currentCoord));

		agent.setNewborn(false);

		this.plm = new PolyLineMover(agent, cr, agent.getTargetJunction());

		double stepLength = ContextManager.getStepLength(); // in seconds
		double speed = agent.getSpeed(); //in m/s

		double distance = speed * stepLength;

		distance = 5.0d;

		boolean GO_GO = true;	

		//		System.out.println(agent.getPosition());

		//		plm.logToPostgres();

		while (GO_GO) {
			OvershootData overshoot = plm.move(distance);
			if (agent.getPosition().distance(destinationCoordinate) < 3) {
				GO_GO = false;
				plm.terminate(destinationCoordinate);
			}
			if (overshoot != null) {
				this.plm = new PolyLineMover(agent, overshoot.getRoad(), overshoot.getTargetJunction());
			}

		}

		// now locate the agent at the destination coordinate
		// TODO: log


	}

	public static  void addBuildingToCaches(Building b) {
		// 1. we need a jsi Point to perform the spatial query
		Point buildingCentroid = b.getCentroid();
		com.infomatiq.jsi.Point pp = new com.infomatiq.jsi.Point((float) buildingCentroid.getX(), (float) buildingCentroid.getY());
		ReturnArray r = new TestPolyLineMover().new ReturnArray();

		SpatialIndex rSi = ContextManager.getRoadSpatialIndex();
		rSi.nearestNUnsorted(pp, r, 8, kNearestEdgeDistance);
		//					System.out.println(r.getResult().size() + " edges found within " + kNearestEdgeDistance + "m.");

		double distance = Double.MAX_VALUE;
		Coordinate cc = null;
		Road rr = null;

		for (Integer i : r.getResult()) {
			Road road = ContextManager.roadIndex.get(i);
			Coordinate c = sTool.getProjectedPointOnPolyline(road.getGeometry(),b.getCentroid().getCoordinate());
			double dist  = c.distance(b.getCoords());
			if (dist<distance) {
				distance = dist;
				cc = c;
				rr = road;
			}
		}
		coordinateCache.put(b, cc);
		roadCache.put(b, rr);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestPolyLineMover tPlm = new TestPolyLineMover();

		tPlm.setupBase();

		// tPlm.setDestinationCoordinate(6,15);

		tPlm.setBuildingAndZone(4,-2);

		//		tPlm.setup_1();

		//		tPlm.setup_2();

		tPlm.setup_3(9,10);

		tPlm.simulate();

	}

}
