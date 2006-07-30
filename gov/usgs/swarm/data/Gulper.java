package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.wave.Wave;

import java.util.HashSet;
import java.util.Set;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/07/26 00:36:03  cervelli
 * Changes for new gulper system.
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
	
	public static final int GULP_SIZE = 30 * 60;
	public static final int WAIT_INTERVAL = 1 * 1000;
	
	public Gulper(GulperList gl, String k, GulperListener glnr, SeismicDataSource source, String ch, double t1, double t2)
	{
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
		System.out.println("Gulper started for " + channel);
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
		System.out.println("kill!");
		removeListener(gl);
		if (listeners.size() == 0)
		{
			killed = true;	
			interrupt();
		}
	}
	
	public void update(double t1, double t2)
	{
		CachedDataSource cache = Swarm.getCache();
		if (t2 < lastTime)
			lastTime = t2;
		goalTime = t1;

		while (cache.inHelicorderCache(channel, lastTime - GULP_SIZE, lastTime) && lastTime > goalTime && !killed)
		{
			lastTime -= GULP_SIZE;
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
				double t1 = lastTime - GULP_SIZE;
				double t2 = lastTime;
				System.out.println("gulper: getWave");
				Wave w = gulpSource.getWave(channel, t1, t2);
				fireGulped(t1, t2, w != null && !killed);
				update(goalTime, lastTime - GULP_SIZE + 10);
			}
			catch (Exception e)
			{
				System.err.println("Exception during gulp:");
				e.printStackTrace();	
			}
			
			if (!killed)
				try { Thread.sleep(WAIT_INTERVAL); } catch (Exception e) {}
		}	
		
		gulpSource.close();
		
		if (killed)
			System.out.println("Gulper killed");
		else
			System.out.println("Gulper finished");
			
		gulperList.removeGulper(this);
		
		fireStopped(killed);
	}
	
	public String toString()
	{
		return channel;	
	}
}