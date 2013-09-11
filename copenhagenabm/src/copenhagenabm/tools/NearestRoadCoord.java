package copenhagenabm.tools;

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

import cern.colt.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import copenhagenabm.environment.Road;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;

public class NearestRoadCoord {
	
	public NearestRoadCoord() {
		
	}
	
	/**
	 * Caches the nearest road Coordinate to every building for efficiency (agents usually/always need to get from the
	 * centroids of houses to/from the nearest road).
	 * <p>
	 * This class can be serialised so that if the GIS data doesn't change it doesn't have to be re-calculated each time.
	 * 
	 * @author Nick Malleson
	 */
	static class NearestRoadCoordCache implements Serializable {

		private static final Logger LOGGER = Logger.getLogger(NearestRoadCoordCache.class.getName());

		private static final long serialVersionUID = 1L;
		private Hashtable<Coordinate, Coordinate> theCache; // The actual cache
		// Check that the road/building data hasn't been changed since the cache was
		// last created
		private File buildingsFile;
		private File roadsFile;
		// The location that the serialised object might be found.
		private File serialisedLoc;
		// The time that this cache was created, can be used to check data hasn't
		// changed since
		private long createdTime;

		private GeometryFactory geomFac;

		private NearestRoadCoordCache(Geography<Building> buildingEnvironment, File buildingsFile,
				Geography<Road> roadEnvironment, File roadsFile, File serialisedLoc, GeometryFactory geomFac)
				throws Exception {

			this.buildingsFile = buildingsFile;
			this.roadsFile = roadsFile;
			this.serialisedLoc = serialisedLoc;
			this.theCache = new Hashtable<Coordinate, Coordinate>();
			this.geomFac = geomFac;

			LOGGER.log(Level.FINE, "NearestRoadCoordCache() creating new cache with data (and modification date):\n\t"
					+ this.buildingsFile.getAbsolutePath() + " (" + new Date(this.buildingsFile.lastModified()) + ") \n\t"
					+ this.roadsFile.getAbsolutePath() + " (" + new Date(this.roadsFile.lastModified()) + "):\n\t"
					+ this.serialisedLoc.getAbsolutePath());

			populateCache(buildingEnvironment, roadEnvironment);
			this.createdTime = new Date().getTime();
			serialise();
		}

		public void clear() {
			this.theCache.clear();
		}

		private void populateCache(Geography<Building> buildingEnvironment, Geography<Road> roadEnvironment)
				throws Exception {
			double time = System.nanoTime();
			theCache = new Hashtable<Coordinate, Coordinate>();
			// Iterate over every building and find the nearest road point
			for (Building b : buildingEnvironment.getAllObjects()) {
				List<Coordinate> nearestCoords = new ArrayList<Coordinate>();
				/*
				 * BESN
				 * 
				 * Route.findNearestObject(b.getCoords(), roadEnvironment, nearestCoords,
						GlobalVars.GEOGRAPHY_PARAMS.BUFFER_DISTANCE.LARGE);
						*/
				// Two coordinates returned by closestPoints(), need to find the one
				// which isn't the building coord
				Coordinate nearestPoint = null;
				for (Coordinate c : nearestCoords) {
					if (!c.equals(b.getCoords())) {
						nearestPoint = c;
						break;
					}
				} // for nearestCoords
				if (nearestPoint == null) {
					throw new Exception("Route.getNearestRoadCoord() error: couldn't find a road coordinate which "
							+ "is close to building " + b.toString());
				}
				theCache.put(b.getCoords(), nearestPoint);
			}// for Buildings
			LOGGER.log(Level.FINER, "Finished caching nearest roads (" + (0.000001 * (System.nanoTime() - time)) + "ms)");
		} // if nearestRoadCoordCache = null;

