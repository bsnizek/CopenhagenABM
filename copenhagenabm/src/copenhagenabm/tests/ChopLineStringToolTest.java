package copenhagenabm.tests;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.tools.ChopLineStringTool;

public class ChopLineStringToolTest {
	
	GeometryFactory fact = new GeometryFactory();
	LineString linestring = null;
	ChopLineStringTool cLST = null;
	
	public ChopLineStringToolTest() {
		
	}

	private void test10_5__0_10() {
		
		Coordinate[] coordinates = {
				new Coordinate(0d,0d),
				new Coordinate(10d,0d),
				new Coordinate(10d,10d),
				new Coordinate(0d,10d),
				};
		
		Coordinate splitPoint = new Coordinate(10,5);
		Coordinate endCoord = new Coordinate(0,10);
		
		linestring = fact.createLineString(coordinates);
		
		cLST = new ChopLineStringTool(linestring);
		
		LineString splittedLineString = cLST.chop(splitPoint, endCoord);
		
		for (Coordinate c : splittedLineString.getCoordinates()) {
			System.out.println(c);
		}
		
		
	}
	
	private void test10_5__0_0() {
		
		Coordinate[] coordinates = {
				new Coordinate(0d,0d),
				new Coordinate(10d,0d),
				new Coordinate(10d,10d),
				new Coordinate(0d,10d),
				};
		
		Coordinate splitPoint = new Coordinate(10,5);
		Coordinate endCoord = new Coordinate(0,0);
		
		linestring = fact.createLineString(coordinates);
		
		cLST = new ChopLineStringTool(linestring);
		
		LineString splittedLineString = cLST.chop(splitPoint, endCoord);
		
		for (Coordinate c : splittedLineString.getCoordinates()) {
			System.out.println(c);
		}
		
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ChopLineStringToolTest slt = new ChopLineStringToolTest();
		slt.test10_5__0_10();
//		slt.test10_5__0_0();
		
	}




}
