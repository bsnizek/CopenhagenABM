package com.copenhagenabm.playground;

public class Test {	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Test t = new Test();
		
		Point currPoint = new Point(0,0);
		Point destinationPoint = new Point(10,10);
		Point farPoint = new Point(-10,10);
		
		Line lineToDestination = new Line(currPoint, destinationPoint);
		Line nextEdge = new Line(currPoint, farPoint);
		
		System.out.println(t.getAngle(lineToDestination, nextEdge));
		
	}
	
	public double getAngle(Line lineToDestination, Line nextEdge) {
		
		double ux = lineToDestination.to.x - lineToDestination.from.x;
		double uy = lineToDestination.to.y - lineToDestination.from.y;
		
		double vx = nextEdge.to.x - nextEdge.from.x;
		double vy = nextEdge.to.y - nextEdge.from.y;
		
		
		double u = ux * vx + uy * vy;
		double l = Math.sqrt(ux*ux + uy*uy) * Math.sqrt(vx*vx + vy*vy);
		
		return 1- (Math.toDegrees(Math.acos(u/l))) / 180;
		
	}

}
