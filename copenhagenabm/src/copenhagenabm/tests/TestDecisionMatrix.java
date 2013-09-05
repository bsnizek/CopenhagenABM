package copenhagenabm.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import copenhagenabm.agent.DecisionMatrix;
import copenhagenabm.main.ContextManager;

public class TestDecisionMatrix {

	
	private DecisionMatrix dm;

	public void setup() {
		
		int agentID = 0;
		setDm(new DecisionMatrix(agentID));
	}
	
	public void setupCharacteristics() {
		getDm().addCharacteristic("left");
		getDm().addCharacteristic("right");
	}
	
	public void addOptions() {
		getDm().addOption("R1");
		getDm().addOption("R2");
	}
	
	public void addCells() {
		getDm().addCell("left", "R1", 0.1);
		getDm().addCell("right", "R1", 0.2);
		getDm().addCell("left", "R2", 0.2);
		getDm().addCell("right", "R2", 0.3);
		
	}

	public DecisionMatrix getDm() {
		return dm;
	}

	public void setDm(DecisionMatrix dm) {
		this.dm = dm;
	}
	
	public static void main(String[] args) {
		ContextManager contextManager = new ContextManager();
		try {
			contextManager.readProperties();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TestDecisionMatrix dm = new TestDecisionMatrix();
		dm.setup();
		dm.setupCharacteristics();
		dm.addOptions();
		dm.addCells();
		
		Object xx = dm.getDm().rollDice();
		
		System.out.println(xx);
	}
	
	

}
