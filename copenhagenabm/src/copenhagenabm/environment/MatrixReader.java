package copenhagenabm.environment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.StringTokenizer;


import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.tools.CopenhagenABMTools;

/*
 * the MatrixReader reads a file with the matrix definition and 
 * distributes the agents to the entry points
 * 
 * The Matrix Definition File 
 * 
 */
public class MatrixReader {

	public class Spawn {
		private String zoneFrom;
		private String zoneTo;
		private long spawnAtTick;

		public Spawn(String zoneFrom, String zoneTo, long spawnAtTick) {
			this.setZoneFrom(zoneFrom);
			this.setZoneTo(zoneTo);
			this.setSpawnAtTick(spawnAtTick);
		}

		public String getZoneFrom() {
			return zoneFrom;
		}

		public void setZoneFrom(String zoneFrom) {
			this.zoneFrom = zoneFrom;
		}

		public String getZoneTo() {
			return zoneTo;
		}

		public void setZoneTo(String zoneTo) {
			this.zoneTo = zoneTo;
		}

		public long getSpawnAtTick() {
			return spawnAtTick;
		}

		public void setSpawnAtTick(long spawnAtTick) {
			this.spawnAtTick = spawnAtTick;
		}

	}

	String line = null;
	int col = 0;
	private String t_from;
	private String t_to;
	private ArrayList<Spawn> spawns = new ArrayList<Spawn>();
	private String zonefrom;
	private String zoneto;
	private Integer number;

	CopenhagenABMTools cphTools = new CopenhagenABMTools();

	private int tickLength = new Integer(ContextManager.getProperty(GlobalVars.StepLength));

	public MatrixReader(String filename) throws IOException {
		BufferedReader bufRdr  = new BufferedReader(new FileReader(filename));
		Boolean dropFirstLine = true;

		if (dropFirstLine) {
			bufRdr.readLine();
		}
		
		while((line = bufRdr.readLine()) != null)
		{
			StringTokenizer st = new StringTokenizer(line,";");

			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				if (col==0) {
					t_from = s;
				}
				if (col==1) {
					t_to = s;
				}
				if (col==2) {
					zonefrom = s;
				}
				if (col==3) {
					zoneto = s;
				}
				if (col==4) {
					number = new Integer(s);
				}
				col++;
			}
			col = 0;

			int groupingFactor = ContextManager.getGroupingFactor();

			number = Math.round(number / groupingFactor);

			if (number > 0) {

				for (int i=0; i<number; i++) {
					try {
						long tick = cphTools.getRandomTick(t_from, t_to, tickLength);
						Spawn s = new Spawn(zonefrom, zoneto, tick);
						this.spawns.add(s);
						// System.out.println("One spawn from " + zonefrom + " to " + zoneto + " added @ " + tick);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}

		}
	}


	public ArrayList<Spawn> getSpawns() {
		// TODO Auto-generated method stub
		return spawns;
	}
}
