package copenhagenabm.loggers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import copenhagenabm.agent.Measurement;
import copenhagenabm.main.ContextManager;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

public class AgentHistoryKMZWriter {

	private String writeFolder;
	private int agentID;

	public AgentHistoryKMZWriter(String writeFolder, int agentID, int modelRun) {
		this.writeFolder = writeFolder;
		this.agentID = agentID;
	}


	public void writeHistory(ArrayList<Measurement> history) throws Exception {

		System.out.println("Dumping History of Agent " + agentID + "(KML)");		
		String fileName = writeFolder + "/" + "agent-" + agentID + ".kml";

		// File file = new File(fileName);

		final Kml kml = new Kml();
		Document document = kml.createAndSetDocument().withName("Agent " +  agentID).withOpen(true);

		kml.setFeature(document);
		Folder folder = document.createAndAddFolder();
		folder.withName("Continents with Earth's surface").withOpen(true);


		
		
		//		final LineString linestring = new LineString();
		//		placemark.setGeometry(linestring);
		//		linestring.setExtrude(true);
		//		linestring.setTessellate(true);
		//		List<Coordinate> coord = new ArrayList<Coordinate>();
		//		linestring.setCoordinates(coord);	
		//		Measurement nullMeasurement = history.get(0);

		double kmef = ContextManager.getKMZExaggerationFactor();

		for (int i=1; i<history.size(); i++) {
			final Placemark placemark = new Placemark();
			document.getFeature().add(placemark);
			placemark.setName("extruded");
			final LineString linestring = new LineString();
			placemark.setGeometry(linestring);
			linestring.setExtrude(true);
			linestring.setTessellate(true);
			List<Coordinate> coord = new ArrayList<Coordinate>();
			linestring.setCoordinates(coord);
			
			Measurement mMinusOne = history.get(i-1);
			Measurement m = history.get(i);

			double good = m.getGood() * kmef;

			Coordinate c1 = new Coordinate(mMinusOne.getGeometry().getX(), mMinusOne.getGeometry().getY(),0);
			Coordinate c2 = new Coordinate(mMinusOne.getGeometry().getX(), mMinusOne.getGeometry().getY(),good);
			Coordinate c3 = new Coordinate(m.getGeometry().getX(), m.getGeometry().getY(),good);
			Coordinate c4 = new Coordinate(m.getGeometry().getX(), m.getGeometry().getY(),0);
			coord.add(c1);
			coord.add(c2);
			coord.add(c3);
			coord.add(c4);
			coord.add(c1);		
			folder.addToFeature(placemark);	
		}

		

		kml.marshal(new File(fileName));

	}


}
