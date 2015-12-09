package gov.usgs.volcanoes.swarm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.earthworm.WaveServer;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.SwarmConfig;

/**
 * An implementation of <code>SeismicDataSource</code> that connects to an
 * Earthworm Wave Server.
 * 
 *
 * @author Dan Cervelli
 */
public class WaveServerSource extends SeismicDataSource {
  private String params;
  private WaveServer waveServer;
  private int timeout = 2000;

  private String server;
  private int port;

  private int gulpSize = 30 * 60;
  private int gulpDelay = 1 * 1000;

  private TimeZone timeZone;


  private static Map<String, Boolean> scnlSources = new HashMap<String, Boolean>();

  // explicit default constructor required for reflection
  public WaveServerSource() {}

  public void parse(String params) {
    this.params = params;
    String[] ss = params.split(":");
    server = ss[0];
    port = Integer.parseInt(ss[1]);
    timeout = Integer.parseInt(ss[2]);
    gulpSize = Integer.parseInt(ss[3]);
    gulpDelay = Integer.parseInt(ss[4]);
    if (ss.length >= 6)
      timeZone = TimeZone.getTimeZone(ss[5]);
    else
      timeZone = TimeZone.getTimeZone("UTC");

    waveServer = new WaveServer(server, port);
    setTimeout(timeout);
  }

  public String toConfigString() {
    String typeString = DataSourceType.getShortName(this.getClass());
    return String.format("%s;" + typeString + ":%s:%d:%d:%d:%d:%s", name, server, port, timeout,
        gulpSize, gulpDelay, timeZone.getID());
  }

  public boolean isSCNL(String p) {
    Boolean b = scnlSources.get(p);
    if (b == null) {
      getMenu();
      b = scnlSources.get(p);
      if (b == null)
        return false;
    }

    return b.booleanValue();
  }

  public static void setIsSCNL(String p, boolean b) {
    scnlSources.put(p, b);
  }

  public synchronized void setTimeout(int to) {
    waveServer.setTimeout(to);
  }

  public synchronized void close() {
    if (waveServer != null)
      waveServer.close();
  }

  public synchronized Menu getMenu() {
    Menu menu = waveServer.getMenuSCNL();
    setIsSCNL(params, menu.isSCNL());
    return menu;
  }

  public String getFormattedSCNL(MenuItem mi) {
    if (isSCNL(params))
      return mi.getSCNL(" ");
    else
      return mi.getSCN(" ");
  }

  public List<String> getMenuList(List<MenuItem> items) {
    List<String> list = new ArrayList<String>(items.size());
    for (Iterator<MenuItem> it = items.iterator(); it.hasNext();) {
      MenuItem mi = it.next();
      list.add(getFormattedSCNL(mi));
    }
    return list;
  }

  public synchronized Wave getWave(String station, double t1, double t2) {
    CachedDataSource cache = CachedDataSource.getInstance();
    Wave sw = null;
    if (useCache)
      sw = cache.getWave(station, t1, t2);
    if (sw == null) {
      String seperator = station.indexOf('$') != -1 ? "\\$" : " ";
      String[] ss = station.split(seperator);
      String loc = null;
      if (isSCNL(params)) {
        loc = "--";
        if (ss.length == 4)
          loc = ss[3];
      }
      double offset = timeZone.getOffset(J2kSec.asEpoch(t1));
      double at1 = J2kSec.asEpoch(t1) + offset / 1000.0;
      double at2 = J2kSec.asEpoch(t2) + offset / 1000.0;
      sw = waveServer.getRawData(ss[0], ss[1], ss[2], loc, at1, at2);
      if (sw == null)
        return null;
      sw.convertToJ2K();
      sw.setStartTime(sw.getStartTime() - offset / 1000.0);
      sw.register();
      if (useCache) {
        cache.cacheWaveAsHelicorder(station, sw);
        cache.putWave(station, sw);
      }
    }
    return sw;
  }

  public synchronized List<String> getChannels() {
    Menu menu = getMenu();
    List<String> channels = getMenuList(menu.getSortedItems());
    SwarmConfig.getInstance().assignMetadataSource(channels, this);
    return channels;
  }

  public synchronized HelicorderData getHelicorder(String station, double t1, double t2,
      GulperListener gl) {
    double now = J2kSec.now();
    // if a time later than now has been asked for make sure to get the latest
    if ((t2 - now) >= -20)
      getWave(station, now - 2 * 60, now);

    CachedDataSource cache = CachedDataSource.getInstance();

    HelicorderData hd = cache.getHelicorder(station, t1, t2, (GulperListener) null);

    if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10))
      GulperList.INSTANCE.requestGulper("ws:" + station, gl, this.getCopy(), station, t1, t2,
          gulpSize, gulpDelay);
    return hd;
  }

  public synchronized void notifyDataNotNeeded(String station, double t1, double t2,
      GulperListener gl) {
    GulperList.INSTANCE.killGulper("ws:" + station, gl);
  }

  public synchronized boolean isActiveSource() {
    return true;
  }
}
