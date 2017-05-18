package gov.usgs.volcanoes.swarm.data;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.net.ReadListener;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;
import gov.usgs.volcanoes.winston.legacyServer.WWSClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An implementation of <code>SeismicDataSource</code> that communicates with a WinstonWaveServer.
 * This is essentially just a copy of WaveServerSource with different helicorder functions. It
 * should probably be made a descendant of WaveServerSource.
 * 
 * 
 * @author Dan Cervelli
 */
public class WWSSource extends SeismicDataSource implements RsamSource {
  private String params;
  private WWSClient winstonClient;
  private int timeout = 2000;
  private boolean compress = false;
  private int protocolVersion = 1;

  private String server;
  private int port;

  private boolean established;

  /**
   * Explicit default constructor required for reflection.
   */
  public WWSSource() {}

  /**
   * Constructor requiring WWS source.
   * @param wws Winston Wave Server Source
   */
  @Deprecated
  public WWSSource(WWSSource wws) {
    this.name = wws.name;
    parse(wws.params);
    protocolVersion = wws.protocolVersion;
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#parse(java.lang.String)
   */
  public void parse(String params) {
    this.params = params;
    String[] ss = params.split(":");
    server = ss[0];
    port = Integer.parseInt(ss[1]);
    timeout = Integer.parseInt(ss[2]);
    compress = ss[3].equals("1");

    winstonClient = new WWSClient(server, port);

    setTimeout(timeout);
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getCopy()
   */
  @Deprecated
  public SeismicDataSource getCopy() {
    return new WWSSource(this);
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#establish()
   */
  public void establish() {
    if (!established) {
      protocolVersion = winstonClient.getProtocolVersion();
      established = true;
    }
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#toConfigString()
   */
  public String toConfigString() {
    String typeString = DataSourceType.getShortName(this.getClass());
    return String.format("%s;" + typeString + ":%s:%d:%d:%s", name, server, port, timeout,
        compress ? "1" : "0");
  }

  /**
   * Set Winston Client time out.
   * @param to time out in milliseconds
   */
  public synchronized void setTimeout(int to) {
    winstonClient.setTimeout(to);
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#close()
   */
  public void close() {
    if (winstonClient != null) {
      winstonClient.close();
    }
  }

  /**
   * Get formatted SCNL.
   * @param mi menu item
   * @return SCNL string space delimited
   */
  public String getFormattedScnl(MenuItem mi) {
    return mi.getSCNSCNL(" ");
  }

  /**
   * Get menu list.
   * @param items list of menu items
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
   * Parse SCNL.
   * @param channel channel string
   * @return array of SCNL info
   */
  public String[] parseScnl(String channel) {
    String[] result = new String[4];
    String token = channel.indexOf("$") != -1 ? "$" : " ";
    StringTokenizer st = new StringTokenizer(channel, token);
    result[0] = st.nextToken();
    result[1] = st.nextToken();
    result[2] = st.nextToken();
    if (st.hasMoreTokens()) {
      result[3] = st.nextToken();
    } else {
      result[3] = "--";
    }
    return result;
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getWave(java.lang.String, double, double)
   */
  public synchronized Wave getWave(String station, double t1, double t2) {
    Wave wave = null;
    if (useCache) {
      CachedDataSource cache = CachedDataSource.getInstance();
      wave = cache.getWave(station, t1, t2);
    }

    if (wave == null) {
      String[] scnl = parseScnl(station);
      if (protocolVersion == 1) {
        wave = winstonClient.getRawData(scnl[0], scnl[1], scnl[2], scnl[3], J2kSec.asEpoch(t1),
            J2kSec.asEpoch(t2));
        if (wave != null) {
          wave.convertToJ2K();
        }
      } else {
        wave = winstonClient.getWave(scnl[0], scnl[1], scnl[2], scnl[3], t1, t2, compress);
      }

      if (wave == null) {
        return null;
      }

      wave.register();
      if (useCache) {
        CachedDataSource cache = CachedDataSource.getInstance();
        cache.putWave(station, wave);
      }
    } else {
      // System.out.println("cached");
    }
    return wave;
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.RsamSource#getRsam(java.lang.String, double, double, int)
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
      String[] scnl = parseScnl(station);
      rsamData =
          winstonClient.getRSAMData(scnl[0], scnl[1], scnl[2], scnl[3], t1, t2, period, compress);
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
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getHelicorder(java.lang.String, double,
   * double, gov.usgs.volcanoes.swarm.data.GulperListener)
   */
  public synchronized HelicorderData getHelicorder(final String station, double t1, double t2,
      GulperListener gl) {
    CachedDataSource cache = CachedDataSource.getInstance();

    HelicorderData hd = cache.getHelicorder(station, t1, t2, this);
    if (hd == null) {
      String[] scnl = parseScnl(station);
      fireHelicorderProgress(station, -1);
      winstonClient.setReadListener(new ReadListener() {
        public void readProgress(double p) {
          fireHelicorderProgress(station, p);
        }
      });
      hd = winstonClient.getHelicorder(scnl[0], scnl[1], scnl[2], scnl[3], t1, t2, compress);
      winstonClient.setReadListener(null);
      fireHelicorderProgress(station, 1.0);

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
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#getChannels()
   */
  public synchronized List<String> getChannels() {
    SwarmConfig swarmConfig = SwarmConfig.getInstance();
    if (protocolVersion == 1) {
      Menu menu = winstonClient.getMenuSCNL();
      List<String> channels = getMenuList(menu.getSortedItems());
      swarmConfig.assignMetadataSource(channels, this);
      return channels;
    } else if (protocolVersion == 2) {
      List<Channel> channels = winstonClient.getChannels();
      List<String> result = new ArrayList<String>(channels.size());
      for (Channel ch : channels) {
        String code = ch.getCode().replace('$', ' ');
        Metadata md = swarmConfig.getMetadata(code, true);
        Instrument ins = ch.getInstrument();
        md.updateLongitude(ins.getLongitude());
        md.updateLatitude(ins.getLatitude());
        md.source = this;
        result.add(code);
      }
      return result;
    } else if (protocolVersion == 3) {
      List<Channel> channels = winstonClient.getChannels(true);
      List<String> result = new ArrayList<String>(channels.size());
      for (Channel ch : channels) {
        String code = ch.getCode().replace('$', ' ');
        Metadata md = swarmConfig.getMetadata(code, true);
        Instrument ins = ch.getInstrument();
        md.updateLongitude(ins.getLongitude());
        md.updateLatitude(ins.getLatitude());
        md.updateMinTime(ch.getMinTime());
        md.updateMaxTime(ch.getMaxTime());
        if (md.getGroups() != null) { // in case of data source refresh
          md.getGroups().clear();
        }
        List<String> groups = ch.getGroups();
        if (groups != null) {
          for (String g : groups) {
            md.addGroup(g);
          }
        }
        md.updateLinearCoefficients(ch.getLinearA(), ch.getLinearB());
        md.updateAlias(ch.getAlias());
        md.updateUnits(ch.getUnit());
        md.updateTimeZone(ch.getInstrument().getTimeZone());
        md.source = this;
        result.add(code);
      }
      return result;
    } else {
      return null;
    }
  }

  /**
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#isActiveSource()
   */
  public synchronized boolean isActiveSource() {
    return true;
  }

}
