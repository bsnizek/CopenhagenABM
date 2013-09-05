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
	
	public void setup() {
		SessionFactory sf = HibernateUtil.getSessionFactory();
		session = sf.getCurrentSession();
		session.beginTransaction();	
		System.out.println("PostgresSQLLogger instantiated ! ");
	}
	
	 public void log(int tick, IAgent agent, Coordinate coordinate) {
		 Dot d = new Dot(tick, agent, coordinate);
		 //session.beginTransaction();
		 session.save(d);
		 
	 }

	 
	 public void close() {
		 session.getTransaction().commit();
		 // session.disconnect();
	 }
}
