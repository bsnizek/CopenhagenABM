/*
© Copyright 2012 Bernhard Snizek
This file is part of copenhagenABM.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
 */

package copenhagenabm.agent;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import copenhagenabm.loggers.DecisionTextLogger;
import copenhagenabm.main.ContextManager;
import copenhagenabm.main.MATRIX_TYPES;

/**
 * @author Bernhard Snizek, 2013 <b@snizek.com>
 * 
 */
public class DecisionMatrix {

	private Random generator = new Random();
	private HashMap<Object, Characteristic> characteristics = new HashMap<Object, Characteristic>();
	private HashMap<Object, Option> options = new HashMap<Object, Option>();
	private double sumWij;

	private static final boolean USE_MULTIPLICATION = (ContextManager.getDecisionMatrixStrategy() == MATRIX_TYPES.MULTIPLICATION);
	private static final boolean TAKE_MAX = (ContextManager.getDecisionMatrixStochasticity() == STOCHASTICTY_TYPES.OFF);

	private static final boolean DEBUG = ContextManager.getDecisionMatrixDebugMode();

	DecisionTextLogger decisionTextLogger = null;

	//	private String TAB = "\t\t\t\t";
	private String TAB = ";";
	DecimalFormat DF = new DecimalFormat("#.##");

	private int agentID;

	public DecisionMatrix(int agentID) {

		this.agentID = agentID;


		if (USE_MULTIPLICATION) {
			sumWij = 1.0;
		} else {
			sumWij = 0.0;
		}
		
		if (ContextManager.logToDecisionTextLogger()) {
			decisionTextLogger = ContextManager.getDecisionTextLogger(); 
		}
		
	}

	private class Option {

		private Object id;
		private HashMap<Characteristic, Cell> cells = new HashMap<Characteristic, Cell>();
		private double sum;
		private double probability = 0.0;
		private double finalP;

		public double getFinalP() {
			return finalP;
		}

		public Option(Object option_id) {
			id = option_id;
			if (USE_MULTIPLICATION) {
				sum = 1.0;
			} else {
				sum = 0.0;
			}
		}

		public Object getId() {
			return this.id;
		}

		public void put(Characteristic characteristic, Cell cell) {
			cells.put(characteristic, cell);
		}

		public void calculateWj() {
			Iterator<Cell> iterator = cells.values().iterator();
			while (iterator.hasNext()) {
				Cell cell = iterator.next();
				if (USE_MULTIPLICATION) {
					sum = sum * cell.getValue(); 
				} else {
					sum = sum + cell.getValue();
				}

			}
		}

		public double getWj() {
			return sum;
		}

		public void calculateProbj(double sumWij, ArrayList<Option> allOtherOptions) {
			//			probability = sum / sumWij;

			double s = 0.0d;
			for (Option o : allOtherOptions) {
				s = s + Math.exp(o.getWj());
			}

			probability = Math.exp(sumWij) / s; 

		}

		public double getProbj() {
			return probability;
		}

		public Cell getCell(Characteristic c) {
			return this.cells.get(c);
		}

		public void setFinalP(double d) {
			this.finalP = d;
		}

	}

	private class Characteristic {

		private Object id;

		public Characteristic(Object characteristic_id) {
			id = characteristic_id;
		}

		public Object getId() {
			return this.id;
		}

	}	

	private class Cell {

		private double value;
		// private Characteristic characteristic;
		// private Option option;

		public Cell(Characteristic characteristic, Option option, double value) {
			// this.option = option;
			// this.characteristic = characteristic;
			this.value = value;
		}

		public double getValue() {
			return this.value;
		}

	}

	public void addCharacteristic(Object id) {
		Characteristic c = new Characteristic(id);
		this.characteristics.put(id,c);		
		if (DEBUG) {
			// System.out.println("Characteristic " + id + " added.");
		}
	}

	public void addOption(Object id) {
		Option o = new Option(id);
		this.options.put(id, o);
	}	

	public void addCell(Object c, Object o, double value) {
		Option option = this.options.get(o);
		Characteristic characteristic = this.characteristics.get(c);

		Cell cell = new Cell(characteristic, 
				option, 
				value);
		option.put(characteristic, cell);
	}

