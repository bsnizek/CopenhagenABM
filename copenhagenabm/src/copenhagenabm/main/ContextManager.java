package copenhagenabm.main;

/*
©Copyright 2012, 2013 Bernhard Snizek, besn@life.ku.dk
©Copyright 2012 Nick Malleson
This file is part of copenhagenABM

copenhagenABM is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.ArrayIndexOutOfBoundsException;

import org.geotools.feature.SchemaException;

import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.test.SpatialIndexFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import copenhagenabm.agent.AgentFactory;

//import copenhagenabm.agent.CPHAgent;
import copenhagenabm.agent.Decision;
import copenhagenabm.agent.Dot;
import copenhagenabm.agent.IAgent;
import copenhagenabm.agent.STOCHASTICTY_TYPES;
//import copenhagenabm.environment.LoadNetwork;
import copenhagenabm.environment.MatrixReader;
import copenhagenabm.environment.Road;
import copenhagenabm.environment.RoadNetwork;
import copenhagenabm.environment.SimpleNearestRoadCoordinateCache;
import copenhagenabm.environment.Zone;
import copenhagenabm.environment.contexts.AgentContext;
import copenhagenabm.environment.contexts.DecisionContext;
import copenhagenabm.environment.contexts.DotContext;
import copenhagenabm.environment.contexts.ResultRouteContext;
import copenhagenabm.environment.contexts.ZoneContext;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;

import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.IllegalParameterException;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;

import repast.simphony.space.gis.SimpleAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.util.collections.IndexedIterable;
import repastcity3.environment.Building;
import repastcity3.environment.GISFunctions;
import repastcity3.environment.Junction;
import repastcity3.environment.NetworkEdgeCreator;
import repastcity3.environment.SpatialIndexManager;
import repastcity3.environment.contexts.BuildingContext;
import repastcity3.environment.contexts.JunctionContext;
import repastcity3.environment.contexts.RoadContext;
import repastcity3.environment.contexts.RouteContext;
//import repastcity3.exceptions.AgentCreationException;
import repastcity3.exceptions.EnvironmentError;
import repastcity3.exceptions.NoIdentifierException;
import repastcity3.exceptions.ParameterNotFoundException;

import copenhagenabm.loggers.BasicAgentLogger;
import copenhagenabm.loggers.CopenhagenABMLogging;
import copenhagenabm.loggers.DecisionTextLogger;
import copenhagenabm.loggers.ModelInfoLogger;
import copenhagenabm.loggers.PostgresLogger;
import copenhagenabm.loggers.RoadLoadLogger;
import copenhagenabm.loggers.RoadNetworkLinksDumper;
import copenhagenabm.loggers.SuccessPerRouteLogger;

import copenhagenabm.routes.MatchedGPSRoute;
import copenhagenabm.routes.Route;
import copenhagenabm.routes.RouteLogger;

import copenhagenabm.tools.CopenhagenABMTools;
import copenhagenabm.tools.EmailTool2;
import copenhagenabm.tools.RasterSpace;
import copenhagenabm.tools.ShapefileWriter;
import copenhagenabm.tools.SimpleDistance;
import copenhagenabm.tools.SnapTool;

public class ContextManager implements ContextBuilder<Object> {
	
	/**
	 * uniqueModelID, the model run id
	 * used for the postgis loggers in order to isolate model runs
	 * as well used for the loggers
	 */
	private static long uniqueModelID;

	
	private static boolean pgDEBUG_MODE = false;

	private static Stack<CalibrationLet> calibrationLets = new Stack<CalibrationLet>();

	private static CoordinateReferenceSystem crs = null;

	public static SimpleDistance simpleDistance = null;

	/**
	 * initialize the tools we need in all parts of the project
	 */
	private static CopenhagenABMTools copenhagenABMTools = null;

	private static SnapTool snapTool = null;

	private static int agentCounter = 0;

	public static void incrementAgentCounter() {
		agentCounter++;
	}

	public static int getAgentCounter() {
		return agentCounter;
	}

	private static ArrayList<IAgent> deadAgents = new ArrayList<IAgent>();

	/*
	 * An index <indexID, Road>
	 * needed by the near selection routines
	 */
	public static HashMap<Integer, Road> roadIndex = new HashMap<Integer, Road>();

	// TODO: throw this into the settings file
	private static final String TARGET_EPSG = "EPSG:2197";

	/*
	 * A logger for this class. Note that there is a static block that is used to configure all logging for the model
	 * (at the bottom of this file).
	 */
	private static Logger LOGGER = Logger.getLogger(ContextManager.class.getName());


	private static RoadLoadLogger roadLoadLogger;

	/**
	 * successPerRouteLogger
	 * 
	 * Logs the number of successes per route within the calibration mode
	 */
	private static SuccessPerRouteLogger successPerRouteLogger = new SuccessPerRouteLogger();

	/**
	 * 
	 *  A logger for the decision matrix
	 * 
	 */

	private static BasicAgentLogger basicAgentLogger;


	public static BasicAgentLogger getBasicAgentLogger() {
		return basicAgentLogger;
	}

	public static void setBasicAgentLogger(BasicAgentLogger basicAgentLogger) {
		ContextManager.basicAgentLogger = basicAgentLogger;
	}

	/**
	 * 
	 *  A logger for the decision matrix
	 * 
	 */

	private static DecisionTextLogger decisionTextLogger;

	/**
	 * 
	 *  A logger for the kill actions
	 * 
	 */

	/**
	 * 
	 * A logger for the successes within the calibration harvesting 
	 * 
	 */

	private static PostgresLogger postgresLogger = new PostgresLogger();


	// private NearestRoadCoord nearestRoadCoord = new NearestRoadCoord();

	public static PostgresLogger getPostgresLogger() {
		return postgresLogger;
	}

	public static void setPostgresLogger(PostgresLogger postgresLogger) {
		ContextManager.postgresLogger = postgresLogger;
	}

	private static Properties properties;

	/*
	 * The route store contains routes of agent which have terminated, i.e. the route is put into
	 * the store on death of the agent. The HashMap is keyed on the agent's ID.
	 * 
	 */

	//	private static RouteStore routeStore = new RouteStore();

	public static void setProperties(Properties properties) {
		ContextManager.properties = properties;
	}

	/*
	 * The stack contains matchedRoutes, is populated at buildCalibrationModel() and 
	 * is continuosly 
	 * 
	 */
	private static Stack<MatchedGPSRoute> matchedGPSRouteStack = new Stack<MatchedGPSRoute>();


	/*
	 * 
	 * Pointers to contexts and projections (for convenience). Most of these can be made public, but the agent ones
	 * can't be because multi-threaded agents will simultaneously try to call 'move()' and interfere with each other. So
	 * methods like 'moveAgent()' are provided by ContextManager.
	 * 
	 */

	private static Context<Object> mainContext;

	// building context and projection can be public (thread safe) because buildings only queried
	public static Context<Building> buildingContext;
	public static Geography<Building> buildingProjection;

	public static Context<Road> roadContext;
	public static Geography<Road> roadProjection;
	public static Network<Junction> roadNetwork;

	public static Context<Junction> junctionContext;
	public static Geography<Junction> junctionGeography;

	private static Context<Route> routeContext;
	public static Geography<Route> routeGeography;
	public static Geography<Route> routeProjection;

	public static Context<Dot> dotContext;
	public static Geography<Dot> dotGeography;
	public static Geography<Dot> dotProjection;	

	private static Context<Decision> decisionContext;
	public static Geography<Decision> decisionGeography;
	public static Geography<Decision> decisionProjection;	

	private static Context<IAgent> agentContext;
	private static Geography<IAgent> agentGeography;

	private static Geography<Zone> zoneProjection;
	private static Context<Zone> zoneContext;
	private static HashMap<String, Zone> zoneHash = new HashMap<String, Zone>();

	public static Context<MatchedGPSRoute> matchedGPSRouteContext;
	public static Geography<MatchedGPSRoute> matchedGPSRouteProjection;

	private ArrayList<copenhagenabm.environment.MatrixReader.Spawn> spawns;

	static GeometryFactory geomFac = new GeometryFactory();

	private static SpatialIndex roadSpatialIndex; // the spatial index for the road network

	private static SimpleNearestRoadCoordinateCache nearestRoadCoordinateCache;

	private static MatchedRouteCache matchedRouteCache = new MatchedRouteCache();

	/**
	 * The crowding network holds the number of agents per edge at the current moment. 
	 */
	private static RoadNetwork crowdingNetwork;

	//	private static SimpleLoadLogger simpleLoadLogger = new SimpleLoadLogger();

	//	private CrowdingNetworkLogger crowdingNetworkLogger;


	private RouteLogger routeLogger;
	private static Coordinate[] resultRouteCoordinates;
	//	private static Coordinate startCoordinate;
	//	private static Coordinate endCoordinate;
	private int currentObjectID;

	// ** Batch mode parameters
	// the ID of the model run when running in batch mode
	private static Integer modelRunID = 0;

	// private Double angleToDestination = null;

	private static MatchedGPSRoute matchedGPSRoute;

	private EmailTool2 emailTool;

	public EmailTool2 getEmailTool() {
		return emailTool;
	}

	public void setEmailTool(EmailTool2 emailTool) {
		this.emailTool = emailTool;
	}

	private String gisDataDir;

	private boolean inTermination = false;

	private static CopenhagenABMLogging copenhagenABMLogging;

	//	private Integer currrentRoadLoadCounter;

	private static long startTime;

	private static int numberOfKills = 0;

	public static int totalNumberOfKills = 0;

	private static boolean inBatchMode;

	//	private static LoadNetwork loadNetwork = null;
	private static RoadNetwork theRoadNetwork;

	// Rasters describing good and bad experiences
	private static RasterSpace goodRaster;
	private static RasterSpace badRaster;

	public static int orthodromicCounter = 0;

	public static ArrayList<IAgent> agentsToBeRemoved = new ArrayList<IAgent>();
	public static ArrayList<IAgent> agentsToBeSpawned = new ArrayList<IAgent>();

	private static CalibrationModeData calibrationModeData;

	private static int calibrationAgentsToModel; // the current number of agents to model

	private static ModelInfoLogger modelInfoLogger = new ModelInfoLogger();

	public static ArrayList<IAgent> getAgentsToBeSpawned() {
		return agentsToBeSpawned;
	}

	public static void setAgentsToBeSpawned(ArrayList<IAgent> agentsToBeSpawned) {
		ContextManager.agentsToBeSpawned = agentsToBeSpawned;
	}

	public static HashMap<Integer, Road> getRoadindex() {
		return roadIndex;
	}

	/**
	 * returns true when model set to be an explicative model in the properties file.
	 * @return
	 */
	public static boolean inCalibrationMode() {
		return ( ContextManager.getProperty(GlobalVars.inCalibrationMode).equals("true"));
	}


	public void loadExperienceRasters(String gisDataDir) throws IllegalArgumentException, IOException {

		String goodfileName = gisDataDir + ContextManager.getProperty("GoodExperienceRasterfile");
		ContextManager.setGoodRaster(new RasterSpace(goodfileName));

		String badfileName = gisDataDir + ContextManager.getProperty("BadExperienceRasterfile");
		ContextManager.setBadRaster(new RasterSpace(badfileName));

	}

	/*
	 * returns the value of the good raster given a point
	 */
	public static double getGoodValueAt(Coordinate pos) {
		return getGoodRaster().valueAt(pos);
	}

	/*
	 * returns the value of the bad raster given a point
	 */
	public static double getBadValueAt(Coordinate pos) {
		return getBadRaster().valueAt(pos);
	}

	/**
	 * Returns the exaggeration factor for the emotional transsects
	 * @return (double)
	 */
	public static double getKMZExaggerationFactor() {
		String kzef = ContextManager.getProperty("KMZExaggerationFactor");
		return new Double(kzef);
	}

	/**
	 * 
	 * @return
	 */
	public static int getGroupingFactor() {
		return new Integer(ContextManager.getProperty("GroupingFactor"));
	}

	/**
	 * 
	 * create the Roads - context and geography
	 * 
	 * @param gisDataDir
	 * @throws MalformedURLException
	 * @throws FileNotFoundException
	 */
	public void loadRoadContext(String gisDataDir) throws MalformedURLException, FileNotFoundException {

		roadContext = new RoadContext();
		roadProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY, roadContext,
				new GeographyParameters<Road>(new SimpleAdder<Road>()));
		String roadFile = gisDataDir + getProperty(GlobalVars.RoadShapefile);
		GISFunctions.readShapefile(Road.class, roadFile, roadProjection, roadContext);

		// here we try to remove all roads which have no geoemtry

		IndexedIterable<Road> rds = roadContext.getObjects(Road.class);

		for (Road r : rds) {
			Geometry g = r.getGeometry();
			if (g==null) {
				roadContext.remove(r);

			}
			Coordinate c1 = r.getGeometry().getCoordinates()[0]; // First coord
			Coordinate c2 = r.getGeometry().getCoordinates()[r.getGeometry().getNumPoints() - 1]; // Last coord
			if (c1==c2) {
				roadContext.remove(r);
			}

		}

		mainContext.addSubContext(roadContext);

		SpatialIndexManager.createIndex(roadProjection, Road.class);

		LOGGER.log(Level.FINER, "Read " + roadContext.getObjects(Road.class).size() + " roads from " + roadFile);
	}

	public void buildRouteGeography() {

		routeContext = new RouteContext();

		routeProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.ROUTE_GEOGRAPHY, getRouteContext(),
				new GeographyParameters<Route>(new SimpleAdder<Route>()));

		mainContext.addSubContext(getRouteContext());

		GeographyParameters<Route> geoParams = new GeographyParameters<Route>();
		routeGeography = GeographyFactoryFinder.createGeographyFactory(null)
				.createGeography("routeGeography", routeContext, geoParams);

		SpatialIndexManager.createIndex(routeProjection, Route.class);
		LOGGER.log(Level.FINER, "Route geography built");

	}


	/*
	 * 
	 * buildJunctionGeography builds a geography for the junctions which will be used in decision points at stars
	 * 
	 */
	public void buildJunctionGeography() {
		junctionContext = new JunctionContext();
		mainContext.addSubContext(junctionContext);

		junctionGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.JUNCTION_GEOGRAPHY, junctionContext,
				new GeographyParameters<Junction>(new SimpleAdder<Junction>()));

		SpatialIndexManager.createIndex(junctionGeography, Junction.class);
		LOGGER.log(Level.FINER, "Junction geography built");
	}

	/*
	 * build the geography for the dots
	 * 
	 */
	public void buildDotGeography() {
		dotContext = new DotContext();
		mainContext.addSubContext(dotContext);

		dotGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.DOT_GEOGRAPHY, dotContext,
				new GeographyParameters<Dot>(new SimpleAdder<Dot>()));

		SpatialIndexManager.createIndex(dotGeography, Dot.class);
		LOGGER.log(Level.FINER, "Dot geography built");

	}

	/*
	 * build the geography for the decisions
	 * 
	 */
	public void buildDecisionGeography() {
		setDecisionContext(new DecisionContext());
		mainContext.addSubContext(getDecisionContext());

		decisionGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.DECISION_GEOGRAPHY, getDecisionContext(),
				new GeographyParameters<Decision>(new SimpleAdder<Decision>()));

		SpatialIndexManager.createIndex(decisionGeography, Decision.class);
		LOGGER.log(Level.FINER, "Decision geography built");

	}

	/*
	 * Builds the road network from the road layer
	 */
	public void buildRoadNetwork()  {
		// 2. roadNetwork
		NetworkBuilder<Junction> builder = new NetworkBuilder<Junction>(GlobalVars.CONTEXT_NAMES.ROAD_NETWORK,
				junctionContext, false);
		builder.setEdgeCreator(new NetworkEdgeCreator<Junction>());
		roadNetwork = builder.buildNetwork();
		GISFunctions.buildGISRoadNetwork(roadProjection, junctionContext, junctionGeography, roadNetwork);

		// Add the junctions to a spatial index (couldn't do this until the
		// road network had been created).
		SpatialIndexManager.createIndex(junctionGeography, Junction.class);
		LOGGER.log(Level.FINER, "Road Network built");

	}

	/*
	 * loads the matched GPS routes for the explicative model
	 * 
	 */
	public void loadMatchedGPSRoutes(String gisDataDir ) throws MalformedURLException, FileNotFoundException {
		matchedGPSRouteContext = new ResultRouteContext();
		matchedGPSRouteProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.RESULT_ROUTE_CONTEXT, matchedGPSRouteContext,
				new GeographyParameters<MatchedGPSRoute>(new SimpleAdder<MatchedGPSRoute>()));
		String matchedGPSRoutesFile = gisDataDir + getProperty(GlobalVars.ResultRoutesShapefile);
		try {
			GISFunctions.readShapefile(MatchedGPSRoute.class, matchedGPSRoutesFile, matchedGPSRouteProjection, matchedGPSRouteContext);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		mainContext.addSubContext(matchedGPSRouteContext);
		SpatialIndexManager.createIndex(matchedGPSRouteProjection, MatchedGPSRoute.class);
		LOGGER.log(Level.FINER, "Read " + matchedGPSRouteContext.getObjects(MatchedGPSRoute.class).size() + " matched GPS Routes from " + matchedGPSRoutesFile);

	}

	private void createAgentContext() {
		setAgentContext(new AgentContext());
		mainContext.addSubContext(getAgentContext());
		agentGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.AGENT_GEOGRAPHY, getAgentContext(),
				new GeographyParameters<IAgent>(new SimpleAdder<IAgent>()));
	}


	public void loadTools() {

		// 3. Initialize the tools.

		emailTool = new EmailTool2(getNotificationEmailAddress());
		copenhagenABMTools = new CopenhagenABMTools();
		snapTool = new SnapTool();
	}


	@Override
	public Context<Object> build(Context<Object> con) {

		// 1. Read in the model properties form  the properties. 
		try {
			readProperties();
		} catch (IOException ex) {
			throw new RuntimeException("Could not read model properties,  reason: " + ex.toString(), ex);
		}

		final Parameters parameters = RunEnvironment.getInstance().getParameters();

		try {
			setModelRunID(new Integer((Integer) parameters.getValue("model_run")));
		} catch (IllegalParameterException e) {
			setModelRunID(1);
		}

		gisDataDir = ContextManager.getProperty(GlobalVars.GISDataDirectory);

		// 2. set the unique model ID
		setUniqueModelID(System.currentTimeMillis());

		// 3. Load the Tools
		loadTools();

		// 4. Setup loggers 
		this.setupLoggers();

		// let's load the experience rasters
		if (useExperiences()) {

			try {
				loadExperienceRasters(gisDataDir);
			} catch (IllegalArgumentException e1) {

				e1.printStackTrace();
			} catch (IOException e1) {

				e1.printStackTrace();
			}

			LOGGER.log(Level.FINE, "Good/Bad rasters loaded from  " + gisDataDir);
		}

		// Keep a useful static link to the main.resources context
		mainContext = con;

		// This is the name of the 'root'context
		mainContext.setId(GlobalVars.CONTEXT_NAMES.MAIN_CONTEXT);

		// Configure the environment

		buildDotGeography();
		buildDecisionGeography();
		buildRouteGeography();

		// we have to load the road network
		try {
			loadRoadContext(gisDataDir);
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}



		setCrs(ContextManager.roadProjection.getCRS());

		simpleDistance = new SimpleDistance(getCrs(), TARGET_EPSG);

		// build the junctions
		buildJunctionGeography();

		// build the roads
		buildRoadNetwork();

		buildSpatialIndexRoad();


		if (inCalibrationMode()) {

			buildCalibrationModel(gisDataDir);

		} else {

			//			currrentRoadLoadCounter = writeRoadLoadEveryTick();

			buildStandardModel();

		}

		if (ContextManager.isBasicAgentLoggerOn()) {
			ContextManager.basicAgentLogger = new  BasicAgentLogger();
		}

		if (ContextManager.isDecisionTextLoggerOn()) {
			decisionTextLogger = new  DecisionTextLogger();
		}

		if (ContextManager.isRoadToCSVDumperOne()) {
			RoadNetworkLinksDumper rNd = new RoadNetworkLinksDumper();
			rNd.dump();
			System.out.println("dumped");
		}


		return mainContext;
	}

	private void buildStandardModel() {
		// setupCrowdingNetworkDumper();


		// let us empty the road logger dir

		// we instantiate a roadLoadLogger with the given 

		roadLoadLogger = new RoadLoadLogger(ContextManager.getRoadLoadLoggerFolder());

		try {

			// Create the buildings - context and geography projection
			buildingContext = new BuildingContext();
			buildingProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					GlobalVars.CONTEXT_NAMES.BUILDING_GEOGRAPHY, buildingContext,
					new GeographyParameters<Building>(new SimpleAdder<Building>()));
			String buildingFile = gisDataDir + getProperty(GlobalVars.BuildingShapefile);
			GISFunctions.readShapefile(Building.class, buildingFile, buildingProjection, buildingContext);
			mainContext.addSubContext(buildingContext);

			SpatialIndexManager.createIndex(buildingProjection, Building.class);
			LOGGER.log(Level.FINER, "Read " + buildingContext.getObjects(Building.class).size() + " buildings from "
					+ buildingFile);

			// create the zones - context and geography projection
			zoneContext = new ZoneContext();
			zoneProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
					GlobalVars.CONTEXT_NAMES.ZONE_GEOGRAPHY, zoneContext,
					new GeographyParameters<Zone>(new SimpleAdder<Zone>()));
			String zoneFile = gisDataDir + getProperty(GlobalVars.ZoneShapefile);
			GISFunctions.readShapefile(Zone.class, zoneFile, zoneProjection, zoneContext);
			mainContext.addSubContext(zoneContext);
			SpatialIndexManager.createIndex(zoneProjection, Zone.class);
			LOGGER.log(Level.FINER, "Read " + zoneContext.getObjects(Zone.class).size() + " zones from "
					+ zoneFile);

			// Create the zones

			/* 
			 * TODO https://github.com/bsnizek/CopenhagenABM/issues/1
			 * 
			 * This is not very elegantly solved, you should 
			 * create a data layer and do the spatial selection at once
			 * 
			 */
			for (Zone zone : ContextManager.getAllZones()) {
				int cntr = 0;
				Geometry zoneGeometry = ContextManager.zoneProjection.getGeometry(zone);
				for (Building building : ContextManager.getAllBuildings()) {
					Geometry buildingGeometry = ContextManager.buildingProjection.getGeometry(building);
					if (zoneGeometry.contains(buildingGeometry)) {
						zone.addBuilding(building);
						cntr++;
					}
				}
				zoneHash.put(zone.getIdentifier(), zone);
				if (cntr == 0) {
					System.out.println("Zone " + zone.getIdentifier() + " has " + cntr + " buildings.");
				}
			}

			// testEnvironment();

		} catch (MalformedURLException e) {
			LOGGER.log(Level.SEVERE, "", e);

		} catch (NoIdentifierException e) {
			LOGGER.log(Level.SEVERE, "One of the input buildings had no identifier (this should be read"
					+ "from the 'identifier' column in an input GIS file)", e);

		} catch (FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Could not find an input shapefile to read objects from.", e);

		}

		createAgentContext();

		buildRouteGeography();

		createTimeTables();

		createDumpEvents();

		createSchedule();

		setNearestRoadCoordinateCache(new SimpleNearestRoadCoordinateCache(ContextManager.getRoadSpatialIndex(), ContextManager.getRoadindex()));

	}

	/**
	 * Returns the data dir as a String
	 * @return
	 */
	public String getGisDataDir() {
		return this.gisDataDir;
	}

	private void setupLoggers() {

		LOGGER.log(Level.FINE, "Configuring the environment with data from " + this.getGisDataDir());

		copenhagenABMLogging = new CopenhagenABMLogging(getLOGGING_FOLDER());

		// let us set up the decision text logger 

		if (getDecisionTextLogger() != null) {
			try {
				ContextManager.getDecisionTextLogger().setup();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// let us set up the basic agent  logger 
		if (ContextManager.getBasicAgentLogger() != null) {
			try {
				ContextManager.getBasicAgentLogger().setup();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// and the success logger
		try {
			ContextManager.getCalibrationModeData();
			CalibrationModeData.getSuccessLogger().setup();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (ContextManager.isPostgreSQLLoggerOn()) {
			ContextManager.getPostgresLogger().setup();
		}

		if (ContextManager.getSuccessPerRouteLoggerFile() != null) {
			try {
				ContextManager.getSuccessPerRouteLogger().setup();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			ContextManager.getModelInfoLogger().setup();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static CopenhagenABMLogging getCopenhagenABMLogging() {
		return copenhagenABMLogging;
	}

	public void setCopenhagenABMLogging(CopenhagenABMLogging copenhagenABMLogging) {
		ContextManager.copenhagenABMLogging = copenhagenABMLogging;
	}

	private static ModelInfoLogger getModelInfoLogger() {
		return ContextManager.modelInfoLogger ;
	}

	private static SuccessPerRouteLogger getSuccessPerRouteLogger() {
		return successPerRouteLogger;
	}

	private void buildCalibrationModel(String gisDataDir) {

		// Let us load the GPS routes.

		try {
			loadMatchedGPSRoutes(gisDataDir);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// build the calibration model data object
		ContextManager.calibrationModeData = new CalibrationModeData();

		calibrationModeData.setAngleToDestWeight(ContextManager.getAngleToDestination());
		calibrationModeData.setOmitDecisionMatrixMultifields(ContextManager.getOmitDecisionMatrixMultifields());
		calibrationModeData.setStartTime(System.currentTimeMillis());
		setStartTime(System.currentTimeMillis());

		int numberOfRoutes = matchedGPSRouteContext.getObjects(MatchedGPSRoute.class).size();

		calibrationModeData.setTotalNumberOfIterations(numberOfRoutes * getNumberOfRepetitions());

		createAgentContext();

		//		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		theRoadNetwork = new RoadNetwork();

		int tMR = 0;

		// let us populate the matchedGPSRouteStack and the routeStore
		for (MatchedGPSRoute matchedGPSRoute : ContextManager.getMatchedGPSRoutes()) {

			// 1. we put all matchedRoutes into a stack

			int nOE = matchedGPSRoute.getNumberOfEdges();

			if (nOE >= getMinEdgeNumberOfEdgesForCalibration()) {

				ContextManager.getMatchedGPSRouteStack().add(matchedGPSRoute);

			} else {
				ContextManager.getCalibrationModeData().incrementOmittedMatchedRoute();
			}
			tMR = tMR + 1;
		}

		ContextManager.getCalibrationModeData().setTotalMatchedGPSRoutes(tMR);

		System.out.println(ContextManager.getCalibrationModeData().getOmittedMatchedGPSRoutes() + " of " + tMR + " omitted as they were too short.");

		// lets initialize the route logger
		try {
			this.routeLogger = new RouteLogger(ContextManager.getProperty("dumpRouteFile"));
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// lets create the schedule
		createSchedule();

		populateCalibrationLets();

		System.out.println("(" + ContextManager.getCurrentTick()  + ")" + calibrationLets.size() + " calibrationLets left.");

		nextCalibrationAgent();

	}

	/**
	 * Populates the calibrationLets stack.
	 */
	public void populateCalibrationLets() {
		Stack<MatchedGPSRoute> theStack = ContextManager.getMatchedGPSRouteStack();
		while (!theStack.isEmpty()) {
			MatchedGPSRoute gpsRoute = theStack.pop();
			for (int i=0; i<ContextManager.getNumberOfRepetitions();i++)
				ContextManager.calibrationLets.add(new CalibrationLet(gpsRoute, i));		
		}

	}

	public static CalibrationModeData getCalibrationModeData() {
		return calibrationModeData;
	}

	public static void setCalibrationModeData(
			CalibrationModeData calibrationModeData) {
		ContextManager.calibrationModeData = calibrationModeData;
	}

	public static RoadNetwork getTheRoadNetwork() {
		return theRoadNetwork;
	}

	public void setTheRoadNetwork(RoadNetwork theRoadNetwork) {
		ContextManager.theRoadNetwork = theRoadNetwork;
	}

	private void buildSpatialIndexRoad() {

		System.out.println("Populating road segment index.");

		IndexedIterable<Road> roads = ContextManager.roadContext.getObjects(Road.class);
		//		int numberOfRoads = roads.size();

		// 1. initialize the index
		Properties p = new Properties();
		p.setProperty("MinNodeEntries", "1");
		p.setProperty("MaxNodeEntries", Integer.toString(roads.size()));
		setRoadSpatialIndex(SpatialIndexFactory.newInstance("rtree.RTree", p));

		// 2. loop through the edges and add them to the index


		//		int cntr = 1;

		for (Road r : roads) {
			Envelope geom =  r.getGeometry().getEnvelopeInternal();

			double minx = geom.getMinX();
			double miny = geom.getMinY();

			double maxx = geom.getMaxX();
			double maxy = geom.getMaxY();

			Point p1 = null;
			Point p2 = null;
			boolean jump = false;
			try {
				p1 = geomFac.createPoint(new Coordinate(minx,miny));
				p2 = geomFac.createPoint(new Coordinate(maxx, maxy));
			} catch (ArrayIndexOutOfBoundsException e) {
				try {
					System.out.println(geom + " " + r.getIdentifier().toString());
				} catch (NoIdentifierException e1) {
					e1.printStackTrace();
				}
				jump = true;
			}

			if (!jump) {

				Integer ident = null;

				try {
					String id = r.getIdentifier();
					ident = new Integer(id);

					ContextManager.getRoadindex().put(ident, r);

				} catch (NoIdentifierException e) {
					e.printStackTrace();
				}

				getRoadSpatialIndex().add(new Rectangle((float) p1.getX(),
						(float) p1.getY(), 
						(float) p2.getX(), 
						(float) p2.getY()), 
						ident);	

			} else {
				jump = false;
			}

		}

		System.out.println("Road segment index populated.");

	}

	public Road getRoadForCoordinate(Coordinate c) {
		return ContextManager.getSnapTool().getRoadByCoordinate(c);
	}

	public static Road getRoadForBuilding(Building b) {
		return getNearestRoadCoordinateCache().getRoad(b);
	}

	public static Coordinate getRoadCoordinateForBuilding(Building b) {
		return getNearestRoadCoordinateCache().getCoordinate(b);
	}

	public static RoadNetwork getCrowdingNetwork() {
		return crowdingNetwork;
	}

	/**
	 * createDumpEvents() schedules a lot of dump events for the roadloadlogger
	 */
	private void createDumpEvents() {

		System.out.println("--- Scheduling dump events");

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		// schedule a drop at every drop tick
		schedule.schedule(ScheduleParameters.createRepeating(0, ContextManager.writeRoadLoadEveryTick(),
				ScheduleParameters.FIRST_PRIORITY), this,
				"roadLoadLoggerDrop");

		// and one finally when the model is terminated
		schedule.schedule(ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY), this, "finalRoadLoadLoggerDrop");

	}

	public void finalRoadLoadLoggerDrop() {

		RoadLoadLogger roadLogger = ContextManager.getRoadLoadLogger();

		if (roadLogger != null) {

			int tick = ContextManager.getCurrentTick();
			ContextManager.getRoadLoadLogger().dump(tick);
			System.out.println("Dumped into RoadLoadLogger at " + tick + ".");

			//			roadLogger.dumpWholeDay();

			roadLogger.dumpSimpleWholeDay();

			System.out.println("Final Road Load Logger dumped.");
		}

	}

	public void roadLoadLoggerDrop() {
		int tick = ContextManager.getCurrentTick();

		if (tick>0) {

			ContextManager.getRoadLoadLogger().dump(tick);
			System.out.println("Dumped into RoadLoadLogger at " + tick + ".");

		}
	}

	private void createTimeTables() {

		int spawnCounter = 0;

		String gisDataDir = ContextManager.getProperty(GlobalVars.GISDataDirectory);
		String mtrxFile = gisDataDir + getProperty(GlobalVars.MatrixFile);

		try {
			MatrixReader mr = new MatrixReader(mtrxFile);
			this.spawns = mr.getSpawns();
		} catch (IOException e) {

			e.printStackTrace();
		} 

		// System.out.println("===" + this.spawns.size() + " agents to be spawned.");

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		ArrayList<String> zonesNotFound = new ArrayList<String>();

		for (MatrixReader.Spawn spawn: this.spawns) {
			schedule.schedule(ScheduleParameters.createOneTime(spawn.getSpawnAtTick(), 
					ScheduleParameters.LAST_PRIORITY, 1), this, 
					"spawnAgent", spawn.getZoneFrom(), spawn.getZoneTo());

			System.out.println("(" + spawnCounter + ") SPAWN AT " + spawn.getSpawnAtTick());

			if (ContextManager.getZoneByID(spawn.getZoneFrom())==null) {
				if (!zonesNotFound.contains(spawn.getZoneFrom())) {
					zonesNotFound.add(spawn.getZoneFrom());
				}

			}

			spawnCounter++;

		}


		for (String s : zonesNotFound) {
			System.out.println("NOT IN .shp: " + s);
		}

		System.out.println(spawnCounter + " SPAWNS" );

	}

	public void spawnAgent(String zFrom, String zTo) {

		AgentFactory agentFactory = new AgentFactory();

		agentFactory.createAgent(zFrom, zTo);

	}

	public void spawnCalibrationAgent(CalibrationLet c) {
		MatchedGPSRoute gpsRoute = c.getGpsRoute();
		AgentFactory agentFactory = new AgentFactory();
		agentFactory.createAgent(
				gpsRoute.getOrigin(),
				gpsRoute.getDestination(),
				gpsRoute,
				c.getIteration());	
	}

	//	/**
	//	 * 
	//	 * Spawn a calibration agent
	//	 * 
	//	 * @param from
	//	 * @param to
	//	 * @param resultRouteID
	//	 * @param matchedGPSRoute
	//	 */
	//	public void spawnAgentByCoordinates(Coordinate from, Coordinate to, 
	//			int resultRouteID, MatchedGPSRoute matchedGPSRoute) {
	//
	//		AgentFactory agentFactory = new AgentFactory();
	//
	//		agentFactory.createAgent(
	//				from, 
	//				to,
	//				matchedGPSRoute, 
	//				ContextManager.getCalibrationModeData().getCurrentNIter() //the index of the current iteration
	//				);
	//
	//	}




	/**
	 * Returns true every time a division of currentStep by stepToFireOn returns 0.0d
	 * @param tickToFireOn
	 * @param currentTick
	 * @return
	 */
	public static boolean fireAtEvery(int tickToFireOn, int currentTick) {
		double xx = currentTick / (tickToFireOn * 1.0d);
		return ((Math.round(xx) - xx) ==0.0d);
	}

	/**
	 * Terminates the model and writes the following to the disk
	 * 	
	 * @param currentTick
	 */
	public void terminateModel(int currentTick) {

		inTermination = true;

		this.removeAgentsToBeRemoved(); // remove the last agent

		RunEnvironment.getInstance().pauseRun();

		// deprecate this one
		this.terminateLoggers();

		System.out.println("Terminating model ... ");

		RunEnvironment.getInstance().endAt(currentTick);

		if (ContextManager.inCalibrationMode()) {

			System.out.println(ContextManager.getCalibrationModeData());

			// writeSuccessRateLog();

			ContextManager.getCalibrationModeData().setRunTime(getModelRunSeconds());

			ContextManager.getCalibrationModeData().log_success();

			ContextManager.getCalibrationModeData().logCalibrationRoutes();

			ContextManager.getCalibrationModeData().closeCalibrationRouteLogger();

			ContextManager.getSuccessPerRouteLogger().log();

			ContextManager.getSuccessPerRouteLogger().close();

		}

		// terminate the PostgreSQL logger

		if (ContextManager.getPostgresLogger() != null) {
			postgresLogger.close();
		}

		ContextManager.getCopenhagenABMLogging().close();

		System.out.println("Model terminated in " + getFormattedModelRunSeconds() + " seconds.");

		emailTool.sendMail("The model finished in " + getFormattedModelRunSeconds() +  " seconds");

	}

	public static String getFormattedModelRunSeconds() {
		long endTime = System.currentTimeMillis();
		long millis = (endTime - ContextManager.getStartTime());
		Date date = new Date(millis);
		return new SimpleDateFormat("HH:mm:ss").format(date); // 9:00

	}

	public static long getModelRunSeconds() {
		long endTime = System.currentTimeMillis();
		long millis = (endTime - ContextManager.getStartTime());
		return millis / 1000;
	}

	//	/*
	//	 * 
	//	 */
	//	private void writeSuccessRateLog() {
	//		ContextManager.getCalibrationModeData();
	//		ContextManager.getCalibrationModeData();
	//		CalibrationModeData.getSuccessLogger().logLine(ContextManager.getAngleToDestination() + ";" + 
	//				ContextManager.omitDecisionMatrixMultifields() + ";" + 
	//				ContextManager.getCalibrationModeData().getTotalNumberOfIterations() + ";" + 
	//				CalibrationModeData.getCanceledAgents() + ";" + 
	//				ContextManager.getModelRunSeconds());
	//	}

	public void writeRouteContext() {


		//		Layer l = routeProjection.getLayer(copenhagenabm.routes.Route.class);
		//		
		//		Iterable<Route> objs = routeProjection.getAllObjects();
		//
		//		for (Route r : objs) {
		//			System.out.println(r.getLength());
		//		}

		ShapefileWriter shfw = new ShapefileWriter(routeGeography);

		URL url = null;
		try {
			url = new File(ContextManager.getProperty("dumpRouteFile")).toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		//		shfw.write("copenhagenabm.routes.Route.FeatureType", url);

		//		Route arg0=null;;
		//		boolean geomType = routeContext.add(arg0);

		shfw.write(routeGeography.getLayer(copenhagenabm.routes.Route.class).getName(), url);
	}

	public void writeJunctions() {

	}

	public void writeRoadContext() {
		ShapefileWriter shfw = new ShapefileWriter(roadProjection);

		URL url = null;
		try {
			url = new File(ContextManager.getProperty("dumpRoadFile")).toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shfw.write("copenhagenabm.environment.Road.FeatureType", url);
	}

	public void writeDecisionContext() {
		ShapefileWriter shfw = new ShapefileWriter(decisionProjection);

		URL url = null;
		try {
			url = new File(ContextManager.getProperty("dumpDecisionFile")).toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shfw.write("copenhagenabm.agenty.Decision.FeatureType", url);
	}

	/*
	 * schedules the next calibration agent
	 */
	public void scheduleNewCalibrationAgent(int currentTick) {

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		CalibrationLet cl = calibrationLets.pop();

		schedule.schedule(ScheduleParameters.createOneTime(currentTick + 1,
				ScheduleParameters.LAST_PRIORITY, 1), this,
				"spawnCalibrationAgent", cl);

		int gPSRouteID = cl.getGpsRoute().getOBJECTID();

		System.out.println("(" + ContextManager.getCurrentTick() + ") " + "Agent scheduled for GPS("+ gPSRouteID +") at " + (currentTick + 1));

	}

	public synchronized static int getCurrentTick() {

		RunEnvironment instance = RunEnvironment.getInstance();

		// if there's no environment we just return 0 need that for test reasons
		if (instance == null) {
			return 0;
		} else 

			return new Integer((int) instance.getCurrentSchedule().getTickCount());
	}


	public synchronized void stepStandardAgents() throws Exception {

		int currentTick = getCurrentTick();

		//		ContextManager.getRoadLoadLogger().tick(currentTick);

		Iterable<IAgent> agents = agentGeography.getAllObjects();

		int n = 0;

		for (IAgent cphA : agents) {

			n++;

			if (cphA == null) {
				System.out.println("agent = null(ContextManager, #1308");
			} else {

				cphA.step();

			}
		}

		System.out.println("TICK " + currentTick + " - " + n + " agents.");

		// let us commit to the postgreSQL database
		if (isPostgreSQLLoggerOn()) {
			int tickToFireOn = ContextManager.dumpAtEveryTick();
			if (ContextManager.fireAtEvery(tickToFireOn, currentTick)) {
				ContextManager.getPostgresLogger().commit();
			}

		}

	}


	public void stepCalibrationAgents() throws Exception {

		Iterable<IAgent> agents = agentGeography.getAllObjects();

		for (IAgent cphA : agents) {

			if (ContextManager.getPOSITION_DEBUG_MODE()) {
				System.out.println("(" + ContextManager.getCurrentTick() + ") - A(" + cphA.getID() + "/" + currentObjectID + ") " + cphA.getPosition());
			}

			if (cphA.isAtDestination()) {


				//				System.out.println("(" + ContextManager.getCurrentTick() + ") - A(" + cphA.getID() + ") is prepared for removal CM(#1448)");
				cphA.prepareForRemoval(true);


			} else {

				if (cphA.isMoreThan50PercentOverGPSRouteDistance()) {

					if (ContextManager.getDEBUG_MODE()) {
						System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + cphA.getID() + ") > 50%");
					}

					cphA.setDidNotFindDestination(true);

					cphA.setDeathLocation(cphA.getPosition());

					ContextManager.getCalibrationModeData().incrementNumberOfCanceledAgents();

					Route route = cphA.getRoute();
					ContextManager.getRouteContext().add(route);

					ContextManager.removeAgent(cphA);

					//					nextCalibrationAgent();

				} else {

					cphA.step();
				}
			}
		}
	}



	/**
	 * @return the number of iterations still to do at a certain moment
	 */
	public static int getCalibrationAgentsToModel() {
		return calibrationAgentsToModel;
	}

	public void jumpToNextRoute() {

	}


	//	/**
	//	 * jumpToNextRoute() gets a route from the route stack and spawns its first agent
	//	 * 
	//	 * @param currentTick
	//	 */
	//	public  void DEPR_jumpToNextRoute(int currentTick) {
	//
	//		if (getMatchedGPSRouteStack().size() > 0) {
	//
	//			ContextManager.setMatchedGPSRoute(getMatchedGPSRouteStack().pop());
	//
	//			// figure out whether the route is longer than 3 road segments:
	//
	//			//			int numberOfRoadSegments = ContextManager.getMatchedGPSRoute().getNumberOfEdges();
	//
	//
	//			ContextManager.setResultRouteCoordinates(ContextManager.matchedGPSRouteProjection.getGeometry(ContextManager.getMatchedGPSRoute()).getCoordinates());
	//			ContextManager.setStartCoordinate(getResultRouteCoordinates()[0]);
	//			ContextManager.setEndCoordinate(getResultRouteCoordinates()[getResultRouteCoordinates().length-1]);
	//
	//
	//			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
	//
	//			schedule.schedule(ScheduleParameters.createOneTime(currentTick + 1, 
	//					ScheduleParameters.LAST_PRIORITY, 1), this, 
	//					"spawnAgentByCoordinates", 
	//					getStartCoordinate(), 
	//					getEndCoordinate(), 
	//					currentObjectID,
	//					getMatchedGPSRoute());
	//
	//			setCalibrationAgentsToModel(ContextManager.getNumberOfRepetitions());
	//
	//			currentObjectID = ContextManager.getMatchedGPSRoute().getOBJECTID(); 
	//
	//			//			System.out.println("(" + getCurrentTick() + ") New agent with riD=" + currentObjectID + " scheduled - " + getMatchedGPSRouteStack().size() + " to go.");
	//
	//
	//		}
	//		else {
	//			this.terminateModel(currentTick);
	//		}
	//
	//	}

	/**
	 * nextCalibrationAgent
	 * 
	 * spawns the next agent if there's 'lets' in the queue, otherwise it
	 * terminates the model.
	 */
	public void nextCalibrationAgent() {

		if (!inTermination) {

			if (!calibrationLets.empty()) {

				ContextManager.getCalibrationModeData().incrementCurrentNIter();

				scheduleNewCalibrationAgent(getCurrentTick());

			} else {

				this.terminateModel(ContextManager.getCurrentTick() + 1000);

			}
		}

	}


	//	public void killAgent(IAgent agent) {
	//
	//		totalNumberOfKills++;
	//		//		ContextManager.getKillLogger().logLine(agent.getPosition().x + ";" + agent.getPosition().y + ";" + agent.getID() + ";" + getMatchedGPSRoute().getOBJECTID());
	//		ContextManager.incrementNumberOfKills();
	//		getAgentContext().remove(agent);
	//	}

	/**
	 * 
	 * removeAgentsToBeRemoved()
	 * 
	 * Loops through agentsToBeRemoved and remove the agents to be removed.
	 * 
	 * Calibration agents are finished up prior to removal.
	 * 
	 * In the end it schedules a new calibration agent
	 * 
	 */
	public void removeAgentsToBeRemoved() {

		if (agentsToBeRemoved.size()>0) {

			// let us check whether there are doubles 
			Set<IAgent> set = new HashSet<IAgent>(agentsToBeRemoved);

			if(set.size() < agentsToBeRemoved.size()){
				System.out.println("DUPLICATES !!!");
			}

			Context<IAgent> ac = getAgentContext();

			for (IAgent a : agentsToBeRemoved) {


				if ((a!=null) && getAgentContext().contains(a)) {

					if (a.isCalibrationAgent()) {

						// calculate the overlap between the GPS route and the modeled one and store it on the agent (overlap)
						a.calcOverlap();

						// write the history
						//						a.writeHistory(ContextManager.getModelRunID());

						a.finishCalibrationData();

						// remove the agent from the context etc. 
						ac.remove(a);

						ContextManager.getDeadAgents().add(a);

						if (a.isSuccessful())
							System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + a.getID() + ") removed (successful).");
						else
							System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + a.getID() + ") removed (not succesful).");


					} else {

						try {
							a.logBasics();

							ac.remove(a);
							// System.out.print(a);
						} catch (java.lang.NullPointerException npe) {
							System.out.println("ERROR removing " + a);
							npe.printStackTrace();
						}
						a = null;
					}
				}

				if (ContextManager.inCalibrationMode()) {

					nextCalibrationAgent();

				}

			}

			agentsToBeRemoved = new ArrayList<IAgent>();

		}


	}

	//	private void decreaseCalibrationAgentsToModel() {
	//		System.out.println("Decreasing calibrationAgentsToModel");
	//		setCalibrationAgentsToModel(ContextManager.getCalibrationAgentsToModel() - 1);
	//
	//	}

	@SuppressWarnings("unchecked")
	public void spawnAgents() {
		ArrayList<IAgent> aTBS = ContextManager.getAgentsToBeSpawned();

		//		if (aTBS.size()>1) {
		//			System.out.println("!!! MORE THAN ONE AGENT !!!");
		//		}

		if (aTBS.size()>0) {

			if (aTBS.contains(null)) {
				System.out.println("spawnAgents() : null contained in list");
			}

			for (IAgent a : (ArrayList<IAgent>) aTBS.clone()) {
				if (a==null) {
					System.out.println("**** ");
				} else {
					if (!ContextManager.agentsToBeRemoved.contains(a)) {
						addAgentToContext(a);
						a.snapAgentToRoad();
						//				removeAgentFromAgentsToBeSpawned(a);
					}
				}
			}
			ContextManager.agentsToBeSpawned = new ArrayList<IAgent>();
		}
	}

	public void removeAgentFromAgentsToBeSpawned(IAgent agent) {
		ContextManager.getAgentsToBeSpawned().remove(agent);
	}

	public static int getEndTime() {
		return new Integer(ContextManager.getProperty("EndTime"));
	}

	public void stepAgents() {

		spawnAgents();

		int currentTick = getCurrentTick();
		int terminationTick = new Integer((int) (ContextManager.getEndTime() ));

		if (currentTick > terminationTick && !ContextManager.inCalibrationMode()) {
			terminateModel(currentTick);
		}



		if (ContextManager.inCalibrationMode()) {

			if (agentContext.getObjects(IAgent.class).size()>1) {
				System.out.println("!!! MORE THAN ONE AGENT !!!");
			}

			try {
				stepCalibrationAgents();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				stepStandardAgents();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// finally we remove the agents scheduled for removal
		removeAgentsToBeRemoved();

	}


	public static void removeAgent(IAgent cphA) {
		agentsToBeRemoved.add(cphA);
	}

	private void terminateLoggers() {

		if (this.routeLogger != null) {
			this.routeLogger.close();

		}

		LOGGER.log(Level.FINER, "Loggers terminated.");



		ContextManager.getModelInfoLogger().log(ContextManager.getCalibrationModeData());
		ContextManager.getModelInfoLogger().close();

	}

	/**
	 * Returns the number of agents on a road at the current tick.
	 * Returns 0 when in explicative mode. 
	 * @param r
	 * @return
	 */
	public static int getCurrentCrowdingN(Road r) {

		if (!ContextManager.inCalibrationMode()) return ContextManager.crowdingNetwork.getRoadLoad(r); else 

			return 0;
	}

	private void createSchedule() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();

		schedule.schedule(ScheduleParameters.createRepeating(1, 1, 0), this,
				"stepAgents");

	}

	public void printTicks() {

		LOGGER.info("Iteration: " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount() 

				);
	}

	/**
	 * Convenience function to get a Simphony parameter
	 * 
	 * @param <T>
	 *            The type of the parameter
	 * @param paramName
	 *            The name of the parameter
	 * @return The parameter.
	 * @throws ParameterNotFoundException
	 *             If the parameter could not be found.
	 */
	public static <V> V getParameter(String paramName) throws ParameterNotFoundException {
		Parameters p = RunEnvironment.getInstance().getParameters();
		Object val = p.getValue(paramName);

		if (val == null) {
			throw new ParameterNotFoundException(paramName);
		}

		// Try to cast the value and return it
		@SuppressWarnings("unchecked")
		V value = (V) val;
		return value;
	}

	/**
	 * Get the value of a property in the properties file. If the input is empty or null or if there is no property with
	 * a matching name, throw a RuntimeException.
	 * 
	 * @param property
	 *            The property to look for.
	 * @return A value for the property with the given name.
	 */
	public static String getProperty(String property) {
		// System.out.println("***" + property);
		if (property == null || property.equals("")) {
			throw new RuntimeException("getProperty() error, input parameter (" + property + ") is "
					+ (property == null ? "null" : "empty"));
		} else {
			String val = ContextManager.properties.getProperty(property);
			if (val == null || val.equals("")) { // No value exists in the
				// properties file
				throw new RuntimeException("checkProperty() error, the required property (" + property + ") is "
						+ (property == null ? "null" : "empty"));
			}
			return val;
		}
	}

	/**
	 * Read the properties file and add properties. Will check if any properties have been included on the command line
	 * as well as in the properties file, in these cases the entries in the properties file are ignored in preference
	 * for those specified on the command line.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void readProperties() throws FileNotFoundException, IOException {

		File propFile = new File("./copenhagenabm.properties");
		if (!propFile.exists()) {
			throw new FileNotFoundException("Could not find properties file in the default location: "
					+ propFile.getAbsolutePath());
		}

		LOGGER.log(Level.FINE, "Initialising properties from file " + propFile.toString());

		ContextManager.properties = new Properties();

		FileInputStream in = new FileInputStream(propFile.getAbsolutePath());
		ContextManager.properties.load(in);
		in.close();

		// See if any properties are being overridden by command-line arguments
		for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
			String k = (String) e.nextElement();
			String newVal = System.getProperty(k);
			if (newVal != null) {
				// The system property has the same name as the one from the
				// properties file, replace the one in the properties file.
				LOGGER.log(Level.INFO, "Found a system property '" + k + "->" + newVal
						+ "' which matches a NeissModel property '" + k + "->" + properties.getProperty(k)
						+ "', replacing the non-system one.");
				properties.setProperty(k, newVal);
			}
		} // for
		return;
	} // readProperties

	/**
	 * Check that the environment looks ok
	 * 
	 * @throws NoIdentifierException
	 */
	//	private void testEnvironment() throws EnvironmentError, NoIdentifierException {
	//
	//		LOGGER.log(Level.FINE, "Testing the environment");
	//		// Get copies of the contexts/projections from main.resources context
	//		// Context<Building> bc = (Context<Building>) mainContext.getSubContext(GlobalVars.CONTEXT_NAMES.BUILDING_CONTEXT);
	//		// Context<Road> rc = (Context<Road>) mainContext.getSubContext(GlobalVars.CONTEXT_NAMES.ROAD_CONTEXT);
	// Context<Junction> jc = (Context<Junction>) mainContext.getSubContext(GlobalVars.CONTEXT_NAMES.JUNCTION_CONTEXT);

	// Geography<Building> bg = (Geography<Building>)
	// bc.getProjection(GlobalVars.CONTEXT_NAMES.BUILDING_GEOGRAPHY);
	// Geography<Road> rg = (Geography<Road>)
	// rc.getProjection(GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY);
	// Geography<Junction> jg = (Geography<Junction>)
	// rc.getProjection(GlobalVars.CONTEXT_NAMES.JUNCTION_GEOGRAPHY);
	// Network<Junction> rn = (Network<Junction>) jc.getProjection(GlobalVars.CONTEXT_NAMES.ROAD_NETWORK);

	// 1. Check that there are some objects in each of the contexts
	// checkSize(bc, rc, jc);

	//		// 2. Check that the number of roads matches the number of edges
	//		if (sizeOfIterable(rc.getObjects(Road.class)) != sizeOfIterable(rn.getEdges())) {
	//			throw new EnvironmentError("There should be equal numbers of roads in the road "
	//					+ "context and edges in the road network. But there are "
	//					+ sizeOfIterable(rc.getObjects(Road.class)) + " and " + sizeOfIterable(rn.getEdges()));
	//		}
	//
	//		// 3. Check that the number of junctions matches the number of nodes
	//		if (sizeOfIterable(jc.getObjects(Junction.class)) != sizeOfIterable(rn.getNodes())) {
	//			throw new EnvironmentError("There should be equal numbers of junctions in the junction "
	//					+ "context and nodes in the road network. But there are "
	//					+ sizeOfIterable(jc.getObjects(Junction.class)) + " and " + sizeOfIterable(rn.getNodes()));
	//		}
	//
	//		LOGGER.log(Level.FINE, "The road network has " + sizeOfIterable(rn.getNodes()) + " nodes and "
	//				+ sizeOfIterable(rn.getEdges()) + " edges.");
	//
	//		// 4. Check that Roads and Buildings have unique identifiers
	//		HashMap<String, ?> idList = new HashMap<String, Object>();
	//		for (Building b : bc.getObjects(Building.class)) {
	//			if (idList.containsKey(b.getIdentifier()))
	//				throw new EnvironmentError("More than one building found with id " + b.getIdentifier());
	//			idList.put(b.getIdentifier(), null);
	//		}
	//		idList.clear();
	//		for (Road r : rc.getObjects(Road.class)) {
	//			if (idList.containsKey(r.getIdentifier()))
	//				throw new EnvironmentError("More than one building found with id " + r.getIdentifier());
	//			idList.put(r.getIdentifier(), null);
	//		}
	//
	//	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int sizeOfIterable(Iterable i) {
		int size = 0;
		Iterator<Object> it = i.iterator();
		while (it.hasNext()) {
			size++;
			it.next();
		}
		return size;
	}

	/**
	 * Checks that the given <code>Context</code>s have more than zero objects in them
	 * 
	 * @param contexts
	 * @throws EnvironmentError
	 */
	public void checkSize(Context<?>... contexts) throws EnvironmentError {
		for (Context<?> c : contexts) {
			int numObjs = sizeOfIterable(c.getObjects(Object.class));
			if (numObjs == 0) {
				throw new EnvironmentError("There are no objects in the context: " + c.getId().toString());
			}
		}
	}

	public static void stopSim(Exception ex, Class<?> clazz) {
		ISchedule sched = RunEnvironment.getInstance().getCurrentSchedule();
		sched.setFinishing(true);
		sched.executeEndActions();
		LOGGER.log(Level.SEVERE, "ContextManager has been told to stop by " + clazz.getName(), ex);
	}

	/**
	 * Move an agent by a vector. This method is required -- rather than giving agents direct access to the
	 * agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @param agent
	 *            The agent to move.
	 * @param distToTravel
	 *            The distance that they will travel
	 * @param angle
	 *            The angle at which to travel.
	 * @see Geography
	 */
	public static synchronized void moveAgentByVector(IAgent agent, double distToTravel, double angle) {
		ContextManager.agentGeography.moveByVector(agent, distToTravel, angle);
	}

	/**
	 * Move an agent. This method is required -- rather than giving agents direct access to the agentGeography --
	 * because when multiple threads are used they can interfere with each other and agents end up moving incorrectly.
	 * 
	 * @param agent
	 *            The agent to move.
	 * @param point
	 *            The point to move the agent to
	 */
	public static synchronized void moveAgent(IAgent agent, Point point) {
		ContextManager.agentGeography.move(agent, point);
	}

	/**
	 * Add an agent to the agent context. This method is required -- rather than giving agents direct access to the
	 * agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @param agent
	 *            The agent to add.
	 */
	public static synchronized void addAgentToContext(IAgent agent) {
		if (agent==null) {
			System.out.println("Agent= null #1964");
		}
		ContextManager.getAgentContext().add(agent);
	}

	/**
	 * Get all the agents in the agent context. This method is required -- rather than giving agents direct access to
	 * the agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @return An iterable over all agents, chosen in a random order. See the <code>getRandomObjects</code> function in
	 *         <code>DefaultContext</code>
	 * @see DefaultContext
	 */
	public static synchronized Iterable<IAgent> getAllAgents() {
		return ContextManager.getAgentContext().getRandomObjects(IAgent.class, ContextManager.getAgentContext().size());
	}

	/**
	 * Get the geometry of the given agent. This method is required -- rather than giving agents direct access to the
	 * agentGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 */
	public static synchronized Geometry getAgentGeometry(IAgent agent) {
		return ContextManager.agentGeography.getGeometry(agent);
	}

	/**
	 * Get a pointer to the agent context.
	 * 
	 * <p>
	 * Warning: accessing the context directly is not thread safe so this should be used with care. The functions
	 * <code>getAllAgents()</code> and <code>getAgentGeometry()</code> can be used to query the agent context or
	 * projection.
	 * </p>
	 */
	public static Context<IAgent> getAgentContext() {
		return ContextManager.agentContext;
	}

	/**
	 * Get a pointer to the agent geography.
	 * 
	 * <p>
	 * Warning: accessing the context directly is not thread safe so this should be used with care. The functions
	 * <code>getAllAgents()</code> and <code>getAgentGeometry()</code> can be used to query the agent context or
	 * projection.
	 * </p>
	 */
	public static Geography<IAgent> getAgentGeography() {
		return ContextManager.agentGeography;
	}

	/**
	 * Get all the zones in the zone context. This method is required -- rather than giving agents direct access to
	 * the zoneGeography -- because when multiple threads are used they can interfere with each other and agents end up
	 * moving incorrectly.
	 * 
	 * @return An iterable over all zones, chosen in a random order. See the <code>getRandomObjects</code> function in
	 *         <code>DefaultContext</code>
	 * @see DefaultContext
	 */
	public static synchronized Iterable<Zone> getAllZones() {
		return zoneContext.getRandomObjects(Zone.class, zoneContext.size());
		// return ContextManager.agentContext.getRandomObjects(IAgent.class, ContextManager.agentContext.size());
	}

	/*
	 * returns an iterable over all result routes
	 */
	public static synchronized Iterable<MatchedGPSRoute> getMatchedGPSRoutes() {
		return matchedGPSRouteContext.getRandomObjects(MatchedGPSRoute.class, matchedGPSRouteContext.size());
		// return ContextManager.agentContext.getRandomObjects(IAgent.class, ContextManager.agentContext.size());
	}


	public static synchronized Iterable<Building> getAllBuildings() {
		return buildingContext.getObjects(Building.class);
		// return ContextManager.zoneContext.getRandomObjects(Building.class, ContextManager.buildingContext.size());
	}

	public static Zone getZoneByID(String zoneID) {
		return zoneHash.get(zoneID);
	}

	public static SimpleNearestRoadCoordinateCache getNearestRoadCoordinateCache() {
		return nearestRoadCoordinateCache;
	}

	public static void setNearestRoadCoordinateCache(
			SimpleNearestRoadCoordinateCache nearestRoadCoordinateCache) {
		ContextManager.nearestRoadCoordinateCache = nearestRoadCoordinateCache;
	}

	//	public NearestRoadCoord getNearestRoadCoord() {
	//		return nearestRoadCoord;
	//	}
	//
	//	public void setNearestRoadCoord(NearestRoadCoord nearestRoadCoord) {
	//		this.nearestRoadCoord = nearestRoadCoord;
	//	}

	// move all orthodromic stuff here
	public double getOrthodromicDistance(Coordinate c1, Coordinate c2) {
		GeodeticCalculator calculator = new GeodeticCalculator(ContextManager.roadProjection.getCRS());
		calculator.setStartingGeographicPoint(c1.x, c1.y);
		calculator.setDestinationGeographicPoint(c2.x, c2.y);
		// System.out.println(c2.distance(c1) + " - " + calculator.getOrthodromicDistance());
		return calculator.getOrthodromicDistance();
	}

	public static RasterSpace getBadRaster() {
		return badRaster;
	}

	public static void setBadRaster(RasterSpace badRaster) {
		ContextManager.badRaster = badRaster;
	}

	public static RasterSpace getGoodRaster() {
		return goodRaster;
	}

	public static void setGoodRaster(RasterSpace goodRaster) {
		ContextManager.goodRaster = goodRaster;
	}

	public static SpatialIndex getRoadSpatialIndex() {
		return roadSpatialIndex;
	}

	public static void setRoadSpatialIndex(SpatialIndex roadSpatialIndex) {
		ContextManager.roadSpatialIndex = roadSpatialIndex;
	}

	/**
	 * 
	 * adds an explicative to the store
	 * 
	 * @param r
	 * @param agentID
	 */
	public static void addExplicativeRouteToStore(Route r, int agentID) {
		// ContextManager.routeStore.addExplicativeRoute(agentID,r);
	}


	public static boolean getDumpIntoRouteFile() {
		return getProperty("dumpIntoRouteFile").equals("true");
	}


	/**
	 * @return 
	 */
	public static String getPathSizeSetFile() {
		return getProperty("pathSizeSetFile");
	}

	//	public static RouteStore getRouteStore() {
	//		return ContextManager.routeStore;
	//	}

	public static CopenhagenABMTools getCopenhagenABMTools() {
		return copenhagenABMTools;
	}

	public void setCopenhagenABMTools(CopenhagenABMTools copenhagenABMTools) {
		ContextManager.copenhagenABMTools = copenhagenABMTools;
	}

	public static SnapTool getSnapTool() {
		return snapTool;
	}

	public static void setSnapTool(SnapTool snapTool) {
		ContextManager.snapTool = snapTool;
	}

	/**
	 *  Returns the matrix decision strategy from the enumeration MATRIX_TYPES
	 * @return
	 */
	public static MATRIX_TYPES getDecisionMatrixStrategy() {
		if (getProperty("DecisionMatrixStrategy").equalsIgnoreCase("ADDITION")) return MATRIX_TYPES.ADDITION; else return MATRIX_TYPES.MULTIPLICATION;	
	}

	public static STOCHASTICTY_TYPES getDecisionMatrixStochasticity() {
		if (getProperty("DecisionMatrixStochasticity").equalsIgnoreCase("ON")) return STOCHASTICTY_TYPES.ON; else return STOCHASTICTY_TYPES.OFF;
	}

	public static AGENT_SPEED_MODES getAgentSpeedMode() {
		if (getProperty("AgentSpeedMode").equals("STATIC")) return AGENT_SPEED_MODES.STATIC; else return AGENT_SPEED_MODES.DYNAMIC;
	}

	public static double getAgentSpeed() {
		return new Double(getProperty("AgentSpeed"));
	}

	public static boolean dumpAgentHistoryIntoDotFile() {
		return getProperty("dumpAgentHistoryIntoDotFile").equalsIgnoreCase("true");
	}

	public static boolean dumpIntoKMLFile() {
		return getProperty("dumpAgentHistoryIntoKMLFile").equalsIgnoreCase("true");
	}

	//	public static void logDecision(List<Road> roads, Road newRoad, int agentID) {
	//		try {
	//			ContextManager.getDecisionLogger().logDecision(roads, newRoad, agentID);
	//		} catch (NumberFormatException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		} catch (FactoryConfigurationError e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		} catch (SchemaException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		} catch (IllegalAttributeException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		} catch (NoIdentifierException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//
	//	}

	/**
	 * returns the step length in meters ? 
	 * @return
	 */
	public static double getStepLength() {
		return new Double((getProperty("StepLength")));
	}

	//	private static DecisionLogger getDecisionLogger() {
	//		return decisionLogger;
	//	}

	public static boolean getOmitDecisionMatrixMultifields() {
		
		String oDM = getProperty("OmitDecisionMatrixMultifields");

		return (oDM.equalsIgnoreCase("ON"));
	}


	public static boolean isDecisionLoggerOn() {
		return (getProperty("DecisionLogger").equalsIgnoreCase("ON"));
	}

	public static boolean isDecisionTextLoggerOn() {
		return (getProperty("DecisionTextLogger").equalsIgnoreCase("ON"));
	}

	public static boolean useExperiences() {
		return (getProperty("UseExperiences").equalsIgnoreCase("ON"));
	}


	public static boolean getDecisionMatrixDebugMode() {
		return (getProperty("DecisionMatrixDebugMode").equalsIgnoreCase("true"));
	}

	public static boolean getDecisionTypeGoodBad() {
		return (getProperty("DecisionType").equalsIgnoreCase("GOOD-BAD"));
	}

	public static boolean getDecisionTypeMultiField() {
		return (getProperty("DecisionType").equalsIgnoreCase("MULTIFIELD"));
	}

	public static DecisionTextLogger getDecisionTextLogger() {
		return decisionTextLogger;
	}

	public static void setDecisionTextLogger(DecisionTextLogger decisionTextLogger) {
		ContextManager.decisionTextLogger = decisionTextLogger;
	}

	public static boolean logToDecisionTextLogger() {
		return getProperty("DecisionTextLogger").equalsIgnoreCase("ON");
	}

	public static boolean isCalibrationRouteLoggerOn() {
		return getProperty("CalibrationRouteLogger").equalsIgnoreCase("ON");
	}

	public static boolean getWriteRouteContext() {
		return getProperty("writeRouteContext").equalsIgnoreCase("ON");
	}

	/**
	 * Returns the file name for the decision logger
	 * @return
	 */
	public static String getDecisionTextLoggerFile() {
		return getProperty("DecisionTextLoggerFile");
	}

	public static String getSuccessPerRouteLoggerFile() {
		return "log/success-per-route-" + ContextManager.getAngleToDestination() + "-" + ContextManager.getOmitDecisionMatrixMultifields() + "-" + getNumberOfRepetitions() + "-reps.txt";
	}

	/**
	 * accessor for the road context
	 * @return
	 */
	public static Context<Road> getRoadContext() {
		return roadContext;
	}

	//	public static LoadNetwork getLoadNetwork() {
	//		return loadNetwork;
	//	}

	public static Integer getModelRunID() {
		return modelRunID;
	}

	public void setModelRunID(Integer modelRunID) {
		ContextManager.modelRunID = modelRunID;
	}

	public static Context<Decision> getDecisionContext() {
		return decisionContext;
	}

	public static void setDecisionContext(Context<Decision> decisionContext) {
		ContextManager.decisionContext = decisionContext;
	}

	public static Context<Route> getRouteContext() {
		return routeContext;
	}

	public static void setRouteContext(Context<Route> routeContext) {
		ContextManager.routeContext = routeContext;
	}

	//	public void experimentalSink() {
	//		FileDataSink f = new FileDataSink(null, null, null);
	//
	//	}

	public static double getAngleToDestination() {

		if (isInBatchMode()) {
			final Parameters parameters = RunEnvironment.getInstance().getParameters();
			Double ad = (Double) parameters.getValue("angle_to_destination");
			return ad;
		}

		return GlobalVars.SCORING_PARAMS.ANGLE_TO_DESTINATION;

	}

	private static boolean isInBatchMode() {
		return inBatchMode;
	}

	public static String getCalibrationTextLoggerFile() {
		// TODO: add the iteration number
		// return "log/calibrationlog-" + ContextManager.omitDecisionMatrixMultifields() + "-" + getAngleToDestination() +  ".txt";
		//		return "log/calibrationlog-" + ContextManager.getAngleToDestination() + "-" + ContextManager.omitDecisionMatrixMultifields() + ".txt";
		return "log/calibrationlog-" + ContextManager.getAngleToDestination() + "-" + ContextManager.getOmitDecisionMatrixMultifields() + "-" + getNumberOfRepetitions() + "-reps.txt";
	}

	public static String getSuccessloggerFile() {
		return "log/successlog.txt";
	}


	public static String getModelInfoLoggerFile() {
		return "log/modelinfo.txt";
	}


	public static String getKillLoggerFile() {
		return getProperty("KillLoggerFile");
	}

	public static String getSimpleLoadLoggerFile() {
		return getProperty("SimpleLoadLoggerFileName");
	}

	public static String getBasicAgentLoggerFileName() {
		return getProperty("BasicAgentLoggerFileName");
	}

	public static boolean isSimpleLoadLoggerOn() {
		return getProperty("SimpleLoadLogger").equals("ON");
	}

	// methods that get settings for the road load logger

	public static boolean isRoadLoadLoggerOn() {
		return getProperty("RoadLoadLogger").equals("ON");
	}

	public static String getRoadLoadLoggerFolder() {
		return getProperty("RoadLoadLoggerFolder");
	}

	public static Integer writeRoadLoadEveryTick() {
		return new Integer(getProperty("writeRoadLoadEveryTick"));
	}

	public static RoadLoadLogger getRoadLoadLogger() {
		return roadLoadLogger;
	}


	// --------------------------------------------------

	/**
	 * @return the minimum mnumber of edges a result route has to have in order to be taken
	 * into consideration for the calibration run. 
	 */
	public int getMinEdgeNumberOfEdgesForCalibration() {
		return new Integer(getProperty("minEdgeNumberOfEdgesForCalibration"));
	}

	public static boolean isPostgreSQLLoggerOn() {
		return getProperty("PostgreSQLLogger").equals("ON");
	}

	public static boolean isBasicAgentLoggerOn() {
		return getProperty("BasicAgentLogger").equals("ON");
	}

	public static boolean isRoadToCSVDumperOne() {
		return getProperty("RoadToCSVDumperOne").equals("ON");
	}

	public String getNotificationEmailAddress() {
		return getProperty("notificationEmailAddress");
	}

	//	returns whether we run in debug mode
	public static boolean getDEBUG_MODE() {
		String DM = getProperty("DEBUG_MODE");
		if (DM != null) {
			return DM.equals("true");
		} else return false;
	}

	//	returns whether we run in debug mode
	public static String getLOGGING_FOLDER() {
		return  getProperty("LOGGING_FOLDER");
	}

	//	returns whether we run in debug mode
	public static boolean getPOSITION_DEBUG_MODE() {
		String DM = getProperty("POSITION_DEBUG_MODE");
		if (DM != null) {
			return DM.equals("true");
		} else return false;
	}

	//	returns whether we run in debug mode
	public static boolean getCHOICE_DEBUG_MODE() {
		String DM = getProperty("CHOICE_DEBUG_MODE");
		if (DM != null) {
			return DM.equals("true");
		} else return false;
	}

	//	public static KillLogger getKillLogger() {
	//		return killLogger;
	//	}
	//
	//	public static void setKillLogger(KillLogger killLogger) {
	//		ContextManager.killLogger = killLogger;
	//	}

	public static void setAgentContext(Context<IAgent> agentContext) {
		ContextManager.agentContext = agentContext;
	}

	public static void setCalibrationAgentsToModel(int calibrationAgentsToModel) {
		ContextManager.calibrationAgentsToModel = calibrationAgentsToModel;
	}



	public static void incrementNumberOfKills() {
		ContextManager.numberOfKills = ContextManager.numberOfKills + 1;
	}

	public static void resetNumberOfKills() {
		ContextManager.numberOfKills = 0;
	}

	public static CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public static void setCrs(CoordinateReferenceSystem coordinateReferenceSystem) {
		ContextManager.crs = coordinateReferenceSystem;
	}

	public static void resetCrowdingNetwork() {
		ContextManager.crowdingNetwork = new RoadNetwork();

	}

	public static int dumpAtEveryTick() {
		return new Integer(getProperty("dumpAtEveryTick"));
	}

	public static void setAgentGeography(Geography<IAgent> agentGeography2) {
		agentGeography = agentGeography2;

	}

	public static int getDistanceSnap() {
		return new Integer(ContextManager.getProperty(GlobalVars.distanceSnap));
	}

	public static MatchedRouteCache getMatchedRouteCache() {
		return matchedRouteCache;
	}

	public static void setMatchedRouteCache(MatchedRouteCache matchedRouteCache) {
		ContextManager.matchedRouteCache = matchedRouteCache;
	}

	public static MatchedGPSRoute getMatchedGPSRoute() {
		return matchedGPSRoute;
	}

	public static void setMatchedGPSRoute(MatchedGPSRoute matchedGPSRoute) {
		ContextManager.matchedGPSRoute = matchedGPSRoute;
	}

	public static Stack<MatchedGPSRoute> getMatchedGPSRouteStack() {
		return matchedGPSRouteStack;
	}

	public static void setMatchedGPSRouteStack(Stack<MatchedGPSRoute> matchedGPSRouteStack) {
		ContextManager.matchedGPSRouteStack = matchedGPSRouteStack;
	}

	public static Coordinate[] getResultRouteCoordinates() {
		return resultRouteCoordinates;
	}

	public static void setResultRouteCoordinates(
			Coordinate[] resultRouteCoordinates) {
		ContextManager.resultRouteCoordinates = resultRouteCoordinates;
	}

	//	public static Coordinate getStartCoordinate() {
	//		return startCoordinate;
	//	}

	//	public static void setStartCoordinate(Coordinate startCoordinate) {
	//		ContextManager.startCoordinate = startCoordinate;
	//	}

	//	public static Coordinate getEndCoordinate() {
	//		return endCoordinate;
	//	}

	//	public static void setEndCoordinate(Coordinate endCoordinate) {
	//		ContextManager.endCoordinate = endCoordinate;
	//	}

	public static GeometryFactory getGeomFac() {
		return geomFac;
	}

	public void setGeomFac(GeometryFactory geomFac) {
		ContextManager.geomFac = geomFac;
	}

	public static void logToPostgres(IAgent agent) {
		if (pgDEBUG_MODE ) {
			System.out.println(">> logging to postreSQL" + agent.getPosition());
		} else {
			if (ContextManager.isPostgreSQLLoggerOn()) {
				ContextManager.getPostgresLogger().log(ContextManager.getCurrentTick(), agent);
			}
		}
	}

	public static ArrayList<IAgent> getDeadAgents() {
		return deadAgents;
	}

	public static void setDeadAgents(ArrayList<IAgent> deadAgents) {
		ContextManager.deadAgents = deadAgents;
	}

	public static Stack<CalibrationLet> getCalibrationLets() {
		return calibrationLets;
	}

	public void setCalibrationLets(Stack<CalibrationLet> calibrationLets) {
		ContextManager.calibrationLets = calibrationLets;
	}

	/**
	 * @return the setting numberOfRepetitions from the config file.
	 * The number of repetition the model does in calibration mode.
	 */
	public static int getNumberOfRepetitions() {
		String p = getProperty("numberOfRepetitions");
		return new Integer(p);

	}

	public static long getUniqueModelID() {
		return ContextManager.uniqueModelID;
	}

	public void setUniqueModelID(long l) {
		ContextManager.uniqueModelID = l;
	}

	public static long getStartTime() {
		return startTime;
	}

	public static void setStartTime(long startTime) {
		ContextManager.startTime = startTime;
	}


}
