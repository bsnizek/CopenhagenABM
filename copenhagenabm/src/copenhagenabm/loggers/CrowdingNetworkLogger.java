package copenhagenabm.loggers;

import java.net.URL;
import java.util.HashMap;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

import copenhagenabm.environment.Road;
import copenhagenabm.environment.RoadNetwork;
import copenhagenabm.main.ContextManager;

public class CrowdingNetworkLogger {

	private HashMap<String, Integer> countings = new HashMap<String, Integer>();

	private String dirName;

	public String getDirName() {
		return dirName;
	}

	public void setDirName(String dirName) {
		this.dirName = dirName;
	}

	public CrowdingNetworkLogger(String dirName) {
		this.dirName = dirName;
	}

	public void addCount(String edgeID) {
		if (countings.containsKey(edgeID)) {
			countings.put(edgeID, countings.get(edgeID)+1);
		} else {
			countings.put(edgeID, 1);
		}
	}

	public void empty() {
		countings = new HashMap<String, Integer>();
	}

	/*
	 * dumps the network into a shapefile
	 * Files look like this crowdingNet-<modelRun>-<tick>.shp
	 *
	 */
	@SuppressWarnings("unchecked")
	public void dump(RoadNetwork network, double tick, int modelRun) throws Exception {

		System.out.println("Dumping");

		Double dTick = new Double(tick);
		String fileName = dirName + "/" + "crowdingNet" + "-" + modelRun + "-" + dTick.intValue() + ".shp";

		final ShapefileDataStore dataStore = new ShapefileDataStore(new URL(fileName));

		AttributeType geomField = null;
		AttributeType nField = null;
//		AttributeType modelRunField = null;

		FeatureType ftRoad = null;

		FeatureCollection collection = FeatureCollections.newCollection();

		CoordinateReferenceSystem crs = ContextManager.getAgentGeography().getCRS();

		geomField = DefaultAttributeTypeFactory.newAttributeType("LineString", Geometry.class, true, null, null, crs);
		nField = AttributeTypeFactory.newAttributeType("n", Integer.class);
//		modelRunField = AttributeTypeFactory.newAttributeType("modelRun", Integer.class);

		ftRoad = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geomField, nField}, "road");

		for (Road road: ContextManager.roadContext.getObjects(Road.class)) {

			Geometry geom = road.getGeometry();
			int n = network.getRoadLoad(road);

			Feature ft = ftRoad.create(new Object [] {geom , n, modelRun}, "links");

			collection.add(ft);

		}

		String typeName = dataStore.getTypeNames()[0];
		FeatureStore featureStore = (FeatureStore) dataStore.getFeatureSource(typeName);

		Transaction transaction = new DefaultTransaction("create");
		featureStore.setTransaction(transaction);

		try {
			featureStore.addFeatures(collection);
			transaction.commit();
		} catch (Exception problem) {
			problem.printStackTrace();
			transaction.rollback();
		} finally {
			transaction.close();
		}
		ContextManager.resetCrowdingNetwork();

	}


}
