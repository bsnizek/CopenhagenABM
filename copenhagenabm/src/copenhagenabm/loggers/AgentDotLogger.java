package copenhagenabm.loggers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.indexed.IndexedShapefileDataStoreFactory;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.agent.Measurement;
import copenhagenabm.main.ContextManager;

public class AgentDotLogger {

	private String writeFolder;
	private int agentID;

	public AgentDotLogger(String writeFolder, int agentID, int modelRun) {
		this.writeFolder = writeFolder;
		this.agentID = agentID;
	}

	@SuppressWarnings("unchecked")
	public void writeHistory(ArrayList<Measurement> history) throws Exception {

		String fileName = writeFolder + "/" + "agent-" + agentID + "-dots.shp";
		System.out.println("Dumping History of Agent " + agentID + "(SHP) to " + fileName);		
		// String fileName = writeFolder + "/" + "agent-" + agentID + ".shp";
		File file = new File(fileName);

		FileDataStoreFactorySpi factory = new IndexedShapefileDataStoreFactory();
		Map<String, URL> map = Collections.singletonMap("shapefile url", file.toURI().toURL());

		DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;

		ShapefileDataStore store = (ShapefileDataStore) factory.createNewDataStore(map);
		store.forceSchemaCRS(crs); // TODO: maybe one should not hardcode this one ? 

		FeatureType type = getFeatureType();

		store.createSchema(type);

		String featureName = store.getTypeNames()[0]; // there is only one in a shapefile
		Transaction transaction = new DefaultTransaction();
		FeatureStore fs = (FeatureStore) store.getFeatureSource(featureName);
		fs.setTransaction(transaction);

		FeatureCollection collection = FeatureCollections.newCollection();

		AttributeType geomField = DefaultAttributeTypeFactory.newAttributeType("Point", Geometry.class); // , true, null, null, crs);
		AttributeType ID = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType good = AttributeTypeFactory.newAttributeType("good", Double.class);
		AttributeType bad = AttributeTypeFactory.newAttributeType("bad", Double.class);
		AttributeType tick = AttributeTypeFactory.newAttributeType("tick", Double.class);
		AttributeType roadID = AttributeTypeFactory.newAttributeType("roadID", String.class);
		AttributeType absoluteCrowding = AttributeTypeFactory.newAttributeType("absCrowd", Integer.class);
		AttributeType crowdingFactor = AttributeTypeFactory.newAttributeType("crowdFac", Double.class);
		AttributeType speed = AttributeTypeFactory.newAttributeType("speed", Double.class);
		AttributeType modelRun = AttributeTypeFactory.newAttributeType("modelRun", Integer.class);

		FeatureType ftMeasurement = FeatureTypeBuilder.newFeatureType(new AttributeType[] {
				geomField, 
				ID, 
				good, 
				bad, 
				tick, 
				roadID, 
				absoluteCrowding, 
				crowdingFactor, 
				speed,
				modelRun
				}, "measurement"); 
		
		int i = 0;

		for (Measurement m : history) {
			Geometry geom = m.getGeometry();
			Object[] o = new Object [] {
					geom,  
					m.getID(),
					m.getGood(),
					m.getBad(),
					m.getTick(),
					m.getRoadID(),
					m.getAbsoluteCrowding(),
					m.getCrowdingFactor(),
					m.getSpeed(),
					ContextManager.getModelRunID()
					};
			Feature ft = ftMeasurement.create(o,"measurement-" + i);
			collection.add(ft);
			i++;
		}

		try {
			fs.addFeatures(collection);
			transaction.commit();
		} finally {
			transaction.close();
		}

	}

	public FeatureType getFeatureType() throws SchemaException {
		return DataUtilities.createType("Location",
				"location:Point:srid=4326,"  + // <- the geometry attribute: Point type
						"ID:Integer," + 
						"good:Double," + 
						"bad:Double," +
						"tick:Double," + 
						"roadID:String," +
						"absCrowd:Integer," + 
						"crowdFac:Double," + 
						"speed:Double," + 
						"modelRun:Integer"

				);
	}



}
