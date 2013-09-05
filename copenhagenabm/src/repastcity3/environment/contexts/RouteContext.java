package repastcity3.environment.contexts;

import copenhagenabm.main.GlobalVars;
import copenhagenabm.routes.Route;
import repast.simphony.context.DefaultContext;

public class RouteContext  extends DefaultContext<Route> {
	
	public RouteContext() {
		super(GlobalVars.CONTEXT_NAMES.ROUTE_CONTEXT);
	}

}
