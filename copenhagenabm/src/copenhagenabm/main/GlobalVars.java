/*
© Copyright 2013 Bernhard Snizek
© Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
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

package copenhagenabm.main;


import com.vividsolutions.jts.geom.Geometry;


/**
 * 
 * @author Bernhard Snizek
 * ©Copyright 2012, 2013 Bernhard Snizek, besn@life.ku.dk, +45 23 71 00 46
 *
 */
public abstract class GlobalVars {

	public static final class SCORING_PARAMS {

		public static final double RIGHT = -1.32893d;
		public static final double LEFT = -1.47166d;
		public static final double CTRACKLANE = 0.6402038d;
		public static final double CSTI = -0.536507d;
		public static final double CFSTI = -0.5226254d;
		public static final double GROENPCT = 0.595598d;
		public static final double E_TVEJ = -0.397053d;
		public static final double E_LVEJ = -0.3707007d;
		public static final double E_AND = -0.7314498d;
		public static final double E_HOJ = -1.2553576d;
		public static final double E_BUT = -1.021367d;

		public static final double ANGLE_TO_DESTINATION = 20.0d;
		public static final double AVOID_U_TURN = 0.0d;
		public static final boolean EXCEPT_U_TURN = true;
		
		public static final boolean AVOID_ALREADY_VISITED = true;
		
		public static final int CROWDING = 0;
		public static final double GOODBAD = 0.0d;

	}


	/* These are meta entries of the model */
	public static final String StartTime = "StartTime";
	public static final String EndTime = "EndTime";


	/* These are strings that match entries in the copenhagenabm.properties file.*/
	public static final String GISDataDirectory = "GISDataDirectory";
	public static final String BuildingShapefile = "BuildingShapefile";
	public static final String ZoneShapefile = "ZoneShapefile";
	public static final String MatrixFile = "MatrixFile";
	public static final String RoadShapefile = "RoadShapefile";
	public static final String BuildingsRoadsCoordsCache = "BuildingsRoadsCoordsCache";
	public static final String BuildingsRoadsCache = "BuildingsRoadsCache";

	// the identified for the result road shapefile in the properties file
	public static final String ResultRoutesShapefile = "ResultRoadShapeFile"; 

	public static final String EntryPointNumberOfPersonsField = "EntryPointNumberOfPersonsField";

	// the length of the tick
	public static final String StepLength = "StepLength";

	public static final String AgentSpeed = "AgentSpeed";

//	public static final String GroupingFactor = "GroupingFactor"; 

	public static final String inCalibrationMode = "inCalibrationMode";
	public static final String numberOfRepetitions = "numberOfRepetitions";		// the number of agents to be sent into the model when running in explicative mode


	public static final String distanceSnap = "distanceSnap";

	public static final String dumpCrowdingNetwork = "dumpCrowdingNetwork"; 

	// the good and bad rasters
	public static final String GoodExperienceRasterfile = "GoodExperienceRasterfile";
	public static final String BadExperienceRasterfile = "BadExperienceRasterfile";

	public static final String AgentHistoryDirectory = "AgentHistoryDirectory";

	public static final String KMZExaggerationFactor = "KMZExaggerationFactor";

	public static final String pathSizeSetFile = "pathSizeSetFile";



	public static final class GEOGRAPHY_PARAMS {

		public static final double TEMPRORAY_STEP_LENGTH = 4; // step length in meters

		/**
		 * Different search distances used in functions that need to find objects that are
		 * close to them. A bigger buffer means that more objects will be analysed (less
		 * efficient) but if the buffer is too small then no objects might be found. 
		 * The units represent a lat/long distance so I'm not entirely sure what they are,
		 * but the <code>Route.distanceToMeters()</code> method can be used to roughly 
		 * convert between these units and meters.
		 * @see Geometry
		 * @see Route
		 */
		public enum BUFFER_DISTANCE {
			/** The smallest distance, rarely used. Approximately 0.001m*/
			SMALL(0.00000001, "0.001"),
			/** Most commonly used distance, OK for looking for nearby houses or roads.
			 * Approximatey 110m */
			MEDIUM(0.001,"110"),
			/** Largest buffer, approximately 550m. I use this when doing things that
			 * don't need to be done often, like populating caches.*/
			LARGE(0.005,"550");
			/**
			 * @param dist The distance to be passed to the search function (in lat/long?)
			 * @param distInMeters An approximate equivalent distance in meters.
			 */
			BUFFER_DISTANCE(double dist, String distInMeters) {
				this.dist = dist;
				this.distInMeters = distInMeters;
			}
			public double dist;
			public String distInMeters;
		}

