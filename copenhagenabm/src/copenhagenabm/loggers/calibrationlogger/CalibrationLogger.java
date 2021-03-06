package copenhagenabm.loggers.calibrationlogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.loggers.CopenhagenABMLogger;
import copenhagenabm.main.ContextManager;


/**
 * The CalibrationLogger logs the path size attribute for every model run
 * 
 * @author besn
 *
 */
public class CalibrationLogger implements CopenhagenABMLogger{

	public ArrayList<Double> pathSizes = new ArrayList<Double>();

	private final static Logger LOGGER = Logger.getLogger(CalibrationLogger.class.getName());
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;

	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}

		public String getHead(Handler h) {
			return "pathSize;angle_to_destination;route_ID;number_of_edges\n";
		}

		//		public String getTail(Handler h) {
		////
		////			double sum = 0.0d;
		////
		////			for (double d : pathSizes) {
		////				sum = sum + d;
		////			}
		////
		////			String[] xx = new Double(sum/pathSizes.size()).toString().split("\\.");
		////
		////			return "AVG:" + xx[0] + "," + xx[1];
		//		}

	}

	public CalibrationLogger(String logFileName) {
		
		LOGGER.setLevel(Level.INFO);
		
		try {
			fileTxt = new FileHandler(logFileName, true);
		} catch (SecurityException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
		
	}


	public void setup() throws IOException {

	}

	public void logLine(String line) {
		LOGGER.info(line);
	}

	public void close() {
		Handler[] handlers = LOGGER.getHandlers();

		for (Handler h : handlers) {
			h.close();
		}
	}



}
