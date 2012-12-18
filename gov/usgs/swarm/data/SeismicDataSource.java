package gov.usgs.swarm.data;

import gov.usgs.swarm.Images;
import gov.usgs.swarm.Swarm;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.util.List;

import javax.swing.Icon;
import javax.swing.event.EventListenerList;

/**
 * Base class for seismic data sources.
 * 
 * @author Dan Cervelli
 */
abstract public class SeismicDataSource
{
	protected String name = "Unnamed Data Source";
	protected boolean storeInUserConfig = true;
	protected boolean useCache = true;
	protected int minimumRefreshInterval = 1;
	
	protected EventListenerList listeners = new EventListenerList();

	abstract public List<String> getChannels();
	
	/**
	 * Either returns the wave successfully or null if the data source could
	 * not get the wave.
	 * 
	 * @param station
	 * @param t1
	 * @param t2
	 * @return wave if possible
	 */
	abstract public Wave getWave(String station, double t1, double t2);
	abstract public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl);
	
	public void addListener(SeismicDataSourceListener l)
	{
		listeners.add(SeismicDataSourceListener.class, l);
	}
	
	public void removeListener(SeismicDataSourceListener l)
	{
		listeners.remove(SeismicDataSourceListener.class, l);
	}
	
	public void fireChannelsUpdated()
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
		    if (ls[i] == SeismicDataSourceListener.class)
		        ((SeismicDataSourceListener)ls[i + 1]).channelsUpdated();
	}
	
	public void fireChannelsProgress(String id, double p)
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
		    if (ls[i] == SeismicDataSourceListener.class)
		        ((SeismicDataSourceListener)ls[i + 1]).channelsProgress(id, p);
	}
	
	public void fireHelicorderProgress(String id, double p)
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
		    if (ls[i] == SeismicDataSourceListener.class)
		        ((SeismicDataSourceListener)ls[i + 1]).helicorderProgress(id, p);
	}
	
	public void notifyDataNotNeeded(String station, double t1, double t2, GulperListener gl)
	{}
	
	public void setStoreInUserConfig(boolean b)
	{
		storeInUserConfig = b;
	}
	
	public boolean isStoreInUserConfig()
	{
		return storeInUserConfig;
	}
	
	public void setUseCache(boolean b)
	{
		useCache = b;
	}
	
	public boolean isUseCache()
	{
		return useCache;
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
	
	public void remove() {}

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
		else if (type.equals(WebServicesSource.WEB_SERVICES_CLIENT_CODE))
		{
			sds = new WebServicesSource(params);
		}
		else if (type.equals("file"))
		{
			sds = Swarm.getApplication().fileSource;
		}
		sds.setName(name);
		return sds;
	}
	
	public int getMinimumRefreshInterval() {
		return minimumRefreshInterval;
	}
}
