package gov.usgs.swarm.database.util;

/**
 * Used in database searching.
 * 
 * @author Chirag Patel
 * @param <T> type of result of task
 */
public interface DataAccessResult<T>
{
	public void done(T taskResult);
}