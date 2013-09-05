package copenhagenabm.exceptions;

import com.vividsolutions.jts.geom.Point;

/*
© 2012 Bernhard Snizek <b@snizek.com>
This file is part of CopenhagenABM.

CopenhagenABM is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
*/

public class MoveException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private double overshoot = 0.0d;
	private Point point;

	public MoveException(String message, double overShootLength, Point point) {
		super(message);
		this.overshoot = overShootLength;
		this.setPoint(point);
	}

	public double getOvershoot() {
		return overshoot;
	}

	public void setOvershoot(double overshoot) {
		this.overshoot = overshoot;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

}
