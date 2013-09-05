package copenhagenabm.tests;

import copenhagenabm.loggers.PostgresLogger;

public class Test2 {
	
	public static void main(String[] args) throws Exception {

		Test2 t = new Test2();
		t.setUp();
	}

	private void setUp() {
		// TODO Auto-generated method stub
		PostgresLogger pl = new PostgresLogger();
		pl.setup();
	}

}
