package copenhagenabm.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TUMatrixConverter {


	private String filename;
	private BufferedReader br;
	private String outFile;

	public TUMatrixConverter(String filename, String outfileName) {
		this.filename = filename;
		this.outFile = outfileName;

		br = null;

	}

	public void run() {
		
		File logFile=new File(outFile);

	    BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(logFile));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			writer.write("timefrom;timeto;zonefrom;zoneto;number\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		String line = "";
		String cvsSplitBy = ";";
		try {

			br = new BufferedReader(new FileReader(filename));

			String headline = br.readLine();

			int cntr = 0;
			
			while ((line = br.readLine()) != null) {

				// use comma as separator
				String[] l = line.split(cvsSplitBy);

				String zoneFromTo = l[0];
				String zoneFrom = zoneFromTo.substring(0, 6);
				String zoneTo = zoneFromTo.substring(6, 12);

				for (int i=2; i<25; i++) {
					Integer nmbr = 0;
					try {
						nmbr = new Integer(l[i]);
					} catch (Exception e){
						System.out.println(line + " throws error");
						nmbr=0;

					}
					if (nmbr>0) {
						String tFrom = "";
						String tTo = "";
						
						if (i<10) {
							tFrom = "0"+ i + ":00";
							if (i > 9) {
								tTo = i + ":59";
							} else {
								tTo = "0" + i + ":59";
							}
						} else {
							int j = i;
							if (i==24) {
								j=0;
							}
							tFrom = new  Integer(j).toString() + ":00";
							tTo = new Integer(j).toString() + ":59";
						}
						
						writer.write(tFrom + ";" + tTo + ";" + zoneFrom + ";" + zoneTo + ";" + nmbr + "\n");
					}
				}
				cntr ++;
			}
			

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("Done");
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {

		String csvFile = "geodata/data/tu-v2.csv";
		String outFile = "geodata/data/tu.txt";
				
				
		TUMatrixConverter tmc = new TUMatrixConverter(csvFile, outFile);
		tmc.run();

	}

}
