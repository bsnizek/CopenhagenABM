package copenhagenabm.main;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import copenhagenabm.agent.IAgent;
import copenhagenabm.loggers.CalibrationRouteLogger;
import copenhagenabm.loggers.SuccessLogger;

/**
 * 
 * CalibrationModeData
 * 
 * Stores data during the calibration process.
 * 
 * @author Bernhard Snizek
 * b@snizek.com / +45 23 71 00 46 
 * 
 */
public class CalibrationModeData {
	
	/**
	 * 
	 * CalibrationRoute
	 * 
	 * Holds the data of a calibration route.
	 * 
	 * 
	 * @author besn
	 *
	 */

	public class CalibrationRoute {
		
		private LineString route;
		private Coordinate origin;
		private Coordinate destination;
		private Coordinate death = new Coordinate(0,0);
		
		private int GPSRouteID;
		private double route_gps_lngth;
		private boolean successful;
		private int nIter;
		private double overlap;
		private IAgent agent;
		private double edge_lngth_avg;
		private double calctime;
		
		public CalibrationRoute(IAgent agent, Coordinate origin, Coordinate destination, int GPSRouteID, double route_gps_lngth, int nIter) {
			
			this.origin = origin;
			this.destination = destination;
			this.GPSRouteID = GPSRouteID;
			this.route_gps_lngth = route_gps_lngth;
			this.nIter = nIter;
			this.agent = agent;
			
			System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + agent.getID() + ") GPS(" + GPSRouteID +") CalibrationRoute() nIter=" + nIter + ".");
			
		}

		public Coordinate getDeath() {
			return death;
		}

		public void setDeath(Coordinate death) {
			this.death = death;
		}

		public void setSuccessful(boolean b) {
			this.successful = b;
			
		}

		public Coordinate getOrigin() {
			return origin;
		}

		public void setOrigin(Coordinate origin) {
			this.origin = origin;
		}

		public Coordinate getDestination() {
			return destination;
		}

		public void setDestination(Coordinate destination) {
			this.destination = destination;
		}

		public int getGPSRouteID() {
			return GPSRouteID;
		}

		public void setGPSRouteID(int gPSRouteID) {
			GPSRouteID = gPSRouteID;
		}

		public double getRoute_gps_lngth() {
			return route_gps_lngth;
		}

		public void setRoute_gps_lngth(double route_gps_lngth) {
			this.route_gps_lngth = route_gps_lngth;
		}

		public boolean isSuccessful() {
			return successful;
		}

		public int getnIter() {
			return nIter;
		}

		public void setnIter(int nIter) {
			this.nIter = nIter;
		}

		public double getOverlap() {
			return overlap;
		}

		public void setOverlap(double overlap) {
			this.overlap = overlap;
		}

		public LineString getRoute() {
			return route;
		}

		public void setRoute(LineString route) {
			this.route = route;
		}

		public void setAgent(IAgent cphAgent) {
			this.agent = cphAgent;
			
		}

		public IAgent getAgent() {
			return agent;
		}

		public double getEdge_lngth_avg() {
			return edge_lngth_avg;
		}

		public void setEdge_lngth_avg(double edge_lngth_avg) {
			this.edge_lngth_avg = edge_lngth_avg;
		}

		public double getCalctime() {
			return calctime;
		}

		public void setCalctime(double calctime) {
			this.calctime = calctime;
		}
		
	}
	
	private long startTime;
	private int totalMatchedGPSRoutes = 0;
	private int omittedMatchedGPSRoutes = 0;
	private int totalNumberOfIterations = 0;
	private int successfullyModeledRoutes = 0;
//	private double angleToDestWeight = 0.0;
	private boolean omitDecisionMatrixMultifields = false;
	private long runTime;
	private ArrayList<CalibrationRoute> calibrationRoutes = new ArrayList<CalibrationRoute>();
	private int currentNIter;
	

	
	/**
	 * The ID of the current iteration : 0 .. n-1
	 */
