package copenhagenabm.loggers;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

//import copenhagenabm.loggers.CalibrationLogger.SimpleTextFormatter;
import copenhagenabm.main.ContextManager;

public class SuccessLogger {
	
	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}
		
		public String getHead(Handler h) {
			return "agentID;angleToDestWeight;totalIterations;successfulIterations;secondsrun\n";
		}
		
//		public String getTail(Handler h) {
//			
//			return "TAIL";
////			double sum = 0.0d;
////			for (double d : pathSizes) {
////				sum = sum + d;
////			}
////			
////			String[] xx = new Double(sum/pathSizes.size()).toString().split("\\.");
////			
////			return "AVG:" + xx[0] + "," + xx[1];
//		}
		
	}
	
	private final static Logger LOGGER = Logger.getLogger(SuccessLogger.class.getName());
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;
	
	public void setup() throws IOException {
		LOGGER.setLevel(Level.INFO);
		fileTxt = new FileHandler(ContextManager.getSuccessloggerFile(), true);
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
	}
	
	 public void logLine(String line) {
		 LOGGER.info(line);
	 }

}
