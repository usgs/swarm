package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.wave.Wave;

/**
 * $Log: not supported by cvs2svn $
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
	private GulperListener listener;
	
	public static final int GULP_SIZE = 30 * 60;
	public static final int WAIT_INTERVAL = 1 * 1000;
	
	public Gulper(GulperList gl, String k, GulperListener glnr, SeismicDataSource source, String ch, double t1, double t2)
	{
		gulperList = gl;
		gulpSource = source;
		key = k;
		listener = glnr;
		channel = ch;
		lastTime = t2;
		
		double now = CurrentTime.getInstance().nowJ2K();
		if (lastTime > now)
			lastTime = now;
		
		update(t1, t2);
		
		start();
		System.out.println("Gulper started for " + channel);
	}
	
	public String getChannel()
	{
		return channel;
	}
	
	public String getKey()
	{
		return key;
	}
	
	public void kill()
	{
		System.out.println("kill!");
		killed = true;	
		interrupt();
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
	
	public void run()
	{
		if (listener != null)
			listener.gulperStarted();
		
		while (lastTime > goalTime && !killed)
		{
			try
			{
				double t1 = lastTime - GULP_SIZE;
				double t2 = lastTime;
				System.out.println("gulper: getWave");
				Wave w = gulpSource.getWave(channel, t1, t2);
				if (listener != null)
					listener.gulperGulped(t1, t2, w != null && !killed);
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
		
		if (listener != null)
			listener.gulperStopped(killed);
	}
	
	public String toString()
	{
		return channel;	
	}
}