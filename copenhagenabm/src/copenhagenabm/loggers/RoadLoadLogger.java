package copenhagenabm.loggers;

/**
 * 
 * A logger for the road load counter.
 * 
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.main.ContextManager;

public class RoadLoadLogger {
	
	public class wholeDayLogger {
		
	}
	
//	private String folder;
	
	
	/**
	 * Stores the countings, keyed by road ID, values = numbers of entries onto the road segment. 
	 */
	private HashMap<String, Integer> values = new HashMap<String, Integer>();
	private FileHandler fileTxt;
	private SimpleTextFormatter simpleTextFormatter;
	
	// whole day covers a whole day of recording key1 = time_of_loggin, key2 = roadID, value = number of stepOns
	private HashMap<Integer, HashMap<String, Integer>> wholeDay = new HashMap<Integer, HashMap<String, Integer>>();


	private Integer dumpAt;
	
	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}
		
		
	}

	public RoadLoadLogger(String folder) {
//		this.folder = folder;
		this.dumpAt = ContextManager.writeRoadLoadEveryTick();
	}
	
	public void setup() {
		
	}
	
	/**
	 * 
	 */
	public void dump(Integer currentTick) {
		
		Logger LOGGER = Logger.getLogger(RoadLoadLogger.class.getName());
		
		LOGGER.setLevel(Level.INFO);
		
		String ctf = ContextManager.getRoadLoadLoggerFolder() + "/roadloads-" + currentTick + ".txt";
		
		try {
			fileTxt = new FileHandler(ctf, true);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);
		
		Set<String> roadIDs = values.keySet();
		for (String key :roadIDs) {
			LOGGER.info(key + ";" + values.get(key));
		}
		
		wholeDay.put(currentTick, values);
		
		// reset the values		
		values = new HashMap<String, Integer>();
		
	}
	
	/**
	 * Increments the values of the segment roadID with 1
	 * @param roadID
	 */
	public void addEntry(String roadID) {
		if (values.containsKey(roadID)) {
			values.put(roadID, values.get(roadID)+1);
		} else {
			values.put(roadID, 1);
		}
	}

	public void tick(Integer tick) {
		double v = (double) tick / dumpAt;
		if (v - Math.floor(v) == 0.0f) {
			this.dump(tick);
		}
		
	}
	
	/**
	 * 
	 * aggregates the whole day into one file, which is kinda nice
	 * 
	 */
	public void dumpWholeDay() {
		Logger wholeDayLOGGER = Logger.getLogger(wholeDayLogger.class.getName());
		wholeDayLOGGER.setLevel(Level.INFO);
		String wDLF = ContextManager.getRoadLoadLoggerFolder() + "/wholeday.txt";
		FileHandler wDLFFile = null;
		try {
			wDLFFile = new FileHandler(wDLF, true);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SimpleTextFormatter wDFormatter = new SimpleTextFormatter();
		wDLFFile.setFormatter(wDFormatter);
		wholeDayLOGGER.addHandler(wDLFFile);
		wholeDayLOGGER.setUseParentHandlers(false);
		
		// 1. get the keys
		
		ArrayList<String> roadKeys = new ArrayList<String>();
		
		Set<Integer> _timeStepKeys = wholeDay.keySet();
		
		List<Integer> timeStepKeys = new ArrayList<Integer>(_timeStepKeys);
		Collections.sort(timeStepKeys);
		
		for (Integer timeStepKey : timeStepKeys) {
			HashMap<String, Integer> timeStepValues = wholeDay.get(timeStepKey);
			Set<String> roadIDs = timeStepValues.keySet();
			for (String roadID : roadIDs) {
				if (!roadKeys.contains(roadID)) {
					roadKeys.add(roadID);
				}
			}
		}
		
		Collections.sort(roadKeys);
		
		String ss = "time";
		for (String roadKey : roadKeys) {
			ss = ss + ";" + roadKey;
		}
		
		wholeDayLOGGER.info(ss);
		
		for (Integer timeStepKey : timeStepKeys) {
			HashMap<String, Integer> day = wholeDay.get(timeStepKey);
			String s  = "";
			for (String roadID : roadKeys) {
				int c;
				Integer count = day.get(roadID);
				if (count == null) {
					c = 0;
				} else {
					c = count;
				}
				s = s + c + ";";
			}
			wholeDayLOGGER.info("" + timeStepKey + ";" + s);
		}
		
	}

}
