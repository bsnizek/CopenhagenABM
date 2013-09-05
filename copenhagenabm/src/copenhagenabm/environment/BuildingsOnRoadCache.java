package copenhagenabm.environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import repast.simphony.space.gis.Geography;
import repastcity3.environment.Building;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;

/**
 * Class can be used to store a cache of all roads and the buildings which can be accessed by them (a map of
 * Road<->List<Building>. Buildings are 'accessed' by travelling to the road which is nearest to them.
 * <p>
 * This class can be serialised so that if the GIS data doesn't change it doesn't have to be re-calculated each time.
 * However, the Roads and Buildings themselves cannot be serialised because if they are there will be two sets of Roads
 * and BUildings, the serialised ones and those that were created when the model was initialised. To get round this, an
 * array which contains the road and building ids is serialised and the cache is re-built using these caches ids after
 * reading the serialised cache. This means that the id's given to Buildings and Roads must not change (i.e.
 * auto-increment numbers are no good because if a simulation is restarted the static auto-increment variables will not
 * be reset to 0).
 * 
 * @author Nick Malleson
 */
public class BuildingsOnRoadCache implements Serializable {

	private static Logger LOGGER = Logger.getLogger(BuildingsOnRoadCache.class.getName());

	private static final long serialVersionUID = 1L;
	// The actual cache, this isn't serialised
	private static transient Hashtable<Road, ArrayList<Building>> theCache;
	// The 'reference' cache, stores the building and road ids and can be
	// serialised
	private Hashtable<String, ArrayList<String>> referenceCache;

	// Check that the road/building data hasn't been changed since the cache was
	// last created
	private File buildingsFile;
	private File roadsFile;
	// The location that the serialised object might be found.
	private File serialisedLoc;
	// The time that this cache was created, can be used to check data hasn't
	// changed since
	private long createdTime;

	// Private constructor because getInstance() should be used
	private BuildingsOnRoadCache(Geography<Building> buildingEnvironment, File buildingsFile,
			Geography<Road> roadEnvironment, File roadsFile, File serialisedLoc, GeometryFactory geomFac)
			throws Exception {
		// this.buildingEnvironment = buildingEnvironment;
		// this.roadEnvironment = roadEnvironment;
		this.buildingsFile = buildingsFile;
		this.roadsFile = roadsFile;
		this.serialisedLoc = serialisedLoc;
		theCache = new Hashtable<Road, ArrayList<Building>>();
		this.referenceCache = new Hashtable<String, ArrayList<String>>();

		LOGGER.log(Level.FINE, "BuildingsOnRoadCache() creating new cache with data (and modification date):\n\t"
				+ this.buildingsFile.getAbsolutePath() + " (" + new Date(this.buildingsFile.lastModified()) + ")\n\t"
				+ this.roadsFile.getAbsolutePath() + " (" + new Date(this.roadsFile.lastModified()) + ")\n\t"
				+ this.serialisedLoc.getAbsolutePath());

		populateCache(buildingEnvironment, roadEnvironment, geomFac);
		this.createdTime = new Date().getTime();
		serialise();
	}

	public void clear() {
		theCache.clear();
		this.referenceCache.clear();

	}

	private void populateCache(Geography<Building> buildingEnvironment, Geography<Road> roadEnvironment,
			GeometryFactory geomFac) throws Exception {
		double time = System.nanoTime();
		for (Building b : buildingEnvironment.getAllObjects()) {
			// Find the closest road to this building
			Geometry buildingPoint = geomFac.createPoint(b.getCoords());
			double minDistance = Double.MAX_VALUE;
			Road closestRoad = null;
			double distance;
			Envelope e = buildingPoint.buffer(GlobalVars.GEOGRAPHY_PARAMS.BUFFER_DISTANCE.LARGE.dist)
					.getEnvelopeInternal();
			for (Road r : roadEnvironment.getObjectsWithin(e)) {
				distance = DistanceOp.distance(buildingPoint, ContextManager.roadProjection.getGeometry(r));
				if (distance < minDistance) {
					minDistance = distance;
					closestRoad = r;
				}
			} // for roads
				// Found the closest road, add the information to the cache
			if (theCache.containsKey(closestRoad)) {
				theCache.get(closestRoad).add(b);
				this.referenceCache.get(closestRoad.getIdentifier()).add(b.getIdentifier());
			} else {
				ArrayList<Building> l = new ArrayList<Building>();
				l.add(b);
				theCache.put(closestRoad, l);
				ArrayList<String> l2 = new ArrayList<String>();
				l2.add(b.getIdentifier());
				this.referenceCache.put(closestRoad.getIdentifier(), l2);
			}
		} // for buildings
		int numRoads = theCache.keySet().size();
		int numBuildings = 0;
		for (List<Building> l : theCache.values())
			numBuildings += l.size();
		LOGGER.log(Level.FINER, "Finished caching roads and buildings. Cached " + numRoads + " roads and "
				+ numBuildings + " buildings in " + 0.000001 * (System.nanoTime() - time) + "ms");
	}

