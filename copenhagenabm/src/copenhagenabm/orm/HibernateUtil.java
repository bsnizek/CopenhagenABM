package copenhagenabm.orm;


import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
//import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	
	private static final SessionFactory sessionFactory = buildSessionFactory();

//	private static SessionFactory buildSessionFactory() {
//		try {
//			Configuration c = new Configuration();
//			return new Configuration().
//					// addAnnotatedClass(Dot.class).
//					configure().buildSessionFactory();
//		}
//		catch (Throwable ex) {
//			throw new ExceptionInInitializerError(ex);
//		}
//	}
	
	private static SessionFactory buildSessionFactory() {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            return new AnnotationConfiguration().configure().buildSessionFactory();
 
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
	
	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

}