package copenhagenabm.loggers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

//import copenhagenabm.loggers.SuccessPerRouteLogger.SimpleTextFormatter;
import copenhagenabm.main.CalibrationModeData;
import copenhagenabm.main.ContextManager;

public class ModelInfoLogger {

	private final static Logger LOGGER = Logger.getLogger(ModelInfoLogger.class.getName());
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
			return "modelid;readable_date;number_of_repetitions;OmitDecisionMatrixMultifields;angle_to_dest;total_agents;n_successes\n";

		}

	}

	public void setup() throws IOException {
		LOGGER.setLevel(Level.INFO);
		String ctf = ContextManager.getModelInfoLoggerFile();
		fileTxt = new FileHandler(ctf, true);
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
	}


	public void log(CalibrationModeData cMd) {

		if (ContextManager.inCalibrationMode()) {

			Date date = new Date(cMd.getUniqueModelID());
			DateFormat formatter = new SimpleDateFormat("d-M-y HH:mm:ss");
			String dateFormatted = formatter.format(date);

			LOGGER.info(
					cMd.getUniqueModelID() + ";" +
							dateFormatted + ";" +	
							cMd.getNumberOfRepetitions() + ";" + 
							ContextManager.getOmitDecisionMatrixMultifields() + ";" + 
							ContextManager.getAngleToDestination() + ";" + 
							cMd.getTotalNumberOfIterations() + ";" + 
							cMd.getSuccessfullyModeledRoutes()
					);

		} else {
			LOGGER.info("TODO:new logline");
		}

	}

	public void close() {
		fileTxt.close();
	}

}
