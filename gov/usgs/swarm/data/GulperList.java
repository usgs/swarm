package gov.usgs.swarm.data;

import java.util.HashMap;
import java.util.Map;

/**
 * $Log: not supported by cvs2svn $
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
	
	public synchronized Gulper requestGulper(String key, GulperListener gl, SeismicDataSource source, String ch, double t1, double t2)
	{
		Gulper g = gulpers.get(key);
		if (g != null)
		{
			g.addListener(gl);
			g.update(t1, t2);	
		}
		else
		{
			System.out.println("Gulp: " + (t2 - t1));
			if (t2 - t1 < Gulper.GULP_SIZE)
			{
				System.out.println("Getting wave instead of gulping");
				source.getWave(ch, t1, t2);
			}
			else
			{
				g = new Gulper(this, key, gl, source, ch, t1, t2);
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
