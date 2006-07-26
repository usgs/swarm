package gov.usgs.swarm.data;

import gov.usgs.swarm.Images;
import gov.usgs.swarm.Swarm;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.util.List;

import javax.swing.Icon;

/**
 * Base class for seismic data sources.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2006/07/22 20:20:16  cervelli
 * Added variable for determining if source should be saved in user config.
 *
 * Revision 1.3  2006/06/05 18:07:03  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.2  2006/04/15 16:00:13  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2005/05/08 16:10:40  cervelli
 * Changes for renaming of WWS.
 *
 * Revision 1.1  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.4  2005/03/24 22:08:35  cervelli
 * Removed support for Winston Server, added support for WWS, and Direct WWS.
 *
 * Revision 1.3  2004/10/28 20:22:59  cvs
 * Some comments.
 *
 * Revision 1.2  2004/10/23 19:34:46  cvs
 * Added support for SAC files.
 * 
 * @author Dan Cervelli
 */
abstract public class SeismicDataSource
{
	protected String name = "Unnamed Data Source";
	
	protected boolean storeInUserConfig = true;
	
	abstract public List<String> getChannels();
	
	/**
	 * Either returns the wave successfully or null if the data source could
	 * not get the wave.
	 * 
	 * @param station
	 * @param t1
	 * @param t2
	 * @return
	 */
	abstract public Wave getWave(String station, double t1, double t2);
	abstract public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl);
	
	public void notifyDataNotNeeded(String station, double t1, double t2)
	{}
	
	public void setStoreInUserConfig(boolean b)
	{
		storeInUserConfig = b;
	}
	
	public boolean isStoreInUserConfig()
	{
		return storeInUserConfig;
	}
	
	/**
	 * Is this data source active; that is, is new data being added in real-time
	 * to this data source?
	 * @return whether or not this is an active data source
	 */
	public boolean isActiveSource()
	{
		return false;	
	}
	
	/**
	 * Close the data source.
	 */
	public void close() {}

	/**
	 * Get a copy of the data source.  The default implementation returns an
	 * identical copy, that is, <code>this</code>.
	 * @return the identical data source (this)
	 */
	public SeismicDataSource getCopy()
	{
		return this;	
	}
	
	/**
	 * Get a string representation of this data source.  The default implementation
	 * return the name of the data source.
	 * @return the string representation of this data source
	 */
	public String toString()
	{
		return name;
	}
	
	/**
	 * Sets the data source name.
	 * @param s the new name
	 */
	public void setName(String s)
	{
		name = s;
	}
	
	/**
	 * Gets the data source name.
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}
	
	public Icon getIcon()
	{
		return Images.getIcon("wave_server");
	}
	
	public void establish()
	{}
	
	abstract public String toConfigString();
	
	/**
	 * Returns the appropriate type of seismic data source based on the passed
	 * parameter string.
	 * 
	 * TODO: make extensible by reading class names from file and having
	 * an abstract create() function.
	 * 
	 * @param source the data source parameters
	 * @return the appropriate data source
	 */
	public static SeismicDataSource getDataSource(String source)
	{
		String name = source.substring(0, source.indexOf(";"));
		source = source.substring(source.indexOf(";") + 1);
		String type = source.substring(0, source.indexOf(":"));
		String params = source.substring(source.indexOf(":") + 1);
		SeismicDataSource sds = null;

		if (type.equals("ws"))
		{
			sds = new WaveServerSource(params);
		}
		else if (type.equals("wws"))
		{
			sds = new WWSSource(params);
		}
		else if (type.equals("wwsd"))
		{
			if (params != null)
			{
				String[] ss = params.split("|");
				String driver = ss[0];
				String url = ss[1];
				String db = ss[2];
				sds = new DirectWWSSource(driver, url, db);
			}
		}
		else if (type.equals("cache"))
		{
			sds = Swarm.getCache();				
		}
		else if (type.equals("seed"))
		{
			sds = new FullSeedDataSource(params);
		}
		else if (type.equals("sac"))
		{
			sds = new SACDataSource(params);
		}
		else if (type.equals("dhi"))
		{
			sds = new DHIDataSource(params);
		}
		sds.setName(name);
		return sds;
	}
}
