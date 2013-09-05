package copenhagenabm.tests;

import copenhagenabm.main.GlobalVars;
import repast.simphony.context.DefaultContext;

public class PointContext extends DefaultContext<PPoint>{ 
	
	public PointContext() {
		super(GlobalVars.CONTEXT_NAMES.DOT_CONTEXT);
	}

}
