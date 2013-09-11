package copenhagenabm.agent;

import java.util.ArrayList;
import java.util.List;

import copenhagenabm.environment.Road;
import copenhagenabm.environment.RoadNetwork;
//import copenhagenabm.loggers.SimpleLoadLogger;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.GlobalVars;

import repastcity3.exceptions.NoIdentifierException;

public class EdgeSelector {

	DecisionMatrix decisionMatrix = null;

	ScoreFunctions sF = new ScoreFunctions();	

	RoadNetwork rn = ContextManager.getCrowdingNetwork();

	private CPHAgent agent;

//	private SimpleLoadLogger simpleLoadLogger;

	public DecisionMatrix getDecisionMatrix() {
		return decisionMatrix;
	}
	

	public EdgeSelector(List<Road> roads, Road currentRoad, CPHAgent agent) {

		this.agent = agent;

		decisionMatrix = new DecisionMatrix(agent.getID());

		ArrayList<Road> newRoads = new ArrayList<Road>();

//		this.simpleLoadLogger = ContextManager.getSimpleLoadLogger();

		for (Road r : roads) {
			if (roads.size()>1) {
				try {

					if (currentRoad==null) {
						newRoads.add(r);
					}
					else {
						if (GlobalVars.SCORING_PARAMS.EXCEPT_U_TURN && (r.getIdentifier()==currentRoad.getIdentifier())) {

						} else {
							newRoads.add(r);
						}
					}
				} catch (NoIdentifierException e) {
					e.printStackTrace();
				}
			} else {newRoads.add(r);}
		} 

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

			if (!ContextManager.omitDecisionMatrixMultifields()) {

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
					for (Road r : agent.getRoute().getRouteAdsRoadList()) {
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


	public Road getRoad() {

		Road r =  (Road) decisionMatrix.rollDice();

		if (ContextManager.isDecisionLoggerOn()) {

			try {
				Decision d = new Decision(this.agent.getID(), this.agent.getPosition(), r.getIdentifier());
				ContextManager.getDecisionContext().add(d);
			} catch (NoIdentifierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


//		if (!ContextManager.inCalibrationMode()) {
//
//			try {
//				simpleLoadLogger.addVisitedToRoad(r.getIdentifier(), ContextManager.getGroupingFactor());
//			} catch (NoIdentifierException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		this.agent.setCurrentRoad(r);

		return r;

	}

}
