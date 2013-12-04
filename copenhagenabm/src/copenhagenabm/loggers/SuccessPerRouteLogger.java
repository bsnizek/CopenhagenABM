package copenhagenabm.loggers;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.agent.IAgent;
import copenhagenabm.main.CalibrationModeData;
import copenhagenabm.main.CalibrationModeData.CalibrationRoute;
import copenhagenabm.main.ContextManager;
import copenhagenabm.routes.MatchedGPSRoute;

/**
 * SuccessPerRouteLogger
 * 
 * Logs the number of success per route 
 * 
 * <route_i>,<number_of_iterations>,<number_of_successes>
 * 
 * @author besn
 *
 */
public class SuccessPerRouteLogger {

	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}

//		public String getTail(Handler h) {
//			return "SUM:" + this.getTotalNumberOfIterations() + ";" + this.getSuccessfulRoutes();
//		}

		private int getSuccessfulRoutes() {
			// TODO Auto-generated method stub
			return 0;
		}

		private int getTotalNumberOfIterations() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	private final static Logger LOGGER = Logger.getLogger(SuccessPerRouteLogger.class.getName());
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;

	public void setup() throws IOException {
		LOGGER.setLevel(Level.INFO);
		String ctf = ContextManager.getSuccessPerRouteLoggerFile();
		fileTxt = new FileHandler(ctf, true);
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
	}

	public String getHead(Handler h) {
		return "route_id;number_of_repetitions;number_of_successful_routes\n";
	}

	public void log() {

		Iterable<IAgent> allAgents = ContextManager.getDeadAgents();

		HashMap<Integer, Integer> GPSRouteIDs = new HashMap<Integer, Integer>();

		for (IAgent agent : allAgents) {

			CalibrationRoute cR = agent.getCalibrationRoute();
			int GPSRouteID = cR.getGPSRouteID();

			if (GPSRouteIDs.containsKey(GPSRouteID)) {

				GPSRouteIDs.put(GPSRouteID, GPSRouteIDs.get(GPSRouteID) +1 );

			} else {
				GPSRouteIDs.put(GPSRouteID, 1);
			}
		}

		Iterable<MatchedGPSRoute> mGPSR = ContextManager.getMatchedGPSRoutes();

		for (MatchedGPSRoute r : mGPSR) {
			int objID = r.getOBJECTID();
			Integer xx = GPSRouteIDs.get(objID);
			if (xx==null) 
				LOGGER.info(objID + ";" + ContextManager.getNumberOfRepetitions() + ";" + 0);
			else 
				LOGGER.info(objID + ";" + ContextManager.getNumberOfRepetitions() + ";" + xx);
		}

	}

	public void close() {
		fileTxt.close();
	}


}
