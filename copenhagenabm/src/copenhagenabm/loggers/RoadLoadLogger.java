package copenhagenabm.loggers;

/**
 * 
 * A logger for the road load counter.
 * 
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.main.ContextManager;

public class RoadLoadLogger {

	public static final boolean PARTIAL_LOGGING = false;

	public static final List<String> EDGE_IDS_TO_LOG = Collections.unmodifiableList(Arrays.asList("30709", "30669", "52268", "21620", "19704", 
			"22651", "20140", "20242", "20280", "11284", "49687", "19697", "22584", "46559", "28677", "45463", 
			"22770", "19404", "21405", "21475", "21411", "24817", "24344", "23252", "16127", "16665", "44925", "19440", "21252", "18299"));

	final List<String> IDS_TO_EXCLUDE = 
			Collections.unmodifiableList(Arrays.asList("-1", "-2"));

	public class wholeDayLogger {

	}

	public class simpleWholeDayLogger {

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

	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}


	}

	public RoadLoadLogger(String folder) {

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

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		simpleTextFormatter = new SimpleTextFormatter();
		fileTxt.setFormatter(simpleTextFormatter);
		LOGGER.addHandler(fileTxt);
		LOGGER.setUseParentHandlers(false);

		Set<String> roadIDs = values.keySet();
		List<String> roadIDList = new ArrayList<String>(roadIDs);

		java.util.Collections.sort(roadIDList);



		for (String key :roadIDList) {

			if (!IDS_TO_EXCLUDE.contains(key)) {

				LOGGER.info(key + ";" + values.get(key));
			}
		}

		wholeDay.put(currentTick, values);

		// reset the values		
		values = new HashMap<String, Integer>();

		System.out.println("RoadLoadLogger dumped at TICK=" + currentTick);

		Handler[] handlers = LOGGER.getHandlers();

		for (Handler h : handlers) {
			h.close();
		}



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



	/**
	 * dumpSimpleWholeDay() dumps whole day data to a file where every road network edge's load is dumped into one line
	 * <road_network_identifier>;<load_int>
	 */
	public void dumpSimpleWholeDay() {

		Logger simpleWholeDayLOGGER = Logger.getLogger(simpleWholeDayLogger.class.getName());
		simpleWholeDayLOGGER.setLevel(Level.INFO);
		String wDLF = ContextManager.getRoadLoadLoggerFolder() + "/wholeday_simple.txt";
		FileHandler wDLFFile = null;

		try {
			wDLFFile = new FileHandler(wDLF, true);
		} catch (SecurityException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		SimpleTextFormatter wDFormatter = new SimpleTextFormatter();
		wDLFFile.setFormatter(wDFormatter);
		simpleWholeDayLOGGER.addHandler(wDLFFile);
		simpleWholeDayLOGGER.setUseParentHandlers(false);

		// 1. get the keys

		//		ArrayList<String> roadKeys = new ArrayList<String>();

		Set<Integer> _timeStepKeys = wholeDay.keySet();

		HashMap<String, Integer> edgeLoads = new HashMap<String, Integer>();

		for (Integer timeStep : _timeStepKeys) {

			HashMap<String, Integer> day = wholeDay.get(timeStep);
			Set<String> edgeIDs = day.keySet();
			
			int cntr = 0;
			
			for (String edgeID : edgeIDs) {

				if (!IDS_TO_EXCLUDE.contains(edgeID)) {
					
					System.out.println("SimpleRoadLogger (" + cntr + "/" + edgeIDs.size() + ").");

					if (edgeLoads.containsKey(edgeID)) {
						Integer l = edgeLoads.get(edgeID);
						l = l + day.get(edgeID);
						edgeLoads.put(edgeID, l);
					} else {
						edgeLoads.put(edgeID, day.get(edgeID));
					}
				}
				cntr++;
			}
		}

		simpleWholeDayLOGGER.info("identifier;count");

		Set<String> _edgeIDKeySet = edgeLoads.keySet();
		List<String> edgeIDKeySet = new ArrayList<String>(_edgeIDKeySet);
		Collections.sort(edgeIDKeySet);

		for (String edgeID : edgeIDKeySet) {
			simpleWholeDayLOGGER.info(edgeID + ";" + edgeLoads.get(edgeID));
		}

		Handler[] handlers = simpleWholeDayLOGGER.getHandlers();

		for (Handler h : handlers) {
			h.close();
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

			e.printStackTrace();
		} catch (IOException e) {

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

			if (!IDS_TO_EXCLUDE.contains(roadKey)) {

				if (PARTIAL_LOGGING) {

					if (!IDS_TO_EXCLUDE.contains(roadKey)) {

						if (EDGE_IDS_TO_LOG.contains(roadKey)) {

							ss = ss + ";" + roadKey;
						} 
					} else {
						ss = ss + ";" + roadKey;
					}
				} else {
					ss = ss + ";" + roadKey;
				}
			}
		}

		wholeDayLOGGER.info(ss);

		for (Integer timeStepKey : timeStepKeys) {
			HashMap<String, Integer> day = wholeDay.get(timeStepKey);
			String s  = "";
			for (String roadID : roadKeys) {

				if (!IDS_TO_EXCLUDE.contains(roadID)) {


					if (PARTIAL_LOGGING) {

						if (EDGE_IDS_TO_LOG.contains(roadID)) {

							int c;
							Integer count = day.get(roadID);
							if (count == null) {
								c = 0;
							} else {
								c = count;
							}
							s = s + c + ";";
						}
					} else {
						int c;
						Integer count = day.get(roadID);
						if (count == null) {
							c = 0;
						} else {
							c = count;
						}
						s = s + c + ";";
					}
				}
				wholeDayLOGGER.info("" + timeStepKey + ";" + s);
			}
		}

		Handler[] handlers = wholeDayLOGGER.getHandlers();

		for (Handler h : handlers) {
			h.close();
		}

	}



}