		/**
		 * 
		 * @param c
		 * @return
		 * @throws Exception
		 */
		public Coordinate get(Coordinate c) throws Exception {
			if (c == null) {
				throw new Exception("Route.NearestRoadCoordCache.get() error: the given coordinate is null.");
			}
			double time = System.nanoTime();
			Coordinate nearestCoord = this.theCache.get(c);
			if (nearestCoord != null) {
				LOGGER.log(Level.FINER, "NearestRoadCoordCache.get() (using cache) - ("
						+ (0.000001 * (System.nanoTime() - time)) + "ms)");
				return nearestCoord;
			}
			// If get here then the coord is not in the cache, agent not starting their journey from a house, search for
			// it manually. Search all roads in the vicinity, looking for the point which is nearest the person
			double minDist = Double.MAX_VALUE;
			Coordinate nearestPoint = null;
			Point coordGeom = this.geomFac.createPoint(c);

			// Note: could use an expanding envelope that starts small and gets bigger
			double bufferDist = GlobalVars.GEOGRAPHY_PARAMS.BUFFER_DISTANCE.LARGE.dist;
			double bufferMultiplier = 1.0;
			Envelope searchEnvelope = coordGeom.buffer(bufferDist * bufferMultiplier).getEnvelopeInternal();
			StringBuilder debug = new StringBuilder(); // incase the operation fails

			for (Road r : ContextManager.roadProjection.getObjectsWithin(searchEnvelope)) {

				DistanceOp distOp = new DistanceOp(coordGeom, ContextManager.roadProjection.getGeometry(r));
				double thisDist = distOp.distance();
				// BUG?: if an agent is on a really long road, the long road will not be found by getObjectsWithin because
				// it is not within the buffer
				debug.append("\troad ").append(r.toString()).append(" is ").append(thisDist).append(
						" distance away (at closest point). ");

				if (thisDist < minDist) {
					minDist = thisDist;
					Coordinate[] closestPoints = distOp.closestPoints();
					// Two coordinates returned by closestPoints(), need to find the
					// one which isn''t the coord parameter
					debug.append("Closest points (").append(closestPoints.length).append(") are: ").append(
							Arrays.toString(closestPoints));
					nearestPoint = (c.equals(closestPoints[0])) ? closestPoints[1] : closestPoints[0];
					debug.append("Nearest point is ").append(nearestPoint.toString());
					nearestPoint = (c.equals(closestPoints[0])) ? closestPoints[1] : closestPoints[0];
				} // if thisDist < minDist
				debug.append("\n");

			} // for nearRoads

			if (nearestPoint != null) {
				LOGGER.log(Level.FINER, "NearestRoadCoordCache.get() (not using cache) - ("
						+ (0.000001 * (System.nanoTime() - time)) + "ms)");
				return nearestPoint;
			}
			/* IF HERE THEN ERROR, PRINT DEBUGGING INFO */
			StringBuilder debugIntro = new StringBuilder(); // Some extra info for debugging
			debugIntro.append("Route.NearestRoadCoordCache.get() error: couldn't find a coordinate to return.\n");
			Iterable<Road> roads = ContextManager.roadProjection.getObjectsWithin(searchEnvelope);
			debugIntro.append("Looking for nearest road coordinate around ").append(c.toString()).append(".\n");
			debugIntro.append("RoadEnvironment.getObjectsWithin() returned ").append(
					ContextManager.sizeOfIterable(roads) + " roads, printing debugging info:\n");
			debugIntro.append(debug);
			throw new Exception(debugIntro.toString());

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
				if (serialisedLoc.exists()) {
					// delete to stop problems loading incomplete file next time
					serialisedLoc.delete();
				}
				throw ex;
			}
			LOGGER.log(Level.FINE, "... serialised NearestRoadCoordCache to " + this.serialisedLoc.getAbsolutePath()
					+ " in (" + 0.000001 * (System.nanoTime() - time) + "ms)");
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
		public synchronized static NearestRoadCoordCache getInstance(Geography<Building> buildingEnv, File buildingsFile,
				Geography<Road> roadEnv, File roadsFile, File serialisedLoc, GeometryFactory geomFac) throws Exception {
			double time = System.nanoTime();
			// See if there is a cache object on disk.
			if (serialisedLoc.exists()) {
				FileInputStream fis = null;
				ObjectInputStream in = null;
				NearestRoadCoordCache ncc = null;
				try {

					fis = new FileInputStream(serialisedLoc);
					in = new ObjectInputStream(fis);
					ncc = (NearestRoadCoordCache) in.readObject();
					in.close();

					// Check that the cache is representing the correct data and the
					// modification dates are ok
					if (!buildingsFile.getAbsolutePath().equals(ncc.buildingsFile.getAbsolutePath())
							|| !roadsFile.getAbsolutePath().equals(ncc.roadsFile.getAbsolutePath())
							|| buildingsFile.lastModified() > ncc.createdTime || roadsFile.lastModified() > ncc.createdTime) {
						LOGGER.log(Level.FINE, "BuildingsOnRoadCache, found serialised object but it doesn't match the "
								+ "data (or could have different modification dates), will create a new cache.");
					} else {
						LOGGER.log(Level.FINER, "NearestRoadCoordCache, found serialised cache, returning it (in "
								+ 0.000001 * (System.nanoTime() - time) + "ms)");
						return ncc;
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

			// No serialised object, or got an error when opening it, just create a new one
			return new NearestRoadCoordCache(buildingEnv, buildingsFile, roadEnv, roadsFile, serialisedLoc, geomFac);
		}

	}
	
	
}
