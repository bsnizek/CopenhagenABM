package copenhagenabm.environment.contexts;

import repast.simphony.context.DefaultContext;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.routes.MatchedGPSRoute;

public class ResultRouteContext extends DefaultContext<MatchedGPSRoute>{
	
	public ResultRouteContext() {
		super(GlobalVars.CONTEXT_NAMES.RESULT_ROUTE_CONTEXT);
	}
	
}