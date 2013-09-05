package copenhagenabm.environment.contexts;

import copenhagenabm.agent.Dot;
import copenhagenabm.main.GlobalVars;
import repast.simphony.context.DefaultContext;

public class DotContext extends DefaultContext<Dot>{ 
	
	public DotContext() {
		super(GlobalVars.CONTEXT_NAMES.DOT_CONTEXT);
	}

}
