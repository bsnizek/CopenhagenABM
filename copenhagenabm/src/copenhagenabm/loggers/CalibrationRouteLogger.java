package copenhagenabm.loggers;


import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import copenhagenabm.main.ContextManager;
import copenhagenabm.main.CalibrationModeData.CalibrationRoute;
import copenhagenabm.orm.CalibrationRouteDBObject;
import copenhagenabm.orm.HibernateUtil;

public class CalibrationRouteLogger {

	Session session = null;
	SessionFactory sf = null;

	public CalibrationRouteLogger() {
		setup();
	}
	
	public void setup() {
		sf = HibernateUtil.getSessionFactory();
		session = sf.getCurrentSession();
		session.beginTransaction();	

		System.out.println("CalibrationRouteLogger instantiated ! ");
	}

	public void log(ArrayList<CalibrationRoute> calibrationRoutes) {
		for (CalibrationRoute c : calibrationRoutes) {
			CalibrationRouteDBObject cRDBO = new CalibrationRouteDBObject(c);
			session.save(cRDBO);
		}
	}

	public void close() {
		if (ContextManager.isCalibrationRouteLoggerOn()) {
			try {
			session.getTransaction().commit();
			} catch(Exception e) {
				e.printStackTrace();
			}
			System.out.println("CalibrationRouteLog commited and closed.");
		}
	}

	/**
	 * commits, closes the session and opens another one
	 */
	public void commit() {
		session.getTransaction().commit();

		session = sf.getCurrentSession();
		session.beginTransaction();	

	}


}
