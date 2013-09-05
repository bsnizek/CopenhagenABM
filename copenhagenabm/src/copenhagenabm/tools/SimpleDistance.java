package copenhagenabm.tools;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import com.vividsolutions.jts.geom.GeometryFactory;


/*
 * 
 * A class for simple distance calculation given a crs (in) and an epsg (out)
 * 
 * Read more here: http://docs.geotools.org/latest/userguide/library/api/jts.html
 */
public class SimpleDistance {
	
	private CoordinateReferenceSystem crs;
	private CoordinateReferenceSystem targetCRS;
	private MathTransform transform;
	private GeometryFactory fact = new GeometryFactory();

	public SimpleDistance(CoordinateReferenceSystem crs, String epsg) {
		this.crs = crs;
		try {
			targetCRS = CRS.decode(epsg);
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			transform = CRS.findMathTransform(crs, targetCRS);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * returns the distance between two coordinates
	 */
	public double distance(Coordinate c1, Coordinate c2) {
		Point p1 = fact.createPoint(c1);
		Point p2 = fact.createPoint(c2);
		return distance(p1, p2);
	}
	
	/*
	 * retruns the distance between two points
	 */
	public double distance(Point p1, Point p2) {
		Geometry targetGeometry0 = null;
		Geometry targetGeometry1 = null;
		try {
			targetGeometry0 = JTS.transform( p1, transform);
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			targetGeometry1 = JTS.transform( p2, transform);
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return targetGeometry0.distance(targetGeometry1);
		
	}
	/**
	 * Returns the length of the geometry g
	 * @param g
	 * @return
	 */
	public double geometryLengthInMetres(Geometry g) {
		Geometry targetGeometry0 = null;
		try {
			targetGeometry0 = JTS.transform( g, transform);
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return targetGeometry0.getLength();
	}

}
