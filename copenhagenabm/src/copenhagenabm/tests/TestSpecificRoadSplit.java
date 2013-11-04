package copenhagenabm.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repastcity3.environment.Junction;
import repastcity3.environment.contexts.RoadContext;
import repastcity3.exceptions.DuplicateRoadException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.tools.SimpleDistance;

public class TestSpecificRoadSplit {

	private static final String TARGET_EPSG = "EPSG:2197";

	GeometryFactory fact = new GeometryFactory();

	private Coordinate c1;
	private Coordinate c2;
	private Junction j1;
	private Junction j2;

	private Road cr;

	private Coordinate firstCoordinateOnRoad;

	void setUpSystem() {

		ContextManager cm = new ContextManager();

		Properties p = new Properties();
		p.setProperty("MinNodeEntries", "1");
		p.setProperty("MaxNodeEntries", Integer.toString(10));

		cm.setProperties(p);

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

		Geography<Road> roadProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY, roadContext,
				new GeographyParameters<Road>(new SimpleAdder<Road>()));

		ContextManager.roadProjection = roadProjection;
		ContextManager.roadContext = roadContext;

		CoordinateReferenceSystem crs = ContextManager.roadProjection.getCRS();

		ContextManager.setCrs(crs);

		ContextManager.simpleDistance = new SimpleDistance(ContextManager.getCrs(), TARGET_EPSG);

	}

	public void setUp() {

		c1 = new Coordinate(12.576621500282045,55.702131300019516);
		c2 = new Coordinate(12.576983200414418,55.702296899382475);
		
		// Junction 28273 (12.576621500282045,55.702131300019516)
		// Junction 28312 (12.576983200414418,55.702296899382475)

		j1 = new Junction();
		j1.setCoords(c1);

		j2 = new Junction();
		j2.setCoords(c1);


		ArrayList<Coordinate> l1 = new ArrayList<Coordinate>();
		l1.add(c1);
		l1.add(c2);

		Coordinate[] coordinates = l1.toArray(new Coordinate[l1.size()]);
		LineString ls1 = fact.createLineString(coordinates);

		cr = new Road();
		cr.setGeometry(ls1);
		try {
			cr.setIdentifier("1");
		} catch (DuplicateRoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cr.addJunction(j1);
		cr.addJunction(j2);

		firstCoordinateOnRoad = new Coordinate(12.576983200414418, 55.702296899382475);

	}


	public static void main(String[] args) {
		TestSpecificRoadSplit tSRS = new TestSpecificRoadSplit();
		tSRS.setUpSystem();
		tSRS.setUp();
		tSRS.split();
	}


	private void split() {
		ArrayList<Junction> junctions = cr.getJunctions();

		Road r1 = new Road(cr, firstCoordinateOnRoad, junctions.get(0), "-1");
		Road r2 = new Road(cr, firstCoordinateOnRoad, junctions.get(1), "-2");

		if (r1.getGeometry() != null) {
			System.out.println(r1.getGeometry());
		}

		if (r2.getGeometry() != null) {
			System.out.println(r2.getGeometry());
		}


	}

}