		// public static final double TRAVEL_PER_TURN = 1; // TODO Make a proper value for this

		public static final double AGENT_SPEED = 0.0005;  // agent speed in m/s
		public static final double TICK_LENGTH = 1; // tick length in s
		public static final boolean AVOID_INCOMING_EDGE = true;  // avoid entering the 
		public static final double SNAP_DISTANCE = 0.00000001;

	}

	/** Names of contexts and projections. These names must match those in the
	 * parameters.xml file so that they can be displayed properly in the GUI. */
	public static final class CONTEXT_NAMES {

		public static final String MAIN_CONTEXT = "maincontext";
		public static final String MAIN_GEOGRAPHY = "MainGeography";

		public static final String BUILDING_CONTEXT = "BuildingContext";
		public static final String BUILDING_GEOGRAPHY = "BuildingGeography";

		public static final String ROAD_CONTEXT = "RoadContext";
		public static final String ROAD_GEOGRAPHY = "RoadGeography";

		public static final String JUNCTION_CONTEXT = "JunctionContext";
		public static final String JUNCTION_GEOGRAPHY = "JunctionGeography";

		public static final String ROAD_NETWORK = "RoadNetwork";

		public static final String AGENT_CONTEXT = "AgentContext";
		public static final String AGENT_GEOGRAPHY = "AgentGeography";

		public static final String ZONE_CONTEXT = "ZoneContext";
		public static final String ZONE_GEOGRAPHY = "ZoneGeography";

		public static final String RESULT_ROUTE_CONTEXT = "ResultRouteContext";
		public static final String RESULT_ROUTE_GEOGRAPHY = "ResultRouteGeography";

		public static final String ROUTE_CONTEXT = "RouteContext";
		public static final String ROUTE_GEOGRAPHY = "RouteGeography";

		public static final String MATCHED_GPS_ROUTE_CONTEXT = "MatchedGPSRouteContext";
		public static final String MATCHED_GPS_ROUTE_GEOGRAPHY = "MatchedGPSRouteGeography";
		
		public static final String DOT_CONTEXT = "DotContext";
		public static final String DOT_GEOGRAPHY = "DotGeography";
		
		public static final Object DECISION_CONTEXT = "DecisionContext";
		public static final String DECISION_GEOGRAPHY = "DecisionGeography";
		

	}

	// Parameters used by transport networks
	//	public static final class TRANSPORT_PARAMS {
	//
	//		// This variable is used by NetworkEdge.getWeight() function so that it knows what travel options
	//		// are available to the agent (e.g. has a car). Can't be passed as a parameter because NetworkEdge.getWeight()
	//		// must override function in RepastEdge because this is the one called by ShortestPath.
	//		public static IAgent currentAgent = null;
	//		public static Object currentBurglarLock = new Object();
	//
	//		public static final String WALK = "walk";
	//		public static final String BUS = "bus";
	//		public static final String TRAIN = "train";
	//		public static final String CAR = "car";
	//		// List of all transport methods in order of quickest first
	//		public static final List<String> ALL_PARAMS = Arrays.asList(new String[]{TRAIN, CAR, BUS, WALK});
	//
	//		// Used in 'access' field by Roads to indicate that they are a 'majorRoad' (i.e. motorway or a-road).
	//		public static final String MAJOR_ROAD = "majorRoad";		
	//		// Speed advantage for car drivers if the road is a major road'
	//		public static final double MAJOR_ROAD_ADVANTAGE = 3;
	//
	//		// The speed associated with different types of road (a multiplier, i.e. x times faster than walking)
	//		public static double getSpeed(String type) {
	//			if (type.equals(WALK))
	//				return 1;
	//			else if (type.equals(BUS))
	//				return 2;
	//			else if (type.equals(TRAIN))
	//				return 10;
	//			else if (type.equals(CAR))
	//				return 5;
	//			else {
	//				LOGGER.log(Level.SEVERE, "Error getting speed: unrecognised type: "+type);
	//				return 1;
	//			}
	//		}


}
