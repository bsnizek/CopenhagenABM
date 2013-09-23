package copenhagenabm.loggers;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.agent.IAgent;
import copenhagenabm.orm.Dot;
import copenhagenabm.orm.HibernateUtil;

//import org.hibernate.classic.Session;


public class PostgresLogger {

	Session session = null;
	SessionFactory sf = null;

	public void setup() {
		sf = HibernateUtil.getSessionFactory();
		session = sf.getCurrentSession();
		session.beginTransaction();	
		
		System.out.println("PostgresSQLLogger instantiated ! ");
	}

	public void log(int tick, IAgent agent) {
		Dot d = new Dot(tick, agent, agent.getPosition());
		//session.beginTransaction();
		session.save(d);

	}


	public void close() {
		session.getTransaction().commit();
	}

	/**
	 * commits, closes the session and opens another one
	 */
	public void commit() {
		session.getTransaction().commit();
		
		session = sf.getCurrentSession();
		session.beginTransaction();	
		
		System.out.println("commited");

	}
}
