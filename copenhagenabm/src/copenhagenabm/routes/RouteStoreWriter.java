package copenhagenabm.routes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
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
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.main.ContextManager;

public class RouteStoreWriter {

	private RouteStore routeStore;

	private DefaultTransaction transaction;

	private FeatureStore fs;

	private static Logger LOGGER = Logger.getLogger(ContextManager.class.getName());

	AttributeType geomField = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class);
	AttributeType ID = AttributeTypeFactory.newAttributeType("ID", Integer.class);
	AttributeType GPSID = AttributeTypeFactory.newAttributeType("GPSID", Integer.class);

	// the route that scores the highest path size
	AttributeType bestAttributeType = AttributeTypeFactory.newAttributeType("best", Boolean.class);

	//path size
	AttributeType pathSizeAttributeType = AttributeTypeFactory.newAttributeType("pathsize", Double.class);

	AttributeType routeLen = AttributeTypeFactory.newAttributeType("routeLength", Double.class);

	int featurecounter = 0;

	private String fileName;



	/**
	 * 
	 * Writes result routes to a shapefile
	 * 
	 * @param r
	 * @param fileName
	 * @throws IOException
	 * @throws SchemaException
	 */
	public RouteStoreWriter(RouteStore r, String fileName) throws IOException, SchemaException {

		this.routeStore = r;
		this.fileName = fileName;

		LOGGER.log(Level.FINER, "Route Logger initialized");
		File file = new File(fileName);

		FileDataStoreFactorySpi factory = new IndexedShapefileDataStoreFactory();
		Map<String, URL> map = Collections.singletonMap("shapefile url", file.toURI().toURL());

		DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;

		ShapefileDataStore store = (ShapefileDataStore) factory.createNewDataStore(map);
		store.forceSchemaCRS(crs);

		FeatureType type = getFeatureType();

		store.createSchema(type);

		String featureName = store.getTypeNames()[0]; // there is only one in a shapefile
		transaction = new DefaultTransaction("create");
		fs = (FeatureStore) store.getFeatureSource(featureName);
		fs.setTransaction(transaction);

	}

	/**
	 * Writes the simulated routes to a shapefile
	 */
	public void write() {
		HashMap<Integer, MatchedGPSRoute> matchedGPSRoutes = routeStore.getMatchedGPSRoutes();
		Set<Integer> routeIDs = matchedGPSRoutes.keySet();

		for (Integer routeID : routeIDs) {
			HashMap<Integer, ArrayList<Route>> simulatedRoutes = routeStore.simulatedRoutes;
			ArrayList<Route> routes = simulatedRoutes.get(routeID);
			for (Route r : routes) {
				try {
					writeSimulatedRoute(r, matchedGPSRoutes.get(routeID));
				} catch (SchemaException e) {

					e.printStackTrace();
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}

		}

		for (Integer routeID : routeIDs) {
			System.out.println(routeID + " :: " + this.routeStore.getAveragePathSizeAttr().get(routeID));
		}


	}

	@SuppressWarnings("unchecked")
	private void writeSimulatedRoute(Route simRoute, MatchedGPSRoute matchedGPSRoute) throws SchemaException, IllegalAttributeException {

		FeatureCollection collection = FeatureCollections.newCollection();

		FeatureType ftMeasurement = FeatureTypeBuilder.newFeatureType(
				new AttributeType[] {	
						geomField,
						ID, 
						GPSID, 
						bestAttributeType, 
						pathSizeAttributeType, 
						routeLen
				}, "route"); 

		LineString mls = simRoute.getRouteAsLineString();
		double length = mls.getLength();

		Object[] o = new Object [] {mls, simRoute.getID(), simRoute.getGPSID(), new Boolean(simRoute.isBestRoute()), simRoute.getOverlap(matchedGPSRoute, false) / length };
		Feature ft = ftMeasurement.create(o, "route-" + featurecounter++);

		collection.add(ft);

		try {
			fs.addFeatures(collection);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * commits the transaction and closes it
	 */
	public void close() {
		try {
			transaction.commit();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			transaction.close();
		}

		System.out.println("RouteStoreWriter : " + this.routeStore.getSimulatedRoutes().size() +  " Routes written to " + this.fileName + ".");

	}


	public FeatureType getFeatureType() throws SchemaException {

		return DataUtilities.createType("Location",
				"route:LineString:srid=4326," + // <- the geometry attribute: Point type
						"ID:Integer," +  		// the route's unique ID
						"GPSID:Integer," + 
						"best:Boolean," + 
						"pathsize:Double," + 
				"routeLen:Double");
		//						"roadID:String," +
		//						"absCrowd:Integer," + 
		//						"crowdFac:Double"

		//				);
	}

}
