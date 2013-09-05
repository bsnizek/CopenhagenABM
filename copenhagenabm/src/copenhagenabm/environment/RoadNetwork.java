package copenhagenabm.environment;

import java.util.ArrayList;
import java.util.HashMap;

import repastcity3.exceptions.NoIdentifierException;

import copenhagenabm.agent.CPHAgent;


/**
 * 
 * @author Bernhard Snizek
 *
 */
public class RoadNetwork {

	HashMap<String, ArrayList<CPHAgent>> roads = new HashMap<String, ArrayList<CPHAgent>>();

	public void addAgentToRoad(CPHAgent agent, Road road) {

		try {
			if (roads.containsKey(road.getIdentifier())) {
				ArrayList<CPHAgent> r = roads.get(road.getIdentifier());
				if (!r.contains(agent)) {
					r.add(agent);
				}
			} else {
				ArrayList<CPHAgent> r = new ArrayList<CPHAgent>();
				r.add(agent);
				try {
					roads.put(road.getIdentifier(), r);
				} catch (NoIdentifierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		} catch (NoIdentifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	public void removeAgentFromRoad(CPHAgent agent, Road road) {
		try {

			if (roads.containsKey(road.getIdentifier())) {

				ArrayList<CPHAgent> r = roads.get(road.getIdentifier());

				if (r != null) {
					r.remove(agent);
				}
			}

		} catch (NoIdentifierException e) {
			e.printStackTrace();
		}
	}

	public int getRoadLoad(Road road) {
		try {
			ArrayList<CPHAgent> r = roads.get(road.getIdentifier());
			if (r != null) {
				return r.size();
			} else {
				return 0;
			}
		} catch (NoIdentifierException e) {
			e.printStackTrace();
		}


		return 0;

	}

	public boolean hasRoad(Road currentRoad) {

		if (currentRoad != null) {
			try {
				return this.roads.containsKey(currentRoad.getIdentifier());
			} catch (NoIdentifierException e) {
				e.printStackTrace();
			}
			return false;
		}
		return false;
	}
}