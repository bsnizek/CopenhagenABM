package copenhagenabm.loggers.decisionmatrixlogger;


import java.io.IOException;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.loggers.CopenhagenABMLogger;


//import org.kvintus.core.setup.GlobalRegister;

/**
 * 
 * The DecisionMatrixLogger logs the decisions taken at nodes of the networks. 
 * 
 * This class is part of the kvintus.org project
 * 
 * @author Bernhard Snizek, metascapes.org
 *
 */
public class DecisionMatrixLogger implements CopenhagenABMLogger {
	
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;
	Logger LOGGER = Logger.getLogger(DecisionMatrixLogger.class.getName());
	
	/**
	 * 
	 * Constructor, want the whole filename as logFileName
	 * 
	 * @param logFileName
	 */
	public DecisionMatrixLogger(String logFileName) {
			
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
	
	/**
	 * 
	 * A minimal formatter for logging
	 * 
	 * @author besn
	 */
	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}


	}

	public void logLine(String l) {
		LOGGER.info(l);
	}

	public void close() {
		Handler[] handlers = LOGGER.getHandlers();

		for (Handler h : handlers) {
			h.close();
		}
	}

	
	
}
