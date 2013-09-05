package copenhagenabm.agent;

import com.vividsolutions.jts.geom.Point;

public class OvershootData {

	private double d;
	private Point endPoint;

	public OvershootData(double d, Point endPoint) {
		this.setOvershoot(d);
		this.setPoint(endPoint);
	}

	public double getOvershoot() {
		return d;
	}

	public void setOvershoot(double d) {
		this.d = d;
	}

	public Point getPoint() {
		return endPoint;
	}

	public void setPoint(Point endPoint) {
		this.endPoint = endPoint;
	}

}
