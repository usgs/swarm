package gov.usgs.swarm.database.util;

import gov.usgs.swarm.Swarm;

import javax.sql.DataSource;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.Session;
import org.h2.jdbcx.JdbcDataSource;

public class SessionCustomize implements SessionCustomizer {
	  public void customize(Session session) {
		  DataSource ds = getDataSource();
		  if(null == ds)
			  return;
	    session.getLogin().setConnector(new JNDIConnector(ds));
	 
	    for (ClassDescriptor descriptor : session.getDescriptors().values()) {
	      descriptor.getCachePolicy().setIdentityMapSize(1000);
	    }
	  }
	  public static DataSource getDataSource() {		  	
	  		JdbcDataSource dataSource = new JdbcDataSource();	
			dataSource.setURL("jdbc:h2:"+Swarm.DBNAME);
			dataSource.setUser(Swarm.USER);
			dataSource.setPassword(Swarm.PASSWORD);
			return dataSource;
		}
}
