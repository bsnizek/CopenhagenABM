package copenhagenabm.loggers;


import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import copenhagenabm.main.ContextManager;
import copenhagenabm.main.CalibrationModeData.CalibrationRoute;
import copenhagenabm.orm.CalibrationRouteDBObject;
import copenhagenabm.orm.HibernateUtil;


/**
 * CalibrationRouteLogger
 * 
 * @author besn
 *
 * The CalibrationRouteLogger logs routes and route information stemming from every 
 * calibration agent into a table "calibrationroute". 
 *
 */
public class CalibrationRouteLogger {

	Session session = null;
	SessionFactory sf = null;
	int nRoutes = 0;

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
		nRoutes = calibrationRoutes.size();
		System.out.println("(" + ContextManager.getCurrentTick() +") logging " + nRoutes + " routes.");
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
			System.out.println("CalibrationRouteLog has logged " + nRoutes + " routes.");
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
