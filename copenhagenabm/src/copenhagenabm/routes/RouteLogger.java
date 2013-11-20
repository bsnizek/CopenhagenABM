package copenhagenabm.routes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import copenhagenabm.main.ContextManager;

public class RouteLogger {
	
	// private static com.vividsolutions.jts.geom.GeometryFactory fact = new com.vividsolutions.jts.geom.GeometryFactory();
	
	private static Logger LOGGER = Logger.getLogger(ContextManager.class.getName());
	
	// the schema
	AttributeType geomField = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class); // , true, null, null, crs);
	AttributeType ID = AttributeTypeFactory.newAttributeType("ID", Integer.class);
	
	
	Transaction transaction = null;
	FeatureStore fs = null;
	int i=0;
	
	
	
	/**
	 * 
	 * A logger that logs every route taken in the model
	 * 
	 * @param fileName
	 * @throws SchemaException
	 * @throws IOException
	 * 
	 */
	public RouteLogger(String fileName) throws SchemaException, IOException {
		LOGGER.log(Level.FINER, "Route Logger initialized");
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
	
	public void writeRoute(Route r, int agentID) throws FactoryConfigurationError, SchemaException, IllegalAttributeException {
		System.out.println("RoutLogger.writeRoute() currently DEPRECATED.");
	}
//	public void writeRoute(Route r, int agentID) throws FactoryConfigurationError, SchemaException, IllegalAttributeException {
//		FeatureCollection collection = FeatureCollections.newCollection();
//		FeatureType ftMeasurement = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geomField, ID}, "route"); 
//		LineString mls = r.getRouteAsLineString(); 
//		
////		int N = mls.getNumGeometries();
////
////		LineString lines[] = new LineString[ N ];
////		for ( int i = 0; i < N; i++ ) {
////		    lines[ i ] = (LineString) mls.getGeometryN( i );
////		}
//	
//
//		Object[] o = new Object [] {mls, agentID};
//		Feature ft = ftMeasurement.create(o, "measurement-" + i);
//
//		collection.add(ft);
//		
//		try {
//			fs.addFeatures(collection);
////			transaction.commit();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		i++;
//		
//	}
	
	/*
	 * 
	 */
	public void close() {
		try {
			transaction.commit();
			transaction.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public FeatureType getFeatureType() throws SchemaException {
		return DataUtilities.createType("Location",
				"route:LineString:srid=4326,"  + // <- the geometry attribute: Point type
						"ID:Integer"); 
//						"good:Double," + 
//						"bad:Double," +
//						"tick:Double," + 
//						"roadID:String," +
//						"absCrowd:Integer," + 
//						"crowdFac:Double"

//				);
	}

	public void log(HashMap<Integer, Route> routeStore) {
	 	Set<Integer> agentIDs = routeStore.keySet();
		for (int agentID : agentIDs) {
			try {
				writeRoute(routeStore.get(agentID), agentID);
			} catch (FactoryConfigurationError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
