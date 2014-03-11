package copenhagenabm.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import copenhagenabm.agent.decisionmatrix.DecisionMatrix;
import copenhagenabm.environment.Road;
import copenhagenabm.environment.RoadNetwork;
//import copenhagenabm.loggers.SimpleLoadLogger;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;
import copenhagenabm.routes.Route;

import repastcity3.exceptions.NoIdentifierException;

public class EdgeSelector {

	DecisionMatrix decisionMatrix = null;

	ScoreFunctions sF = new ScoreFunctions();	

	RoadNetwork rn = ContextManager.getCrowdingNetwork();

	private CPHAgent agent;

	public CPHAgent getAgent() {
		return agent;
	}


	public void setAgent(CPHAgent agent) {
		this.agent = agent;
	}

	private String originalRoadIDS = "Original Road IDs ";

	//	private SimpleLoadLogger simpleLoadLogger;

	public DecisionMatrix getDecisionMatrix() {
		return decisionMatrix;
	}

	private String originRoadID;

	private boolean abort = false;


	//	private class RoadIdentifierTuple {
	//		private Road road;
	//		private String identifier;
	//
	//		public RoadIdentifierTuple(Road r, String id) {
	//			this.road = r;
	//			this.identifier = id;
	//			
	//		}
	//	}

	public EdgeSelector(List<Road> roads, Road currentRoad, CPHAgent agent) {

		// we build two hashes. getIdentifier() takes time so we speed it up a lot.

		HashMap<String, Road> roadIdentifierHash= new HashMap<String, Road>();
		HashMap<Road, String> identifierRoadHash= new HashMap<Road, String>();

		for (Road r : roads) {
			String rID = null;
			try {
				rID = r.getIdentifier();
			} catch (NoIdentifierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			roadIdentifierHash.put(rID, r);
			identifierRoadHash.put(r, rID);

		}

		if (currentRoad==null) {
			originRoadID = "<entry>";
		} else {

			try {
				originRoadID = currentRoad.getIdentifier();
			} catch (NoIdentifierException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		this.agent = agent;

		decisionMatrix = new DecisionMatrix(agent.getID());

		ArrayList<Road> newRoads = new ArrayList<Road>();

		if (roads.size() == 0) {
			if (ContextManager.getCHOICE_DEBUG_MODE()) {
				System.out.println("roads=null");
			}
		}

		if (ContextManager.getCHOICE_DEBUG_MODE()) {
			String dMRoadIDS = "(" + ContextManager.getCurrentTick() + ") A(" + this.getAgent().getID() + ") = ";

			for (Road r : roads) {
				try {
					dMRoadIDS = dMRoadIDS + r.getIdentifier() + " ";
				} catch (NoIdentifierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println(dMRoadIDS);
		}

		for (Road r : roads) {

			if (roads.size() > 1) {

				String rID = identifierRoadHash.get(r);

				int occurrences = Collections.frequency(this.agent.getRoadHistory(), rID);

				if (occurrences>3) {
					// do not add the road, we have already been there 3 times
					if (ContextManager.getCHOICE_DEBUG_MODE()) {
						System.out.println("Road with ID=" + rID +" has already " + occurrences + " - skipping;");
					}
				} else {

					if (currentRoad == null) {
						newRoads.add(r);
					}
					else {

						// crID current road id
						String crID = identifierRoadHash.get(currentRoad);

						if (GlobalVars.SCORING_PARAMS.EXCEPT_U_TURN && (rID == crID)) {
							if (ContextManager.getCHOICE_DEBUG_MODE()) {
								System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + this.getAgent().getID() + ") Road ID=" + rID + " not added (U-turn).");
							}

						} else {
							newRoads.add(r);
						}
					}
				}

			} else {newRoads.add(r);}
		} 

		if (newRoads.size()==0) {
			if (ContextManager.getCHOICE_DEBUG_MODE()) {
				System.out.println("No edges left, aborting...");
			}

			this.abort  = true;

		}

		if (abort) {

		} else {

			for (Road r : newRoads) {
				decisionMatrix.addOption(r);
			}
			
			decisionMatrix.addCharacteristic("angle_to_destination");	
			if (GlobalVars.SCORING_PARAMS.AVOID_ALREADY_VISITED) {
				decisionMatrix.addCharacteristic("avoid_already_visited");
			}

			if (ContextManager.getDecisionTypeGoodBad()) {
				decisionMatrix.addCharacteristic("goodbad");
			}

			if (ContextManager.getDecisionTypeMultiField()) {

				if (!ContextManager.getOmitDecisionMatrixMultifields()) {

					decisionMatrix.addCharacteristic("left");
					decisionMatrix.addCharacteristic("right");
					
					decisionMatrix.addCharacteristic("ctracklane");
					decisionMatrix.addCharacteristic("csti");
					decisionMatrix.addCharacteristic("cfsti");
					
					decisionMatrix.addCharacteristic("groenpct");
					
					decisionMatrix.addCharacteristic("e_tvej");
					decisionMatrix.addCharacteristic("e_lvej");
					decisionMatrix.addCharacteristic("e_and");
					decisionMatrix.addCharacteristic("e_hoj");
					decisionMatrix.addCharacteristic("e_but");
				}

			}

			decisionMatrix.addCharacteristic("avoid_u_turn");

			// no crowding in the explicative model
			/*
		if (!ContextManager.inCalibrationMode() && ContextManager.isCrowdingLoggerOn()) {
			decisionMatrix.addCharacteristic("avoid_crowding");
		}
			 */
			for (Road rr : newRoads) {

				double btd = ScoreFunctions.scoreAngleToDestination(rr, agent.getPosition(), 
						agent.getDestinationCoordinate(), ContextManager.getAngleToDestination());

				decisionMatrix.addCell("angle_to_destination", rr, btd);

				if (ContextManager.getDecisionTypeGoodBad()) {
					decisionMatrix.addCell("goodbad", rr, rr.getGoodbad() * GlobalVars.SCORING_PARAMS.GOODBAD);
				}

				if (ContextManager.getDecisionTypeMultiField()) {

					if (currentRoad != null)  {

						double right = ScoreFunctions.scoreTurnRight(
								rr, 
								agent.getPosition(), 
								currentRoad, 
								GlobalVars.SCORING_PARAMS.RIGHT);

						decisionMatrix.addCell("right", rr, right);
					} else {
						decisionMatrix.addCell("right", rr, 0.0d);
					}

					if (GlobalVars.SCORING_PARAMS.AVOID_ALREADY_VISITED) {
						double aav = 10.0;
						Route route = agent.getRoute();
						if (route != null) {
							for (Road r : route.getRouteAdsRoadList()) {
								try {
									if (r.getIdentifier() == rr.getIdentifier()) {
										aav = 0.0d;
									}
								} catch (NoIdentifierException e) {
									e.printStackTrace();
								}
								decisionMatrix.addCell("avoid_already_visited", rr, aav);
							}
						}
					}


					if (currentRoad != null)  {

						double left = ScoreFunctions.scoreTurnLeft(
								rr, 
								agent.getPosition(), 
								currentRoad, 
								GlobalVars.SCORING_PARAMS.LEFT);

						decisionMatrix.addCell("left", rr, left);
					} else {
						decisionMatrix.addCell("left", rr, 0.0d);
					}

					decisionMatrix.addCell("ctracklane", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.CTRACKLANE);
					decisionMatrix.addCell("csti", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.CSTI);
					decisionMatrix.addCell("cfsti", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.CFSTI);
					decisionMatrix.addCell("groenpct", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.GROENPCT);
					decisionMatrix.addCell("e_tvej", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.E_TVEJ);
					decisionMatrix.addCell("e_lvej", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.E_LVEJ);
					decisionMatrix.addCell("e_and", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.E_AND);
					decisionMatrix.addCell("e_hoj", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.E_HOJ);
					decisionMatrix.addCell("e_but", rr, rr.getCtracklane() * GlobalVars.SCORING_PARAMS.E_BUT);

				}

				double aUt = ScoreFunctions.avoidUturn(rr, agent.getCurrentRoad(), GlobalVars.SCORING_PARAMS.AVOID_U_TURN);
				decisionMatrix.addCell("avoid_u_turn", rr, aUt);


				/**
				 * Omit crowding if we have an calibration model
				 */
				/*
			if (!ContextManager.inCalibrationMode() && ContextManager.isCrowdingLoggerOn()) {
				int numberOfAgentsOnSegment = rn.getRoadLoad(rr);

				if (numberOfAgentsOnSegment == 0) {
					decisionMatrix.addCell("avoid_crowding", rr, 1.0 * GlobalVars.SCORING_PARAMS.CROWDING);
				} else {
					decisionMatrix.addCell("avoid_crowding", rr, 1 / numberOfAgentsOnSegment * GlobalVars.SCORING_PARAMS.CROWDING);
				}
			}
				 */
			}
		}
	}


	public Road getRoad() {

		if (abort) {
			return null;
		} else {

			this.agent.addRoadToRoadHistory();

			Road r =  (Road) decisionMatrix.rollDice();

			if (ContextManager.isDecisionLoggerOn()) {

				try {
					Decision d = new Decision(this.agent.getID(), this.agent.getPosition(), r.getIdentifier());
					ContextManager.getDecisionContext().add(d);
				} catch (NoIdentifierException e) {

					e.printStackTrace();
				}
			}


			if (r == null) {
				if (ContextManager.getCHOICE_DEBUG_MODE()) {
					System.out.println("EdgeSelector.getRoad() : rollDice() returned null");
				}
				r =  (Road) decisionMatrix.rollDice();
				if (ContextManager.getCHOICE_DEBUG_MODE()) {
					System.out.println("rollDice() second attempt returned "+ r);
				}
			}

			if (r==null) {
				if (ContextManager.getCHOICE_DEBUG_MODE()) {
					System.out.println("EdgeSelector returns null. " + this.originalRoadIDS);
				}
				r =  (Road) decisionMatrix.rollDice();
			}

			this.agent.setCurrentRoad(r);

			String newRoadID=null;

			try {
				newRoadID = r.getIdentifier();
			} catch (NoIdentifierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (ContextManager.getCHOICE_DEBUG_MODE()) {
				System.out.println("(" + ContextManager.getCurrentTick() + ") A(" + this.getAgent().getID() + ") EdgeSelector: " + this.originRoadID + " -> " +  newRoadID);
			}
			return r;

		}

	}
}
