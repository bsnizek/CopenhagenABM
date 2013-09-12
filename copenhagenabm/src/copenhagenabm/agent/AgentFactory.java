package copenhagenabm.agent;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.environment.Person;
import copenhagenabm.environment.Zone;
import copenhagenabm.main.ContextManager;
import copenhagenabm.routes.MatchedGPSRoute;

public class AgentFactory {
	
	
	/*
	 * createAgent gives birth to an agent
	 * 
	 */
	public CPHAgent createAgent(String zoneFromID, String zoneToID) {

		// 0. Increment the agent ID
		ContextManager.incrementAgentCounter();
//		this.agentID = ContextManager.getAgentCounter();

		// 1. get the zone of origin
		Zone zoneFrom = ContextManager.getZoneByID(zoneFromID);

		// 2. get a random Person 
		Person person=null;
		
		if (zoneFrom == null) {
			System.out.println("zoneFrom = null");
		}

		try {
			person = zoneFrom.getRandomPerson();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 3. build the agent
		CPHAgent a = new CPHAgent(person.getBuilding(), ContextManager.getZoneByID(zoneToID)); // Create a new agent
		
		// System.out.print("+ " + a);
		
		if (a==null) {
			System.out.println("null");
		}
		
		ContextManager.agentsToBeSpawned.add(a);
		
		//ContextManager.addAgentToContext(a); // Add the agent to the context

		// 4. get the projection of the current coordinate onto 
		// the closest road segment and put the agent in that position
		//a.snapAgentToRoad();

		return a;
	}

	/*
	 * createAgent() gives birth to an agent given two coordinates
	 * used for explicative agents that are born in a coodinate and die in one
	 * 
	 */
	public CPHAgent createAgent(Coordinate from, Coordinate to, int sourceRouteID, MatchedGPSRoute matchedGPSRoute) {
		ContextManager.incrementAgentCounter();
		
		CPHAgent a = new CPHAgent(from, to, sourceRouteID, matchedGPSRoute); // Create a new agent
		ContextManager.addAgentToContext(a);  // add it to the context
		a.snapAgentToRoad();
		return a;
	}


}
