package copenhagenabm.environment.contexts;

import copenhagenabm.agent.Decision;
import copenhagenabm.main.GlobalVars;
import repast.simphony.context.DefaultContext;

public class DecisionContext extends DefaultContext<Decision>{ 
	
	public DecisionContext() {
		super(GlobalVars.CONTEXT_NAMES.DECISION_CONTEXT);
	}

}
