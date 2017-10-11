package gov.usgs.volcanoes.swarm.data.seedLink;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.ChannelUtil;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.DataSourceType;
import gov.usgs.volcanoes.swarm.data.Gulper;
import gov.usgs.volcanoes.swarm.data.GulperList;
import gov.usgs.volcanoes.swarm.data.GulperListener;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

/**
 * An implementation of <code>SeismicDataSource</code> that connects to an
 * SeedLink Server.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkSource extends SeismicDataSource {
  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SeedLinkSource.class);

  /** The gulp delay. */
  private static final int GULP_DELAY = 1000;

  /** The gulp size. */
  private static final int GULP_SIZE = 60;

  // /** Info string prefix text or null if none. */
  private static final String INFO_FILE_TEXT =
      System.getProperty(DataSourceType.getShortName(SeedLinkSource.class) + "infofile");

  /** The server host. */
  private String host;

  /** The information string File or null if none. */
  private File infoStringFile;

  /** The colon separated parameters. */
  private String params;

  /** The server port. */
  private int port;

  /** SeedLink client list. */
  private final List<SeedLinkClient> seedLinkClientList;

  public SeedLinkSource() {
    seedLinkClientList = new ArrayList<SeedLinkClient>();
  }

  /**
   * Create a SeedLink server source.
   * 
   * @param s the colon separated parameters.
   */
  public SeedLinkSource(String name, String s) {
    seedLinkClientList = new ArrayList<SeedLinkClient>();
    this.name = name;
    parse(s);
  }


  public void parse(String params) {
    this.params = params;
    String[] ss = params.split(":");
    int ssIndex = 0;
    host = ss[ssIndex++];
    port = Integer.parseInt(ss[ssIndex++]);
    if (INFO_FILE_TEXT != null)
      infoStringFile = new File(INFO_FILE_TEXT + host + port + ".xml");
  }

  /**
   * Close the data source.
   */
  public void close() {
    // close clients
    synchronized (seedLinkClientList) {
      if (seedLinkClientList.size() != 0) {
        LOGGER.debug("close the data source");
        for (SeedLinkClient client : seedLinkClientList) {
          client.close();
        }
        seedLinkClientList.clear();
      }
    }
  }


  public Gulper createGulper(GulperList gl, String k, String ch, double t1,
      double t2, int size, int delay) {
    return new SeedLinkGulper(gl, k, this, ch, t1, t2, size, delay);
  }

  /**
   * Get the channels. 
   * 
   * @return the list of channels.
   */
  public List<String> getChannels() {
    String infoString = readChannelCache();
    if (infoString == null) {
      final SeedLinkClient client = createClient();
      infoString = client.getInfoString();
      removeClient(client);
      writeChannelCache(infoString);
    }

    List<String> channels = Collections.emptyList();
    if (!(infoString == null || infoString.isEmpty())) {
      SeedLinkChannelInfo seedLinkChannelInfo = new SeedLinkChannelInfo(this);
      try {
        seedLinkChannelInfo.parse(infoString);
        channels = seedLinkChannelInfo.getChannels();
      } catch (Exception ex) {
        LOGGER.error("Cannot parse station list", ex);
      }
    }
    
    ChannelUtil.assignChannels(channels, this);
    return Collections.unmodifiableList(channels);
  }


  private String readChannelCache() {
    String infoString = null;
    if (infoStringFile != null && infoStringFile.canRead()) {
      try {
        return SeedLinkChannelInfo.readFile(infoStringFile);
      } catch (IOException e) {
        LOGGER.error("Cannot read seedlink channel cache. ({})", infoStringFile);
      }
    }

    return infoString;
  }


  private void writeChannelCache(String infoString) {
    try {
      SeedLinkChannelInfo.writeString(infoStringFile, infoString);
    } catch (IOException e) {
      LOGGER.error("Cannot write seedlink channel cache. ({})", infoStringFile);
    }
  }

  /**
   * Get a copy of this data source.
   * 
   * @return a copy of this data source.
   */
  public SeismicDataSource getCopy() {
    return new SeedLinkSource(name, params);
  }

  /**
   * Get the gulper key for the specified station.
   * 
   * @param station the station.
   * @return the gulper key.
   */
  private String getGulperKey(String station) {
    return DataSourceType.getShortName(SeedLinkSource.class) + ":" + station;
  }

  /**
   * Get the helicorder data.
   * 
   * @param scnl the scnl.
   * @param t1 the start time.
   * @param t2 the end time.
   * @param gl the gulper listener.
   * @return the helicorder data or null if none.
   */
  public HelicorderData getHelicorder(String scnl, double t1, double t2,
      GulperListener gl) {
    // check if data is in the cache
    HelicorderData hd = CachedDataSource.getInstance().getHelicorder(scnl,
        t1, t2, gl);
    // if no data or data start time is greater than requested
    if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10)
        || hd.getEndTime() < t2) {
      requestGulper(scnl, t1, t2, gl);
    }
    // if data end time is less than requested
    else if (hd.getEndTime() < t2) {
      requestGulper(scnl, hd.getEndTime(), t2, gl);
    }

    LOGGER.debug("getHelicorder(scnl={}, start={}, end={})\nDATA={}", scnl, J2kSec.toDateString(t1),
        J2kSec.toDateString(t2), (hd == null ? "NONE" : hd.toString()));
    return hd;
  }


  /**
   * Either returns the wave successfully or null if the data source could not
   * get the wave.
   * 
   * @param scnl the scnl.
   * @param t1 the start time.
   * @param t2 the end time.
   * @return the wave or null if none.
   */
  public Wave getWave(String scnl, double t1, double t2) {
    // check if data is in the cache
    Wave wave = CachedDataSource.getInstance().getWave(scnl, t1, t2);
    if (wave == null) {
      // remove all data in the future to avoid blocking
      final double now = J2kSec.now();
      if (t1 <= now) {
        final SeedLinkClient client = createClient();
        wave = client.getWave(scnl, t1, t2);
        removeClient(client);
      }
    }

    LOGGER.debug("getWave(scnl={}, start={}, end={})\nDATA={}", scnl, J2kSec.toDateString(t1),
        J2kSec.toDateString(t2), (wave == null ? "NONE" : wave.toString()));
    return wave;
  }

  /**
   * Is this data source active; that is, is new data being added in real-time
   * to this data source?
   * 
   * @return whether or not this is an active data source.
   */
  public boolean isActiveSource() {
    return true;
  }

  public synchronized void notifyDataNotNeeded(String station, double t1,
      double t2, GulperListener gl) {
    GulperList.INSTANCE.killGulper(getGulperKey(station), gl);
  }


  /**
   * Create a client.
   * 
   * @return the client.
   */
  protected SeedLinkClient createClient() {
    final SeedLinkClient client = new SeedLinkClient(host, port);
    synchronized (seedLinkClientList) {
      seedLinkClientList.add(client);
    }
    return client;
  }


  /**
   * Remove the client.
   * 
   * @param client the client.
   */
  protected void removeClient(SeedLinkClient client) {
    synchronized (seedLinkClientList) {
      seedLinkClientList.remove(client);
    }
  }


  /**
   * Request data from the gulper.
   * 
   * @param scnl the scnl.
   * @param t1 the start time.
   * @param t2 the end time.
   * @param gl the gulper listener.
   */
  protected void requestGulper(String scnl, double t1, double t2,
      GulperListener gl) {
    GulperList.INSTANCE.requestGulper(getGulperKey(scnl), gl, this,
        scnl, t1, t2, GULP_SIZE, GULP_DELAY);
  }

  /**
   * Get the configuration string.
   * 
   * @return the configuration string.
   */
  public String toConfigString() {
    return String.format("%s;%s:%s:%d", name, DataSourceType.getShortName(SeedLinkSource.class),
        host, port);
  }
}
