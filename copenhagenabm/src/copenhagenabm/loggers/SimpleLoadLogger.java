package copenhagenabm.loggers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.main.ContextManager;

public class SimpleLoadLogger {
	
	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}
		
		public String getHeader(Handler h) {
			return "agentID;gpsRouteID;pathSize";
		}
		
//		public String getTail(Handler h) {
//			return "tail";
//		}
		
	}
	
	HashMap<String, Integer> loads = new HashMap<String, Integer>();
	
	private final static Logger LOGGER = Logger.getLogger(SimpleLoadLogger.class.getName());
	
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;
	
	public void addVisitedToRoad(String roadID, int numberOfAgents) {
		if (loads.containsKey(roadID)) {
			loads.put(roadID, loads.get(roadID) + numberOfAgents);
		} else {
			loads.put(roadID, numberOfAgents);
		}
	}
	
	public void setup() throws IOException {
		LOGGER.setLevel(Level.INFO);
		fileTxt = new FileHandler(ContextManager.getSimpleLoadLoggerFile());
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
	}
	
	public void dump() {
		Set<String> keys = loads.keySet();
		for (String key : keys) {
			String line = key + ";" + loads.get(key);
			LOGGER.info(line);
		}
	}

}
