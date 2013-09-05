package copenhagenabm.tools;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.geometry.DirectPosition2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class RasterSpace {
	
	private GridCoverage2D coverage;

	public RasterSpace(String fileName) throws IllegalArgumentException, IOException {
		File f = new File(fileName);
		ArcGridReader reader = new ArcGridReader(f);
		coverage = (GridCoverage2D) reader.read(null);
	}
	
	public float valueAt(DirectPosition2D pos) {
		Object x = coverage.evaluate(pos);
		float[] o = (float[]) x;
		return o[0];
	}
	
	public float valueAt(float x, float y) {
		return this.valueAt(new DirectPosition2D(x,y));
	}
	
	public float valueAt(Coordinate c) {
		return this.valueAt(new DirectPosition2D(c.x, c.y));
	}
	
	public float valueAt(Point p) {
		return this.valueAt(new DirectPosition2D(p.getX(), p.getY()));
	}

}
