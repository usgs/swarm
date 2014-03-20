package gov.usgs.swarm.data;

import gov.usgs.plot.data.Wave;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Dan Cervelli
 */
public class Gulper implements Runnable
{
	private final SeismicDataSource gulpSource;
	private final GulperList gulperList;
	private final  String channel;
	private double lastTime;
	private double goalTime;
	private Thread thread;
	private final String key;
	private final Set<GulperListener> listeners;
	
	private final int gulpSize;
	private final int gulpDelay;

	/**
	 * Create the gulper. This does not call methods to support subclassing.
	 * @param gl the gulper list.
	 * @param k the key.
	 * @param ch the channel.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @param size the gulper size.
	 * @param delay the gulper delay.
	 */
	public Gulper(GulperList gl, String k, SeismicDataSource source, String ch, double t1, double t2, int size, int delay)
	{
		gulpSize = size;
		gulpDelay = delay;
		gulperList = gl;
		gulpSource = source;
		key = k;
		listeners = new HashSet<GulperListener>();
		channel = ch;
		lastTime = t2;
		
		double now = CurrentTime.getInstance().nowJ2K();
		if (lastTime > now)
			lastTime = now;
	}

	/**
	 * Create the gulper and start it.
	 * @param gl the gulper list.
	 * @param k the key.
	 * @param glnr the gulper listener.
	 * @param source the seismic data source.
	 * @param ch the channel.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @param size the gulper size.
	 * @param delay the gulper delay.
	 * @deprecated use {@link #Gulper(GulperList, String, SeismicDataSource, String, double, double, int, int)} that does not call methods to support subclassing.
	 */
	public Gulper(GulperList gl, String k, GulperListener glnr, SeismicDataSource source, String ch, double t1, double t2, int size, int delay)
	{
		this(gl, k, source, ch, t1, t2, size, delay);
		addListener(glnr);
		update(t1, t2);
		start();
	}

	public synchronized void addListener(GulperListener gl)
	{
		listeners.add(gl);
	}
	
	public synchronized void removeListener(GulperListener gl)
	{
		listeners.remove(gl);
	}
	
	public String getChannel()
	{
		return channel;
	}
	
	public String getKey()
	{
		return key;
	}
	
	/**
	 * Kill this gulper.
	 */
	protected void kill()
	{
		// use local copy to ensure it isn't changed elsewhere,
		// no need to lock since OK to have multiple calls to Thread.interrupt
		final Thread t = thread;
		if (t != null)
		{
			thread = null;
			t.interrupt();
		}
	}

	/**
	 * Determine if this gulper has been killed.
	 * @return true if the gulper has been killed or was never started.
	 */
	public boolean isKilled()
	{
		return thread == null;
	}

	public void kill(GulperListener gl)
	{
		removeListener(gl);
		if (listeners.size() == 0)
		{
			kill();
		}
	}
	
	public void start()
	{
		thread = new Thread(this);
		thread.start();
		Swarm.logger.finer("gulper started for " + channel);
	}

	public void update(double t1, double t2)
	{
		CachedDataSource cache = CachedDataSource.getInstance();
		if (t2 < lastTime)
			lastTime = t2;
		goalTime = t1;

		while (cache.inHelicorderCache(channel, lastTime - gulpSize, lastTime) && lastTime > goalTime && !isKilled())
		{
			lastTime -= gulpSize;
			lastTime += 10;	
		}
	}
	
	protected synchronized void fireStarted()
	{
		for (GulperListener listener : listeners)
			listener.gulperStarted();
	}
	
	protected synchronized void fireGulped(double t1, double t2, Wave w)
	{
		fireGulped(t1, t2, w != null && !isKilled());
	}

	protected synchronized void fireGulped(double t1, double t2, boolean success)
	{
		for (GulperListener listener : listeners)
			listener.gulperGulped(t1, t2, success);
	}
	
	protected synchronized void fireStopped()
	{
		final boolean killed = isKilled();
		for (GulperListener listener : listeners)
			listener.gulperStopped(killed);
	}
	
	public void run()
	{
		fireStarted();
		runLoop();	
		gulpSource.close();
		if (isKilled())
			Swarm.logger.finest("gulper killed");
		else
			Swarm.logger.finest("gulper finished");
		gulperList.removeGulper(this);
		fireStopped();
	}

	/**
	 * This is the run loop which a subclass may override.
	 */
	protected void runLoop()
	{
		while (lastTime > goalTime && !isKilled())
		{
			try
			{
				double t1 = lastTime - gulpSize;
				double t2 = lastTime;
				Wave w = gulpSource.getWave(channel, t1, t2);
				fireGulped(t1, t2, w);
				update(goalTime, lastTime - gulpSize + 10);
			}
			catch (Throwable e)
			{
				System.err.println("Exception during gulp:");
				e.printStackTrace();	
			}
			delay();
		}
	}

	protected void delay()
	{
		if (!isKilled())
			try { Thread.sleep(gulpDelay); } catch (Exception e) {}
	}
	
	public String toString()
	{
		return channel;	
	}
}