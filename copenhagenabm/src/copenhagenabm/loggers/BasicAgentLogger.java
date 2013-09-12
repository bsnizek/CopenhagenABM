package copenhagenabm.loggers;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.main.ContextManager;


/**
 * 
 * The BasicAgentLogger logs birth tick, death tick, birth edge ID, death edge ID
 * 
 * @author Bernhard Snizek
 *
 */

public class BasicAgentLogger {
	
	class SimpleTextFormatter extends Formatter {
		
		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}
		
		public String getHeader(Handler h) {
			return "agentID;birthTick;deathTick;birthZone;deathZone;birthCoord;DeathCoord";
		}
	}
	
	private final static Logger LOGGER = Logger.getLogger(BasicAgentLogger.class.getName());
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;
	
	public void setup() throws IOException {
		LOGGER.setLevel(Level.INFO);
		fileTxt = new FileHandler("log/" + ContextManager.getBasicAgentLoggerFileName(), true);
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
	}

	
	public void doLog(int agentID, int birthTick, int deathTick, String birthZone, String deathZone, Coordinate fromCoordinate, Coordinate toCoordinate) {
		
		String fromCoordinateString = fromCoordinate.x + "," + fromCoordinate.y;
		String toCoordinateString = toCoordinate.x + "," + toCoordinate.y;
		
		LOGGER.info(agentID + ";" + birthTick + ";" + deathTick + ";" + birthZone + ";" + deathZone + ";" + fromCoordinateString + ";" + toCoordinateString);
		
	}

		

}
