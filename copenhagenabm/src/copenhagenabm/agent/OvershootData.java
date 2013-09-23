package copenhagenabm.agent;

import repastcity3.environment.Junction;

import copenhagenabm.environment.Road;

public class OvershootData {
	
	private Junction targetJunction;
	private double nextPosition;

	public OvershootData(Road road, Junction targetJunction, double nextPosition) {
		this.road = road;
		this.targetJunction = targetJunction;
		this.nextPosition = nextPosition;
	}

	private Road road;
	public Road getRoad() {
		return road;
	}

	public void setRoad(Road road) {
		this.road = road;
	}

	public Junction getTargetJunction() {
		return targetJunction;
	}

	public void setTargetJunction(Junction targetJunction) {
		this.targetJunction = targetJunction;
	}

	public double getNextPosition() {
		return nextPosition;
	}

	public void setNextPosition(double nextPosition) {
		this.nextPosition = nextPosition;
	}



}