	public void calculateSumWij() {
		Iterator<Option> iterator = this.options.values().iterator();
		while (iterator.hasNext()) {
			Option option = iterator.next();
			option.calculateWj();
			// this.sumWij = this.sumWij * option.getWj();
			this.sumWij = this.sumWij + option.getWj();

		}
	}

	public void calculateProbjs() {
		Iterator<Option> iterator = this.options.values().iterator();
		while (iterator.hasNext()) {
			Option option = iterator.next();

			ArrayList<Option> allOtherOptions = new ArrayList<Option>();
			Iterator<Option> iterator2 = this.options.values().iterator();
			while (iterator2.hasNext()) {
				Option o2 = iterator2.next();
				if (o2 != option) {
					allOtherOptions.add(o2);
				}
			}

			option.calculateProbj(this.sumWij, allOtherOptions);
		}
	}

	public void calculateFinalPs() {
		// get total sum 
		double sum = 0.0d;
		Iterator<Option> iterator = this.options.values().iterator();
		while (iterator.hasNext()) {
			Option option = iterator.next();
			double value = option.getProbj();
			sum = sum + value;
		}

		Iterator<Option> iterator2 = this.options.values().iterator();
		while (iterator2.hasNext()) {
			Option option = iterator2.next();
			option.setFinalP(option.getProbj()/sum);
		}
	}



	public void printMatrix() {


		Iterator<Option> options_iterator = this.options.values().iterator();
		Iterator<Characteristic> characteristics_iterator = this.characteristics.values().iterator();

		if (ContextManager.logToDecisionTextLogger()) {

			decisionTextLogger.logLine("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

			String line = "";

			line = line + "AGENTID" + TAB + "OPTION" + TAB;
			while (characteristics_iterator.hasNext()) {
				Characteristic c = characteristics_iterator.next();
				line = line + c.getId() + TAB;
			}

			decisionTextLogger.logLine(line + "Wj" + TAB + "Probj");

			while (options_iterator.hasNext()) {
				line = new Integer(this.agentID).toString() + TAB;
				Option o = options_iterator.next();
				line =line + o.getId() + TAB;
				characteristics_iterator = this.characteristics.values().iterator();
				while (characteristics_iterator.hasNext()) {
					Characteristic c = characteristics_iterator.next();
					Cell cell = o.getCell(c);

					if (cell!=null) {

						Double d = new Double(cell.value);

						line = line + DF.format(d) + TAB;
						
					} else 
					{
						line = line + "null" + TAB;
					}
				}
				decisionTextLogger.logLine(line + DF.format(o.getWj()) + TAB + DF.format(o.getProbj()) + TAB + DF.format(o.getFinalP()));
				line="";
			}
			// decisionTextLogger.logLine("Sum Wj \t" + this.sumWij);
			decisionTextLogger.logLine("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
		}	
	}


	public Object rollDice() {

		if (this.options.size() == 1) {

			return this.options.values().iterator().next().getId();

		}

		this.calculateSumWij();
		this.calculateProbjs();
		this.calculateFinalPs();
		double p = generator.nextDouble();

		Iterator<Option> iterator = this.options.values().iterator();

		if (TAKE_MAX) {
			double v = Double.MIN_VALUE;
			Object selected = null;
			Option option=null; 
			while (iterator.hasNext()) {
				option = iterator.next();
				double prob = option.getFinalP();
				if (prob>v) {
					v = prob;
					selected = option.getId();
				}
			}

			if (DEBUG) {
				this.printMatrix();

				if (ContextManager.logToDecisionTextLogger()) {
					decisionTextLogger.logLine("Option chosen: " + option.getId() + " (maximum value)");
					decisionTextLogger.logLine("====================================================================================================================================================================================");
				}

			}


			return selected;

		} else {



			// we calculate the sum of the sums of the options

			if (DEBUG) {
				this.printMatrix();
			}

			double p_sum = 0.0;
			double p_from;

			while (iterator.hasNext()) {
				Option option = iterator.next();
				p_from = p_sum;
				p_sum = p_sum + option.getFinalP();
				if ((p > p_from)
						&& (p < p_sum)) {
					if (DEBUG) {
						if (ContextManager.logToDecisionTextLogger()) {
							decisionTextLogger.logLine("Option chosen: " + option.getId() + " with p=" + p);
							decisionTextLogger.logLine("====================================================================================================================================================================================");
						}
					}
					return option.getId();
				}
			}
			return null;
		}
	}


}

