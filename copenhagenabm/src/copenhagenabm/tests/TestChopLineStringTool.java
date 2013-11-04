package copenhagenabm.tests;

import java.util.ArrayList;
import java.util.Properties;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.main.ContextManager;
import copenhagenabm.tools.ChopLineStringTool;

public class TestChopLineStringTool {

	GeometryFactory fact = new GeometryFactory();
	
	private Coordinate c1;
	private Coordinate c2;
	private Coordinate c3;

	private LineString ls1;

	private Coordinate c4;

	void setUp() {
		
		ContextManager cm = new ContextManager();
		
		Properties p = new Properties();
		p.setProperty("MinNodeEntries", "1");
		p.setProperty("MaxNodeEntries", Integer.toString(10));
		
		ContextManager.setProperties(p);
		
		c1 = new Coordinate(0,0);
		c2 = new Coordinate(10,0);
		c3 = new Coordinate(10,15);
		c4 = new Coordinate(15,15);
		
		ArrayList<Coordinate> l1 = new ArrayList<Coordinate>();
		l1.add(c1);
		l1.add(c2);
		l1.add(c3);
		l1.add(c4);

		Coordinate[] coordinates = l1.toArray(new Coordinate[l1.size()]);
		ls1 = fact.createLineString(coordinates);
		
	}
	
	public LineString chopOnLine(Coordinate coordinate) {
		ChopLineStringTool cLST = new ChopLineStringTool(ls1);
		LineString result = cLST.chop(coordinate, c4);
		return result;
	}
	
	public LineString chopOffLine(Coordinate coordinate) {
		ChopLineStringTool cLST = new ChopLineStringTool(ls1);
		LineString result = cLST.chop(coordinate, c4);
		return result;
	}
	
	public static void main(String[] args) {
		
		TestChopLineStringTool tCLST = new TestChopLineStringTool();
		tCLST.setUp();
		
		// chop 
		System.out.println(tCLST.chopOnLine(new Coordinate(10,10)).toString().equals("LINESTRING (10 10, 10 15, 15 15)"));
		System.out.println(tCLST.chopOffLine(new Coordinate(11,10)).toString().equals("LINESTRING (10 10, 10 15, 15 15)"));
		
		// chopping off at the last node of the polyline, should return null
		System.out.println(tCLST.chopOnLine(new Coordinate(15,15)) == null);
		
		// chopping off at the first node of the polyline, should return the whole polyline
		System.out.println(tCLST.chopOnLine(new Coordinate(0,0)).equals("LINESTRING (0 0, 10 0, 10 15, 15 15)"));
	
		// choopping off at the first vertex inside the polyline
		System.out.println(tCLST.chopOnLine(new Coordinate(10,0)).equals("LINESTRING (10 0, 10 0, 10 15, 15 15)"));							// LINESTRING (10 0, 10 0, 10 15, 15 15)
	}

	
}
