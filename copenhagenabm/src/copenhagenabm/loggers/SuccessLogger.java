package copenhagenabm.loggers;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.main.CalibrationModeData;
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
			return "angleToDestWeight;OmitDecisionMatrixMultifields;totalIterations;successfulIterations;secondsrun\n";
		}

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

	public void log(CalibrationModeData calibrationModeData) {
		LOGGER.info(calibrationModeData.getAngleToDestWeight() + ";" + 
				calibrationModeData.isOmitDecisionMatrixMultifields() + ";" +
				calibrationModeData.getNumberOfRepetitions() + ";" + 
				calibrationModeData.getSuccessfullyModeledRoutes() + ";" + 
				calibrationModeData.getRunTime()

				);
	}

}
