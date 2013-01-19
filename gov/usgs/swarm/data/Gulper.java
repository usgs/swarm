package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.wave.Wave;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Dan Cervelli
 */
public class Gulper extends Thread
{
	private SeismicDataSource gulpSource;
	private GulperList gulperList;
	private String channel;
	private double lastTime;
	private double goalTime;
	private boolean killed;
	private String key;
	private Set<GulperListener> listeners;
	
	private int gulpSize;
	private int gulpDelay;
	
	public Gulper(GulperList gl, String k, GulperListener glnr, SeismicDataSource source, String ch, double t1, double t2, int size, int delay)
	{
		gulpSize = size;
		gulpDelay = delay;
		gulperList = gl;
		gulpSource = source;
		key = k;
		listeners = new HashSet<GulperListener>();
		addListener(glnr);
		channel = ch;
		lastTime = t2;
		
		double now = CurrentTime.getInstance().nowJ2K();
		if (lastTime > now)
			lastTime = now;
		
		update(t1, t2);
		
		start();
		Swarm.logger.finer("gulper started for " + channel);
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
	
	public void kill(GulperListener gl)
	{
		removeListener(gl);
		if (listeners.size() == 0)
		{
			killed = true;	
			interrupt();
		}
	}
	
	public void update(double t1, double t2)
	{
		CachedDataSource cache = CachedDataSource.getInstance();
		if (t2 < lastTime)
			lastTime = t2;
		goalTime = t1;

		while (cache.inHelicorderCache(channel, lastTime - gulpSize, lastTime) && lastTime > goalTime && !killed)
		{
			lastTime -= gulpSize;
			lastTime += 10;	
		}
	}
	
	private synchronized void fireStarted()
	{
		for (GulperListener listener : listeners)
			listener.gulperStarted();
	}
	
	private synchronized void fireGulped(double t1, double t2, boolean success)
	{
		for (GulperListener listener : listeners)
			listener.gulperGulped(t1, t2, success);
	}
	
	private synchronized void fireStopped(boolean killed)
	{
		for (GulperListener listener : listeners)
			listener.gulperStopped(killed);
	}
	
	public void run()
	{
		fireStarted();
		
		while (lastTime > goalTime && !killed)
		{
			try
			{
				double t1 = lastTime - gulpSize;
				double t2 = lastTime;
				Wave w = gulpSource.getWave(channel, t1, t2);
				fireGulped(t1, t2, w != null && !killed);
				update(goalTime, lastTime - gulpSize + 10);
			}
			catch (Throwable e)
			{
				System.err.println("Exception during gulp:");
				e.printStackTrace();	
			}
			
			if (!killed)
				try { Thread.sleep(gulpDelay); } catch (Exception e) {}
		}	
		
		gulpSource.close();
		
		if (killed)
			Swarm.logger.finest("gulper killed");
		else
			Swarm.logger.finest("gulper finished");
			
		gulperList.removeGulper(this);
		
		fireStopped(killed);
	}
	
	public String toString()
	{
		return channel;	
	}
}