//	private int currentNIter;
//	private int GPSId;
	

	public boolean isOmitDecisionMatrixMultifields() {
		return omitDecisionMatrixMultifields;
	}

	public void setOmitDecisionMatrixMultifields(
			boolean omitDecisionMatrixMultifields) {
		this.omitDecisionMatrixMultifields = omitDecisionMatrixMultifields;
	}

	// canceledAgents : agents that have been deleted as they were > 50 % off the length of the route
	public static int canceledAgents = 0;
	
	private static SuccessLogger successLogger = new SuccessLogger();
	private static CalibrationRouteLogger calibrationRouteLogger = new CalibrationRouteLogger();
	
	/**
	 * CalibrationModeData
	 */
	
	public CalibrationModeData(boolean omitDecisionMatrixMultifields) {
		this.omitDecisionMatrixMultifields = omitDecisionMatrixMultifields;
	}

	public CalibrationModeData() {
	}

	/**
	 * log()
	 * 
	 * dumps the data into the success log
	 * 
	 */
	public void log_success() {
		CalibrationModeData.getSuccessLogger().log(this);
		System.out.println("Calibration Mode Data : Successlog logged");
	}
	
	public void logCalibrationRoutes() {
		CalibrationModeData.getCalibrationRouteLogger().log(this.getCalibrationRoutes());
		System.out.println("Calibration Mode Data : Calibration Routes logged");
	}
	
	
	private static CalibrationRouteLogger getCalibrationRouteLogger() {
		return calibrationRouteLogger;
	}

//	public String toString() {
//		return "total routes=" + totalMatchedGPSRoutes + "; omitted routes=" + omittedMatchedGPSRoutes + 
//				"; total number of iterations=" + totalNumberOfIterations + 
//				"; successfully modeled routes=" + successfullyModeledRoutes + 
//				"; angleToDestWeight=" + angleToDestWeight;
//	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public int getTotalMatchedGPSRoutes() {
		return totalMatchedGPSRoutes;
	}

	public void setTotalMatchedGPSRoutes(int totalMatchedGPSRoutes) {
		this.totalMatchedGPSRoutes = totalMatchedGPSRoutes;
	}

	public int getOmittedMatchedGPSRoutes() {
		return omittedMatchedGPSRoutes;
	}

	public void setOmittedMatchedGPSRoutes(int omittedMatchedGPSRoutes) {
		this.omittedMatchedGPSRoutes = omittedMatchedGPSRoutes;
	}

	public void incrementOmittedMatchedRoute() {
		omittedMatchedGPSRoutes = omittedMatchedGPSRoutes + 1;
		
	}

	public void setTotalNumberOfIterations(int totalNumberOfIterations) {
		this.totalNumberOfIterations = totalNumberOfIterations;
	}
	
	public void incrementSuccessfullyModeledRoutes() {
		successfullyModeledRoutes = successfullyModeledRoutes + 1;
	}

	public int getSuccessfullyModeledRoutes() {
		return successfullyModeledRoutes;
	}

	public void setSuccessfullyModeledRoutes(int successfullyModeledRoutes) {
		this.successfullyModeledRoutes = successfullyModeledRoutes;
	}

	public static SuccessLogger getSuccessLogger() {
		return successLogger;
	}

	public void setSuccessLogger(SuccessLogger successLogger) {
		CalibrationModeData.successLogger = successLogger;
	}

	public static int getCanceledAgents() {
		return canceledAgents;
	}

	public static void setCanceledAgents(int canceledAgents) {
		CalibrationModeData.canceledAgents = canceledAgents;
	}

	public void incrementNumberOfCanceledAgents() {
		canceledAgents = canceledAgents + 1;
		
	}

	public double getRunningTime() {
		return 0;
	}

	public void setRunTime(long modelRunSeconds) {
		this.runTime = modelRunSeconds;
		
	}

	public long getRunTime() {
		return runTime;
	}

	public void addCalibrationRoute(CalibrationRoute calibrationRoute) {
		this.calibrationRoutes.add(calibrationRoute);
		System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + calibrationRoute.getAgent().getID() + ") IT(" + calibrationRoute.getnIter() + ") CalibrationRoute added.");
		
	}

	public ArrayList<CalibrationRoute> getCalibrationRoutes() {
		return calibrationRoutes;
	}

	public void setCalibrationRoutes(ArrayList<CalibrationRoute> calibrationRoutes) {
		this.calibrationRoutes = calibrationRoutes;
	}

	public void closeCalibrationRouteLogger() {
		CalibrationModeData.calibrationRouteLogger.close();
		
	}

	public void incrementCurrentNIter() {
		currentNIter = currentNIter + 1;
	}
	
	public void zeroCurrentNIter() {
		currentNIter = 0;
	}

	public int getNumberOfRepetitions() {
		return ContextManager.getNumberOfRepetitions();
	}

	public int getTotalNumberOfIterations() {
		return totalNumberOfIterations;
	}

	public long getUniqueModelID() {
		
		return ContextManager.getUniqueModelID();
	}
	
	
}
