package copenhagenabm.tests.data;

import gnu.trove.TIntProcedure;

import java.util.ArrayList;

class ReturnArray implements TIntProcedure {

	private ArrayList<Integer> result = new ArrayList<Integer>();

	public boolean execute(int value) {
		this.result.add(value);
		return true;
	}

	ArrayList<Integer> getResult() {
		return this.result;
	}
}