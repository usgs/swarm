package gov.usgs.swarm.database.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Holds database connection configuration information.
 * 
 * @author Chirag Patel
 */
public final class ApplicationConnection
{
	private String dbUrl;
	private String userName;
	private String password;
	private Connection connection;

	public ApplicationConnection(String dbUrl, String userName, String password) throws SQLException
	{
		this.dbUrl = dbUrl;
		this.userName = userName;
		this.password = password;
		System.out.println("before establishing connection");
		this.connection = establishConnection();
		System.out.println("after establishing connection");
	}

	private Connection establishConnection() throws SQLException
	{
		return DriverManager.getConnection(dbUrl, userName, password);
	}

	public void commit() throws SQLException
	{
		if (!getConnection().getAutoCommit())
			getConnection().commit();
	}

	public String getDbUrl()
	{
		return dbUrl;
	}

	public String getUserName()
	{
		return userName;
	}

	public String getPassword()
	{
		return password;
	}

	public void closeConnection() throws SQLException
	{
		getConnection().close();
	}

	public boolean isClosed() throws SQLException
	{
		return getConnection().isClosed();
	}

	public Connection getConnection()
	{
		return connection;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dbUrl == null) ? 0 : dbUrl.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationConnection other = (ApplicationConnection) obj;
		if (dbUrl == null) {
			if (other.dbUrl != null)
				return false;
		} else if (!dbUrl.equals(other.dbUrl))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
}