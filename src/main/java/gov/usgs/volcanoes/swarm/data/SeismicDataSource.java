package gov.usgs.volcanoes.swarm.data;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Wave;

import java.util.List;

import javax.swing.event.EventListenerList;

/**
 * Base class for seismic data sources.
 * 
 * @author Dan Cervelli
 */
public abstract class SeismicDataSource {
  protected String name = "Unnamed Data Source";
  protected boolean storeInUserConfig = true;
  protected boolean useCache = true;
  protected int minimumRefreshInterval = 1;

  protected EventListenerList listeners = new EventListenerList();

  public Gulper createGulper(GulperList gl, String k, String ch, double t1, double t2, int size,
      int delay) {
    return new Gulper(gl, k, this, ch, t1, t2, size, delay);
  }

  public abstract List<String> getChannels();

  public abstract void parse(String params);

  /**
   * Either returns the wave successfully or null if the data source could not get the wave.
   * 
   * @param station channel name
   * @param t1 start time in j2k
   * @param t2 end time in j2k
   * @return wave if possible
   */
  public abstract Wave getWave(String station, double t1, double t2);

  public abstract HelicorderData getHelicorder(String station, double t1, double t2,
      GulperListener gl);

  public abstract String toConfigString();

  protected SeismicDataSource() {
    // explicit default constructor needed for reflection
  }

  public void addListener(SeismicDataSourceListener l) {
    listeners.add(SeismicDataSourceListener.class, l);
  }

  public void removeListener(SeismicDataSourceListener l) {
    listeners.remove(SeismicDataSourceListener.class, l);
  }

  /**
   * Fire channels updated.
   */
  public void fireChannelsUpdated() {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == SeismicDataSourceListener.class) {
        ((SeismicDataSourceListener) ls[i + 1]).channelsUpdated();
      }
    }
  }

  /**
   * Fire channels progress.
   * 
   * @param id progress id
   * @param p progress percent
   */
  public void fireChannelsProgress(String id, double p) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == SeismicDataSourceListener.class) {
        ((SeismicDataSourceListener) ls[i + 1]).channelsProgress(id, p);
      }
    }
  }

  /**
   * Fire helicorder progress.
   * 
   * @param id progress id
   * @param p progress percent
   */
  public void fireHelicorderProgress(String id, double p) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == SeismicDataSourceListener.class) {
        ((SeismicDataSourceListener) ls[i + 1]).helicorderProgress(id, p);
      }
    }
  }

  public void notifyDataNotNeeded(String station, double t1, double t2, GulperListener gl) {
    
  }

  public void setStoreInUserConfig(boolean b) {
    storeInUserConfig = b;
  }

  public boolean isStoreInUserConfig() {
    return storeInUserConfig;
  }

  public void setUseCache(boolean b) {
    useCache = b;
  }

  public boolean isUseCache() {
    return useCache;
  }

  /**
   * Is active data source.
   * 
   * @return whether or not this is an active data source
   */
  public boolean isActiveSource() {
    return false;
  }

  /**
   * Close the data source.
   */
  public abstract void close();

  /**
   * Get a string representation of this data source. The default implementation return the name of
   * the data source.
   * 
   * @return the string representation of this data source
   */
  public String toString() {
    return name;
  }

  /**
   * Sets the data source name.
   * 
   * @param s the new name
   */
  public void setName(String s) {
    name = s;
  }

  /**
   * Gets the data source name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  public int getMinimumRefreshInterval() {
    return minimumRefreshInterval;
  }

}
