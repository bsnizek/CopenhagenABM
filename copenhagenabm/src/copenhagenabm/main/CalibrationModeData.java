package copenhagenabm.main;

import copenhagenabm.loggers.SuccessLogger;

public class CalibrationModeData {
	
	private long startTime;
	private int totalMatchedGPSRoutes = 0;
	private int omittedMatchedGPSRoutes = 0;
	private int totalNumberOfIterations = 0;
	private int successfullyModeledRoutes = 0;
	
	// canceledAgents : agents that have been deleted as they were > 50 % off the length of the route
	public static int canceledAgents = 0;
	
	private static SuccessLogger successLogger = new SuccessLogger();
	
	public CalibrationModeData() {
		
	}
	
	public String toString() {
		return "total routes=" + totalMatchedGPSRoutes + "; omitted routes=" + omittedMatchedGPSRoutes + 
				"; total number of iterations=" + totalNumberOfIterations + 
				"; successfully modeled routes" + successfullyModeledRoutes;
	}

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

	// returns the total number of agents we send off for a calibration run. 
	public void calculateNumberOfIteration() {
		this.totalNumberOfIterations = (totalMatchedGPSRoutes-omittedMatchedGPSRoutes) * ContextManager.getNumberOfRepetitions();
		
	}

	public int getTotalNumberOfIterations() {
		return totalNumberOfIterations;
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

}
