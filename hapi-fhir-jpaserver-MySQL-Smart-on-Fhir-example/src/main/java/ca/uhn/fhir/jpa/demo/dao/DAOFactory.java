package ca.uhn.fhir.jpa.demo.dao;

import java.io.InputStream;
import java.util.Properties;

public class DAOFactory {
	private static Properties __props;

	//private static DAO __theDAO;

	private DAOFactory() {}

	/*public static DAO getTheDAO() throws DAOException {
		if (__theDAO == null) {
			throw new DAOException("DAO not properly initialized");
		}
		return __theDAO;
	}*/

	/**
	 * We should really create a config object
	 * @return
	 */
	public static Properties getDAOProperties() {
		return __props;
	}

	// static initalizer of the dao and the related properties
	static {
        __props = new Properties();
        try {
        		InputStream propFile = DAOFactory.class.getResourceAsStream("dao.properties");
        		__props.load(propFile);
        		propFile.close();

        		/*String daoClass = __props.getProperty("dao.class");

        		if (daoClass != null) {
        			Class<?> daoClazz = Class.forName(daoClass);
        			//get constructor
        			Constructor<?> constructor = daoClazz.getConstructor(java.util.Properties.class);
        			__theDAO = (DAO) constructor.newInstance(__props);*/
            
        } catch (Throwable t) {
            t.printStackTrace();
            try {
				throw new DAOException(t);
			} catch (DAOException e) {
				e.printStackTrace();
			}
        }
	}

}
