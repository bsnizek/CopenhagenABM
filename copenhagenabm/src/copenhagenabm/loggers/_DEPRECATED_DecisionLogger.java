package copenhagenabm.loggers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.indexed.IndexedShapefileDataStoreFactory;
import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import repast.simphony.engine.environment.RunEnvironment;
import repastcity3.exceptions.NoIdentifierException;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;

public class _DEPRECATED_DecisionLogger {

	private static Logger LOGGER = Logger.getLogger(ContextManager.class.getName());

	// the schema

	AttributeType geomField = DefaultAttributeTypeFactory.newAttributeType("MultiLineString", MultiLineString.class); // , true, null, null, crs);
	AttributeType edgeID = AttributeTypeFactory.newAttributeType("edgeID", Integer.class);
	AttributeType tick = AttributeTypeFactory.newAttributeType("tick", Integer.class);
	AttributeType agentID = AttributeTypeFactory.newAttributeType("agentID", Integer.class);
	AttributeType selected = AttributeTypeFactory.newAttributeType("selected",Boolean.class);

	// total number of agents on the edge at the current tick
	AttributeType crowdingN = AttributeTypeFactory.newAttributeType("crowdingFact", Double.class);
	AttributeType greenPct = AttributeTypeFactory.newAttributeType("greenPct", Double.class);
	
	AttributeType left = AttributeTypeFactory.newAttributeType("left", Double.class);
	AttributeType right = AttributeTypeFactory.newAttributeType("right", Double.class);
	
	// TODO: more to come


	Transaction transaction = null;
	FeatureStore fs = null;
	int i=0;


	public _DEPRECATED_DecisionLogger(String fileName) throws IOException, SchemaException {

		LOGGER.log(Level.FINER, "Decision Logger initialized");
		File file = new File(fileName);

		FileDataStoreFactorySpi factory = new IndexedShapefileDataStoreFactory();
		Map<String, URL> map = Collections.singletonMap("shapefile url", file.toURI().toURL());

		DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;

		ShapefileDataStore store = (ShapefileDataStore) factory.createNewDataStore(map);
		store.forceSchemaCRS(crs); // TODO: maybe one should not hardcode this one ? 

		FeatureType type = getFeatureType();

		store.createSchema(type);

		String featureName = store.getTypeNames()[0]; // there is only one in a shapefile
		transaction = new DefaultTransaction();
		fs = (FeatureStore) store.getFeatureSource(featureName);
		fs.setTransaction(transaction);


	}

	public FeatureType getFeatureType() throws SchemaException {
		return DataUtilities.createType("Location",
				"edge:MultiLineString:srid=4326,"  + // <- the geometry attribute: LineString type
						"edgeID:Integer," + 
						"tick:Integer," + 
						"agentID:Integer," + 
						"selected:Boolean," + 
						"crowdingN:Integer," + 
						"greenPct:Double," + 
						"left:Double," + 
						"right:Double," 
				);

	}

	/**
	 * 
	 * Logs a decision into the decision log shape file. 
	 * 
	 * @param roads
	 * @param newRoad
	 * @throws FactoryConfigurationError
	 * @throws SchemaException
	 * @throws IllegalAttributeException
	 * @throws NoIdentifierException 
	 * @throws NumberFormatException 
	 */
	public void logDecision(List<Road> roads, Road newRoad, int agentIDV) throws FactoryConfigurationError, SchemaException, 
	IllegalAttributeException, 
	NumberFormatException, NoIdentifierException {

		int i = 0;

		Integer tickV = new Integer((int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount());

		FeatureCollection collection = FeatureCollections.newCollection();
		FeatureType ftDecision = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geomField, edgeID, tick, agentID, selected, crowdingN, greenPct, left, right}, "decision"); 
		boolean selectedV = false;

		for (Road r : roads) {
			if (r==newRoad) {
				selectedV = true;

				MultiLineString mLs = (MultiLineString) r.getGeometry();
				Integer edgeIDV = new Integer(r.getIdentifier());
				Integer crowdingNV;
//				if (ContextManager.isCrowdingLoggerOn()) {
//					crowdingNV = ContextManager.getCurrentCrowdingN(r);
//				} else {
//					crowdingNV = 0;
//				}

//				Object[] o = new Object [] {mLs, edgeIDV, tickV, agentIDV, selectedV, crowdingNV, r.getGroenpct()};
//				Feature ft = ftDecision.create(o, "decision-" + i);
//				collection.add(ft);
				i++;
			}
		}

		try {
			fs.addFeatures(collection);
			//			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		i++;

	}


	/*
	 * Closes the logger, commits the transaction.
	 */
	public void close() {
		try {
			transaction.commit();
			transaction.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block ADD A LOGGER event
			e.printStackTrace();
		}
	}

}
