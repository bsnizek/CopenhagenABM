package copenhagenabm.environment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import copenhagenabm.main.ContextManager;

import repast.simphony.context.Context;
import repast.simphony.util.collections.IndexedIterable;
import repastcity3.exceptions.NoIdentifierException;


/**
 * 
 * The LoadNetwork is a class that stores 
 * 
 * @author besn
 *
 */
public class LoadNetwork {

	HashMap<String, Integer> roads = new HashMap<String, Integer>();

	private String folderName;

	private int iteration;

	public LoadNetwork(String folderName, int iteration) {
		this.folderName = folderName;
		this.iteration = iteration;
	}

	public void addEntryToRoad(Road road) throws NoIdentifierException {

		if (road != null) {


			String roadID = road.getIdentifier();
			if (roads.containsKey(roadID)) {
				Integer x = roads.get(roadID);
				x = x + 1;
				roads.remove(roadID);
				roads.put(roadID, x);

			} else {
				roads.put(roadID, 1);
			}

		} else {
			System.out.println("road error");
		}
	}

	public void dump() throws NoIdentifierException, FactoryConfigurationError, SchemaException, IllegalAttributeException, IOException {

		// get hold of the road context
		Context<Road> roadContext = ContextManager.getRoadContext();

		// build an iterator over 
		IndexedIterable<Road> roadIter = roadContext.getObjects(Road.class);

		// now let us set up the file

		if (!folderName.endsWith("/")) {
			folderName += "/";
		}
		String fileName = folderName + "load" + iteration + ".shp";

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

		// the shapefile's fields
		AttributeType geomField = DefaultAttributeTypeFactory.newAttributeType("MultiLineString", Geometry.class, true, null, null, crs);
		AttributeType nField = AttributeTypeFactory.newAttributeType("n", Integer.class);

		FeatureType ftRoad = null;

		ftRoad = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geomField, nField}, "road");

		int i=0;

		for (Road r : roadIter) {
			MultiLineString geom = (MultiLineString) r.getGeometry();
			String identifier = r.getIdentifier();

			if (roads.containsKey(identifier)) {

				int load = roads.get(identifier);

				Feature ft = ftRoad.create(new Object [] {geom , load}, "links-" + i);

				collection.add(ft);

				i++;

			} else {
				System.out.println("LoadNetworkWriter: Road " + identifier + " not found.");
			}

		}

		System.out.println("Roads written: " + i);

		try {
			fs.addFeatures(collection);
			transaction.commit();
		} finally {
			transaction.close();
		}

	}

	public FeatureType getFeatureType() throws SchemaException {
		return DataUtilities.createType("Location",
				"geomField:MultiLineString:srid=4326," +
						"nField:Integer" 
				);
	}


}
