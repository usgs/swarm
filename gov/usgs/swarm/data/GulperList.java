package gov.usgs.swarm.data;

import java.util.HashMap;
import java.util.Map;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2006/08/07 22:36:23  cervelli
 * Removed chatter.
 *
 * Revision 1.3  2006/07/30 22:45:12  cervelli
 * Fixes bug with multiple helicorders using the same gulper.
 *
 * Revision 1.2  2006/07/26 00:36:02  cervelli
 * Changes for new gulper system.
 *
 * @author Dan Cervelli
 */
public class GulperList
{
	private Map<String, Gulper> gulpers;
	private static GulperList gulperList;
	
	private GulperList()
	{
		gulpers = new HashMap<String, Gulper>();
	}
	
	public static GulperList getInstance()
	{
		if (gulperList == null)
			gulperList = new GulperList();
		
		return gulperList;
	}
	
	public synchronized Gulper requestGulper(String key, GulperListener gl, SeismicDataSource source, String ch, double t1, double t2, int size, int delay)
	{
		Gulper g = gulpers.get(key);
		if (g != null)
		{
			g.addListener(gl);
			g.update(t1, t2);	
		}
		else
		{
			if (t2 - t1 < size)
			{
				source.getWave(ch, t1, t2);
			}
			else
			{
				g = source.createGulper(this, key, ch, t1, t2, size, delay);
				g.addListener(gl);
				g.update(t1, t2);
				g.start();
				gulpers.put(key, g);
			}
		}
		return g;
	}

	public synchronized void killGulper(String key, GulperListener gl)
	{
		Gulper g = gulpers.get(key);
		if (g != null)
			g.kill(gl);	
	}
	
	/**
	 * Called from the gulper.
	 * @param g
	 */
	public synchronized void removeGulper(Gulper g)
	{
		gulpers.remove(g.getKey());
	}
}
