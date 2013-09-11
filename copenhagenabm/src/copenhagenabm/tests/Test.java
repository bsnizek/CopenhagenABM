package copenhagenabm.tests;

import org.hibernate.Session;

import com.vividsolutions.jts.geom.Coordinate;

import copenhagenabm.orm.Dot;
import copenhagenabm.orm.HibernateUtil;

public class Test {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		Test t = new Test();
		t.setUp();
		t.addDot();
		System.out.println("Done.");
	}


	protected void setUp() throws Exception {
		

	}
	
	public void addDot() {
//		Dot d = new Dot(0, 0, new Coordinate(0.0,0.0));
//		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
//		session.beginTransaction();
//		session.save(d);
//		session.getTransaction().commit();
	}
}
