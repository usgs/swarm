package gov.usgs.swarm.data;

/**
 * This interface defines the Gulper methods.
 * 
 * @author Kevin Frechette (ISTI)
 */
public interface IGulper extends Runnable
{
	/**
	 * Add the gulper listener.
	 * 
	 * @param gl the gulper listener.
	 */
	public void addListener(GulperListener gl);

	/**
	 * Get the key used in the gulper list.
	 * 
	 * @return the key.
	 */
	public String getKey();

	/**
	 * Remove the gulper listener and kill the gulper if no more listeners.
	 * 
	 * @param gl the gulper listener.
	 */
	public void kill(GulperListener gl);

	/**
	 * Start the gulper.
	 */
	public void start();

	/**
	 * Update the gulper start and end times.
	 * 
	 * @param t1 the start time.
	 * @param t2 the end time.
	 */
	public void update(double t1, double t2);
}