	public List<Building> get(Road r) {
		return theCache.get(r);
	}

	private void serialise() throws IOException {
		double time = System.nanoTime();
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			if (!this.serialisedLoc.exists())
				this.serialisedLoc.createNewFile();
			fos = new FileOutputStream(this.serialisedLoc);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		} catch (IOException ex) {
			if (serialisedLoc.exists())
				serialisedLoc.delete(); // delete to stop problems loading incomplete file next time
			throw ex;
		}
		LOGGER.log(Level.FINER, "Serialised BuildingsOnRoadCache to " + this.serialisedLoc.getAbsolutePath() + " in ("
				+ 0.000001 * (System.nanoTime() - time) + "ms)");
	}

	/**
	 * Used to create a new BuildingsOnRoadCache object. This function is used instead of the constructor directly so
	 * that the class can check if there is a serialised version on disk already. If not then a new one is created and
	 * returned.
	 * 
	 * @param buildingEnv
	 * @param buildingsFile
	 * @param roadEnv
	 * @param roadsFile
	 * @param serialisedLoc
	 * @param geomFac
	 * @return
	 * @throws Exception
	 */
	public synchronized static BuildingsOnRoadCache getInstance(Geography<Building> buildingEnv, File buildingsFile,
			Geography<Road> roadEnv, File roadsFile, File serialisedLoc, GeometryFactory geomFac) throws Exception {
		double time = System.nanoTime();
		// See if there is a cache object on disk.
		if (serialisedLoc.exists()) {
			FileInputStream fis = null;
			ObjectInputStream in = null;
			BuildingsOnRoadCache bc = null;
			try {
				fis = new FileInputStream(serialisedLoc);
				in = new ObjectInputStream(fis);
				bc = (BuildingsOnRoadCache) in.readObject();
				in.close();

				// Check that the cache is representing the correct data and the
				// modification dates are ok
				// (WARNING, if this class is re-compiled the serialised object
				// will still be read in).
				if (!buildingsFile.getAbsolutePath().equals(bc.buildingsFile.getAbsolutePath())
						|| !roadsFile.getAbsolutePath().equals(bc.roadsFile.getAbsolutePath())
						|| buildingsFile.lastModified() > bc.createdTime || roadsFile.lastModified() > bc.createdTime) {
					LOGGER.log(Level.FINER, "BuildingsOnRoadCache, found serialised object but it doesn't match the "
							+ "data (or could have different modification dates), will create a new cache.");
				} else {
					// Have found a useable serialised cache. Now use the cached
					// list of id's to construct a
					// new cache of buildings and roads.
					// First need to buld list of existing roads and buildings
					Hashtable<String, Road> allRoads = new Hashtable<String, Road>();
					for (Road r : roadEnv.getAllObjects())
						allRoads.put(r.getIdentifier(), r);
					Hashtable<String, Building> allBuildings = new Hashtable<String, Building>();
					for (Building b : buildingEnv.getAllObjects())
						allBuildings.put(b.getIdentifier(), b);

					// Now create the new cache
					theCache = new Hashtable<Road, ArrayList<Building>>();

					for (String roadId : bc.referenceCache.keySet()) {
						ArrayList<Building> buildings = new ArrayList<Building>();
						for (String buildingId : bc.referenceCache.get(roadId)) {
							buildings.add(allBuildings.get(buildingId));
						}
						theCache.put(allRoads.get(roadId), buildings);
					}
					LOGGER.log(Level.FINER, "BuildingsOnRoadCache, found serialised cache, returning it (in "
							+ 0.000001 * (System.nanoTime() - time) + "ms)");
					return bc;
				}
			} catch (IOException ex) {
				if (serialisedLoc.exists())
					serialisedLoc.delete(); // delete to stop problems loading incomplete file next tinme
				throw ex;
			} catch (ClassNotFoundException ex) {
				if (serialisedLoc.exists())
					serialisedLoc.delete();
				throw ex;
			}

		}

		// No serialised object, or got an error when opening it, just create a
		// new one
		return new BuildingsOnRoadCache(buildingEnv, buildingsFile, roadEnv, roadsFile, serialisedLoc, geomFac);
	}
}