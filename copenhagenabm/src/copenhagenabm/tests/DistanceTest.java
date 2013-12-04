package copenhagenabm.tests;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.tools.geodetic.GeodeticCalculator;

import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repastcity3.environment.GISFunctions;

public class DistanceTest {

	private static PointContext pointContext;
	static Geography<PPoint> pointProjection;


	void loadPointContext() throws MalformedURLException, FileNotFoundException {
		pointContext = new PointContext();
		pointProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				Vars.CONTEXT_NAMES.POINT_GEOGRAPHY, pointContext,
				new GeographyParameters<PPoint>(new SimpleAdder<PPoint>()));
		String pointFile = "src/copenhagenabm/tests/data/point.shp";
		GISFunctions.readShapefile(PPoint.class, pointFile, pointProjection, pointContext);
	}
	
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException, FileNotFoundException {
		DistanceTest dt = new DistanceTest();
		dt.loadPointContext();
		try {
			try {
				dt.getPoints();
			} catch (NoSuchAuthorityCodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FactoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static synchronized Iterable<PPoint> getAllPoints() {
		return pointContext.getRandomObjects(PPoint.class, pointContext.size());
	}
	

	private void getPoints() throws TransformException, NoSuchAuthorityCodeException, FactoryException {
		ArrayList<PPoint> ppoints = new ArrayList<PPoint>();
		for (PPoint point: getAllPoints()) {
			System.out.println(point.getCoords());
			System.out.println(point.getIdentifier());
			ppoints.add(point);
		}
		
		System.out.println(ppoints.get(0).getCoords().distance(ppoints.get(1).getCoords()));
		
		GeodeticCalculator calculator = new GeodeticCalculator();
		calculator.setStartingGeographicPoint(ppoints.get(0).getCoords().x, ppoints.get(0).getCoords().y);
		calculator.setDestinationGeographicPoint(ppoints.get(1).getCoords().x, ppoints.get(1).getCoords().y);
		
		double orthodromic = calculator.getOrthodromicDistance();
		
		System.out.println("orthodromic: " + orthodromic);
		
		// System.out.println( JTS.orthodromicDistance(ppoints.get(0).getCoords(), ppoints.get(1).getCoords(), pointProjection.getCRS()));
		
		System.out.println(pointProjection.getCRS().getCoordinateSystem().getIdentifiers());
		
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:23032"); //23032
		
		MathTransform transform = CRS.findMathTransform(pointProjection.getCRS(), targetCRS);
		Geometry targetGeometry0 = JTS.transform( ppoints.get(0).getGeometry(), transform);
		Geometry targetGeometry1 = JTS.transform( ppoints.get(1).getGeometry(), transform);
		
		float x = (float) targetGeometry0.distance(targetGeometry1);
		
		System.out.println(x);
		
		System.out.println("DIFF: " + String.valueOf(orthodromic-x));

	}

	// 2197		0.005873446311937869
	// 23032	0.03598141208649963
	

}
