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
	
	private void test_001() {
		
		Coordinate[] coordinates = {
				new Coordinate(12.43145940955113, 55.758040634369536),
				new Coordinate(12.431306700425377, 55.75802509864386),
				new Coordinate(12.43145940955113, 55.758040634369536),
				new Coordinate(12.431765130487896, 55.75807173455766)
		};
	
		Coordinate splitPoint = new Coordinate(12.431765130487896, 55.75807173455766);
		Coordinate endCoord = new Coordinate(0,10);
		
		
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
			System.out.print((int) c.x + "/" + (int) c.y + " -> ");
		}
		
		System.out.println();
		
		System.out.println("10/5 -> 10/10 -> 0/10");
		System.out.println("=====================");
		
		
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
