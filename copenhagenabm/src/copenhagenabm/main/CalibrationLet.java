package copenhagenabm.main;

import copenhagenabm.routes.MatchedGPSRoute;

/**
 * @author besn
 * An object that is instantiated in the build phase of the ABMS.
 *
 */
public class CalibrationLet {

	private MatchedGPSRoute gpsRoute;
	private int iteration;

	public CalibrationLet(MatchedGPSRoute gpsRoute, int i) {
		this.setGpsRoute(gpsRoute);
		this.setIteration(i);
	}

	public MatchedGPSRoute getGpsRoute() {
		return gpsRoute;
	}

	public void setGpsRoute(MatchedGPSRoute gpsRoute) {
		this.gpsRoute = gpsRoute;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	

}
