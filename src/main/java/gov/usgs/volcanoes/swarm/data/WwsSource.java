package gov.usgs.volcanoes.swarm.data;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.RSAMData;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.legacy.ew.MenuItem;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.wwsclient.WWSClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An implementation of <code>SeismicDataSource</code> that communicates with a
 * WinstonWaveServer. This is essentially just a copy of WaveServerSource with
 * different helicorder functions. It should probably be made a descendant of
 * WaveServerSource.
 * 
 * 
 * @author Dan Cervelli
 */
public class WwsSource extends SeismicDataSource implements RsamSource {
  private WWSClient winstonClient;
  private int timeout = 2000;
  private boolean compress = false;

  private String server;
  private int port;

  /**
   * Explicit default constructor required for reflection.
   */
  public WwsSource() {}

  /**
   * Parse parameters.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#parse(java.lang.String)
   */
  public synchronized void parse(String params) {
    String[] ss = params.split(":");
    server = ss[0];
    port = Integer.parseInt(ss[1]);
    timeout = Integer.parseInt(ss[2]);
    compress = ss[3].equals("1");

    winstonClient = new WWSClient(server, port, timeout);
  }

  /**
   * Print config text.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#toConfigString()
   */
  public String toConfigString() {
    String typeString = DataSourceType.getShortName(this.getClass());
    return String.format("%s;" + typeString + ":%s:%d:%d:%s", name, server, port, timeout,
        compress ? "1" : "0");
  }

  /**
   * Close Winston client connection.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#close()
   */
  public synchronized void close() {
    if (winstonClient != null) {
      winstonClient.close();
    }
  }

  /**
   * Get formatted SCNL.
   * 
   * @param mi
   *            menu item
   * @return SCNL string space delimited
   */
  public String getFormattedScnl(MenuItem mi) {
    return mi.getSCNSCNL(" ");
  }

  /**
   * Get menu list.
   * 
   * @param items
   *            list of menu items
   * @return list of SCNL string
   */
  public List<String> getMenuList(List<MenuItem> items) {
    List<String> list = new ArrayList<String>(items.size());
    for (Iterator<MenuItem> it = items.iterator(); it.hasNext();) {
      MenuItem mi = it.next();
      list.add(getFormattedScnl(mi));
    }
    return list;
  }

  /**
   * Get wave data.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getWave(java.lang.String,
   *      double, double)
   */
  public synchronized Wave getWave(String station, double t1, double t2) {
    Wave wave = null;
    if (useCache) {
      CachedDataSource cache = CachedDataSource.getInstance();
      wave = cache.getWave(station, t1, t2);
    }
    if (wave == null) {
      String delimiter = station.indexOf("$") == -1 ? " " : "$";
      Scnl scnl;
      try {
        scnl = Scnl.parse(station, delimiter);
        TimeSpan timeSpan = TimeSpan.fromJ2kSec(t1, t2);
        wave = winstonClient.getWave(scnl, timeSpan, compress);
      } catch (UtilException e) {
        System.err.println("WWSSource.getWave: Cannot parse station " + station);
      }

      if (wave == null) {
        return null;
      }

      wave.register();
      if (useCache) {
        CachedDataSource cache = CachedDataSource.getInstance();
        cache.putWave(station, wave);
      }
    } 
    return wave;
  }

  /**
   * Get RSAM data.
   * @see gov.usgs.volcanoes.swarm.data.RsamSource#getRsam(java.lang.String,
   *      double, double, int)
   */
  public synchronized RSAMData getRsam(String station, double t1, double t2, int period) {
    RSAMData rsamData = null;
    if (useCache) {
      CachedDataSource cache = CachedDataSource.getInstance();
      rsamData = cache.getRsam(station, t1, t2, period);
      if (rsamData != null) {
        System.out.println("found in cache");
      }
    }

    if (rsamData == null) {
      String delimiter = station.indexOf("$") == -1 ? " " : "$";
      try {
        Scnl scnl = Scnl.parse(station, delimiter);
        TimeSpan timeSpan = TimeSpan.fromJ2kSec(t1, t2);
        rsamData =
            winstonClient.getRSAMData(scnl, timeSpan, period, compress);
      } catch (UtilException e) {
        System.err.println("WWSSource.getRsam: Cannot parse station " + station);
      }
      if (rsamData == null) {
        return null;
      }

      if (useCache) {
        CachedDataSource cache = CachedDataSource.getInstance();
        cache.putRsam(station, rsamData);
      }
    } else {
      // System.out.println("cached");
    }
    return rsamData;
  }

  /**
   * Get helicorder data.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getHelicorder(java.lang.String,
   *      double, double, gov.usgs.volcanoes.swarm.data.GulperListener)
   */
  public synchronized HelicorderData getHelicorder(final String station, double t1, double t2,
      GulperListener gl) {
    CachedDataSource cache = CachedDataSource.getInstance();

    HelicorderData hd = cache.getHelicorder(station, t1, t2, this);
    if (hd == null) {
      String delimiter = station.indexOf("$") == -1 ? " " : "$";
     
      Scnl scnl;
      try {
        scnl = Scnl.parse(station, delimiter);
        fireHelicorderProgress(station, -1);
        // winstonClient.setReadListener(new ReadListener() {
        // public void readProgress(double p) {
        // fireHelicorderProgress(station, p);
        // }
        // });
        TimeSpan timeSpan = TimeSpan.fromJ2kSec(t1, t2);
        hd = winstonClient.getHelicorder(scnl, timeSpan, compress);
        // winstonClient.setReadListener(null);
        fireHelicorderProgress(station, 1.0);
      } catch (UtilException e) {
        System.err.println("WWSSource.getHelicorder: Cannot parse SCNL '" + station + "'.");
      }

      if (hd != null && hd.rows() != 0) {
        HelicorderData noLatest = hd.subset(hd.getStartTime(), J2kSec.now() - 30);
        if (noLatest != null && noLatest.rows() > 0) {
          cache.putHelicorder(station, noLatest);
        }
      } else {
        hd = null;
      }
    }
    return hd;
  }

  /**
   * Get list of channels.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getChannels()
   */
  public synchronized List<String> getChannels() {
    List<Channel> channels = winstonClient.getChannels(true);
    List<String> channelNames = new ArrayList<String>(channels.size());
    SwarmConfig swarmConfig = SwarmConfig.getInstance();

    for (Channel chan : channels) {
      String chanName = chan.scnl.toString(" ");
      channelNames.add(chanName);
      Metadata md = swarmConfig.getMetadata(chanName, true);
      md.update(chan);
      md.source = this;
    }

    return channelNames;
  }

  /**
   * Check if active source.
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#isActiveSource()
   */
  public synchronized boolean isActiveSource() {
    return true;
  }

}
