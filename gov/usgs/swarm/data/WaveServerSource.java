package gov.usgs.swarm.data;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.earthworm.WaveServer;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
 
/**
 * An implementation of <code>SeismicDataSource</code> that connects to an
 * Earthworm Wave Server.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2006/04/15 16:00:13  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.2  2005/09/02 16:40:29  dcervelli
 * CurrentTime changes.
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.1  2005/05/02 16:22:10  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.4  2005/04/25 15:07:55  cervelli
 * Fixed kiosk null pointer bug in isSCNL().
 *
 * Revision 1.3  2005/04/23 15:54:17  cervelli
 * Uses space delimiting instead of underscore.  Handles -- locations better.
 *
 * Revision 1.2  2005/04/16 20:57:18  cervelli
 * SCNL changes.
 *
 * @author Dan Cervelli
 */
public class WaveServerSource extends SeismicDataSource
{
	private String params;
	private WaveServer waveServer;
	private int timeout = 2000;
	
	private String server;
	private int port;
	
	private static Map<String, Boolean> scnlSources = new HashMap<String, Boolean>();
	
	public WaveServerSource(String s)
	{
		params = s;
		String[] ss = params.split(":");
		server = ss[0];
		port = Integer.parseInt(ss[1]);
		if (ss.length == 3)
			timeout = Integer.parseInt(ss[2]);
		
		waveServer = new WaveServer(server, port);
		setTimeout(timeout);
	}
	
	public WaveServerSource(WaveServerSource wss)
	{
		this(wss.params);
	}
 
	public SeismicDataSource getCopy()
	{
		return new WaveServerSource(this);	
	}

	public String toConfigString()
	{
		return String.format("%s;ws:%s:%d:%d", name, server, port, timeout);
	}
	
	public boolean isSCNL(String p)
	{
		Boolean b = scnlSources.get(p);
		if (b == null)
		{
			getMenu();
			b = scnlSources.get(p);
			if (b == null)
				return false;
		}
		
		return b.booleanValue();
	}

	public static void setIsSCNL(String p, boolean b)
	{
		scnlSources.put(p, b);
	}
	
	public synchronized void setTimeout(int to)
	{
		waveServer.setTimeout(to);
	}
	
	public synchronized void close()
	{
		if (waveServer != null)
			waveServer.close();
	}
	
	public synchronized Menu getMenu()
	{
		Menu menu = waveServer.getMenuSCNL();
		setIsSCNL(params, menu.isSCNL());
		return menu;
	}
	
	public String getFormattedSCNL(MenuItem mi)
	{
		String scnl = mi.getStation() + " " + mi.getChannel() + " " + mi.getNetwork();
		if (isSCNL(params))
		{
			String loc = mi.getLocation();
			if (loc != null && !loc.equals("--"))
				scnl = scnl + " " + loc;
		}
		return scnl;
	}
	
	public List<String> getMenuList(List items)
	{
		List<String> list = new ArrayList<String>(items.size());
		for (Iterator it = items.iterator(); it.hasNext(); )
		{
			MenuItem mi = (MenuItem)it.next();
			list.add(getFormattedSCNL(mi));
		}
		return list;
	}
	
	public synchronized Wave getWave(String station, double t1, double t2)
	{
		CachedDataSource cache = Swarm.getCache();
		Wave sw = cache.getWave(station, t1, t2);
		if (sw == null)
		{
			String[] ss = station.split(" ");
			String loc = null;
			if (isSCNL(params))
			{
				loc = "--";
				if (ss.length == 4)
					loc = ss[3];
			}
			sw = waveServer.getRawData(ss[0], ss[1], ss[2], loc, Util.j2KToEW(t1), Util.j2KToEW(t2));
			if (sw == null)
				return null;
			sw.convertToJ2K();
			sw.register();
			cache.cacheWaveAsHelicorder(station, sw);
			cache.putWave(station, sw);
		}
		else
		{
			//System.out.println("cached");	
		}
		return sw;
	}
	
	public synchronized List<String> getChannels()
	{
		Menu menu = getMenu();
		return getMenuList(menu.getSortedItems());
	}
	
	public synchronized HelicorderData getHelicorder(String station, double t1, double t2)
	{
		double now = CurrentTime.getInstance().nowJ2K();
		// if a time later than now has been asked for make sure to get the latest 
		if ((t2 - now) >= -20)
			getWave(station, now - 2*60, now);	
		
		CachedDataSource cache = Swarm.getCache();
		HelicorderData hd = cache.getHelicorder(station, t1, t2);	

		if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 0))
			requestGulper(station, t1, t2);
		return hd;
	}
	
	public synchronized void notifyDataNotNeeded(String station, double t1, double t2)
	{
		stopGulper(station);
	}
	
	private final static int MAX_GULPERS = 5;
	private List<Gulper> gulpers;
	
	public synchronized boolean isActiveSource()
	{
		return (gulpers != null && gulpers.size() > 0);	
	}
	
	public synchronized void requestGulper(String ch, double t1, double t2)
	{
		if (gulpers == null)
			gulpers = new ArrayList<Gulper>();
			
		boolean allowed = true;
//		for (int i = 0; i < gulpers.size(); i++)
		for (Gulper g : gulpers)
		{
//			Gulper g = (Gulper)gulpers.elementAt(i);
			if (g.channel == ch)	
			{
				allowed = false;
				g.update(t1, t2);
				break;
			}
		}
		
		if (gulpers.size() >= MAX_GULPERS)
			allowed = false;
		
		if (allowed)
		{
			Gulper g = new Gulper(ch, t1, t2);
			gulpers.add(g);
		}
	}

	public synchronized void removeGulper(Gulper g)
	{
		gulpers.remove(g);	
	}
	
	public synchronized void stopGulper(String ch)
	{
		if (gulpers == null)
			return;
		for (Gulper g : gulpers)
		{
			if (g.channel == ch)	
			{
				g.kill();
				break;
			}
		}
	}
	
	class Gulper extends Thread
	{
		private WaveServerSource gulpSource;
		private String channel;
		private double lastTime;
		private double goalTime;
		private boolean killed;
		
		private int GULP_SIZE = 15 * 60;
		private int WAIT_INTERVAL = 1 * 1000;
		
		public Gulper(String ch, double t1, double t2)
		{
			channel = ch;
			lastTime = t2;
			
			double now = CurrentTime.getInstance().nowJ2K();
			if (lastTime > now)
				lastTime = now;
			
			update(t1, t2);
			
			gulpSource = new WaveServerSource(params);
			gulpSource.setTimeout(30*1000);
			start();
			System.out.println("Gulper started for " + channel);
		}
		
		public void kill()
		{
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
			while (lastTime > goalTime && !killed)
			{
				try
				{
					if (gulpSource.getWave(channel, lastTime - GULP_SIZE, lastTime) != null)
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
				
			removeGulper(this);
		}
		
		public String toString()
		{
			return channel;	
		}
	}
	
}