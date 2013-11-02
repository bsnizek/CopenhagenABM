package copenhagenabm.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.main.ContextManager;

public class CopenhagenABMTools {


	DateFormat formatter = new SimpleDateFormat("HH:mm");

	public SnapTool getSnapTool() {
		return ContextManager.getSnapTool();
	}
	
	/*
	 * returns a random tick within the given interval
	 * ticks need 
	 * 
	 * @tFrom : the lower bound of the interval, string, like 00:00
	 * @tTo : the upper bound of the interval, string, like 01:00
	 * @tickSize : the length of the tick in ms
	 */

	public long getRandomTick(String tFrom, String tTo, int tickSize) throws ParseException {

		Date tFromString = formatter.parse(tFrom);
		Date tToString = formatter.parse(tTo);
		
		long b = (tFromString.getTime() + 3600000) / 1000;
		long t = (tToString.getTime() + 3600000) / 1000;

		long bottom = b / tickSize;
		long top = t / tickSize; 
		return new Random().nextInt((int) (top-bottom)) + bottom;
	}

}
