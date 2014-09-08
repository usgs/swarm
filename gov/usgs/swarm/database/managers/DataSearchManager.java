package gov.usgs.swarm.database.managers;

import gov.usgs.swarm.database.model.ApplicationConnection;
import gov.usgs.swarm.database.model.ResultSetModel;
import gov.usgs.swarm.database.model.SearchQueryCriteria;
import gov.usgs.swarm.database.util.DataAccessResult;
import gov.usgs.swarm.database.util.DataAccessSwingWorker;
import gov.usgs.swarm.database.util.DataAccessTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Handle searching for events in the database.
 * 
 * @author Chirag Patel
 */
public class DataSearchManager extends AbstractDataAccessManager
{
	private ApplicationConnection appConnection = null;

	public DataSearchManager(ApplicationConnection connection)
	{
		this.appConnection = connection;
	}

	public void doSearch(final SearchQueryCriteria searchQueryCriteria,
			final DataAccessResult<ResultSetModel> dataAccessResult)
	{
		super.executeLongOperation(new DataAccessTask<ResultSetModel, Void>()
		{
			private PreparedStatement statement = null;

			@Override
			public ResultSetModel doInBackground(DataAccessSwingWorker<ResultSetModel, Void> workerThread)
					throws SQLException, ClassNotFoundException
			{
				Connection connection = null;
				try {
					connection = appConnection.getConnection();
					statement = connection
							.prepareStatement(
									"select distinct concat(isNull(e.user_label,''),'(',isNull(e.event_id,''),')') as event,a.attempt_id,a.preferred,a.duration,a.latitude,a.longitude from event e,attempt a, marker m where (e.user_label=? or ?) and (m.marker_time>=? or ?) and (m.marker_time<=? or ?) and (a.duration>=? or ?) and (a.duration<=? or ?) and (e.event_id=a.event_id) and (m.attempt_id=a.attempt_id)",
									ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					String userLabel = searchQueryCriteria.getUserLabel();
					boolean isNull = userLabel == null;
					if (isNull) {
						statement.setNull(1, Types.NULL);
					} else {
						statement.setString(1, userLabel);
					}
					statement.setBoolean(2, isNull);
					Timestamp startTime = searchQueryCriteria.getStartTime();
					isNull = startTime == null;
					if (isNull) {
						statement.setNull(3, Types.NULL);
					} else {
						statement.setTimestamp(3, startTime);
					}
					statement.setBoolean(4, isNull);
					Timestamp endTime = searchQueryCriteria.getEndTime();
					isNull = endTime == null;
					if (isNull) {
						statement.setNull(5, Types.NULL);
					} else {
						statement.setTimestamp(5, endTime);
					}
					statement.setBoolean(6, isNull);
					Long durationMin = searchQueryCriteria.getDurationMin();
					isNull = durationMin == null;
					if (isNull) {
						statement.setNull(7, Types.NULL);
					} else {
						statement.setLong(7, durationMin);
					}
					statement.setBoolean(8, isNull);
					Long durationMax = searchQueryCriteria.getDurationMax();
					isNull = durationMax == null;
					if (isNull) {
						statement.setNull(9, Types.NULL);
					} else {
						statement.setLong(9, durationMax);
					}
					statement.setBoolean(10, isNull);
					workerThread.setProgressValue(60);
					ResultSet resultSet = statement.executeQuery();
					workerThread.setProgressValue(90);
					return createResultSetModel(resultSet);
				} finally {
					if (statement != null) {
						statement.close();
					}
				}
			}

			@Override
			public void done(ResultSetModel taskResult)
			{
				dataAccessResult.done(taskResult);
			}

			@Override
			public void cancel() throws SQLException
			{
				if (statement != null) {
					try {
						statement.cancel();
					} finally {
						statement.close();
					}
				}
			}
		});
	}

	public void executeUpdateQuery(String sql, Object[] parameters) throws SQLException
	{
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = appConnection.getConnection();
			statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			for (int i = 0; i < parameters.length; i++) {
				statement.setObject(i + 1, parameters[i]);
			}
			statement.executeUpdate();
		} finally {
			if (statement != null) {
				statement.close();
			}
		}
		// printAttempts();
	}

	private ResultSetModel createResultSetModel(ResultSet resultSet) throws SQLException, ClassNotFoundException
	{
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		String[] columnNames = new String[columnCount];
		Class<?>[] columnClasses = new Class[columnCount];

		for (int i = 0; i < columnCount; i++) {
			columnNames[i] = metaData.getColumnLabel(i + 1);
			columnClasses[i] = Class.forName(metaData.getColumnClassName(i + 1), false, getClass().getClassLoader());
		}
		int rowCount = 0;
		resultSet.beforeFirst();
		while (resultSet.next()) {
			rowCount++;
		}
		resultSet.beforeFirst();
		Object[][] data = new Object[rowCount][columnCount];
		rowCount = 0;
		while (resultSet.next()) {
			data[rowCount] = new Object[columnCount];
			for (int i = 0; i < columnCount; i++) {
				data[rowCount][i] = resultSet.getObject(i + 1);
			}
			rowCount++;
		}
		return new ResultSetModel(columnNames, columnClasses, data, columnCount);
	}
}