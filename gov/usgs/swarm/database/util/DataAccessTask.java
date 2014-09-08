package gov.usgs.swarm.database.util;

import java.sql.SQLException;

/**
 * Interface for a database task.
 * 
 * @author Chirag Patel
 *
 * @param <T> type of result of task
 * @param <V> type of intermediate results
 */
public interface DataAccessTask<T, V>
{
	T doInBackground(DataAccessSwingWorker<T, V> workerThread) throws Exception;

	void done(T taskResult);

	void cancel() throws SQLException;
}