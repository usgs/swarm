package gov.usgs.volcanoes.swarm.data.fdsnws;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.ChannelGroupInfo;
import gov.usgs.volcanoes.swarm.ChannelInfo;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.DataSourceType;
import gov.usgs.volcanoes.swarm.data.Gulper;
import gov.usgs.volcanoes.swarm.data.GulperList;
import gov.usgs.volcanoes.swarm.data.GulperListener;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServicesSource extends SeismicDataSource {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebServicesSource.class);
  private static final String IRIS_DATASELECT_URL = "http://service.iris.edu/fdsnws/dataselect/1/query";
  private static final String IRIS_STATION_URL = "http://service.iris.edu/fdsnws/station/1/query";

  public static final String DEFAULT_DATASELECT_URL = "http://localhost/fdsnws/dataselect/1/query";
  public static final String DEFAULT_STATION_URL = "http://localhost/fdsnws/station/1/query";

  /** Web services source tab title. */
  public static final String TAB_TITLE = "FDSN WS";
  /** Web services source description. */
  public static final String DESCRIPTION = "an FDSN Web Services server";
  /** Web-Services client code. */
  public static final String typeString;
  /** Parameter split text. */
  public static final String PARAM_SPLIT_TEXT = "\\|";
  /** Parameter format text. */
  public static final String PARAM_FMT_TEXT = "%s|%s|%s|%s|%d|%d|%s|%s";
  /** instance counter. */
  private static int counter = 0;
  /** instance count. */
  private final int count = ++counter;
  /** Web Services Client. */
  private WebServicesClient client;
  /** The colon separated parameters. */
  private String params;
  /** The gulp delay. */
  private int gulpDelay;
  /** The gulp size. */
  private int gulpSize;
  // /** The channel. */
  // private String chan;
  // /** The location. */
  // private String loc;
  // /** The network. */
  // private String net;
  // /** The station. */
  // private String sta;
  // /** Web Services dataselect URL. */
  // private String wsDataSelectUrl;
  // /** Web Services station URL. */
  // private String wsStationUrl;

  private String configString;

  static {
    typeString = DataSourceType.getShortName(WebServicesSource.class);
  }

  /**
   * Default constructor.
   */
  public WebServicesSource() {}

  /**
   * Get web services source for channel.
   * 
   * @param channel channel name
   */
  public WebServicesSource(String channel) {
    setChannel(channel);
  }


  /**
   * Build params for channel using IRIS web service and parse.
   * 
   * @param channel channel name
   */
  private void setChannel(String channel) {
    String[] comps = channel.split("\\$");
    LOGGER.debug("SPLIT {}", channel);
    StringBuilder sb = new StringBuilder();
    sb.append(comps[2]).append("|");
    sb.append(comps[0]).append("|");

    if (comps.length > 3) {
      sb.append(comps[3]).append("|");
    } else {
      sb.append("--|");
    }
    sb.append(comps[1]).append("|");
    sb.append(3600).append("|");
    sb.append(1000).append("|");
    sb.append(IRIS_DATASELECT_URL).append("|");
    sb.append(IRIS_STATION_URL);
    parse(sb.toString());
  }

  /**
   * Parse parameters.
   * 
   * @see gov.usgs.volcanoes.swarm.data.SeismicDataSource#parse(java.lang.String)
   */
  public void parse(String params) {
    this.params = params;
    String[] ss = params.split(PARAM_SPLIT_TEXT);
    int ssIndex = 0;
    String net = ss[ssIndex++];
    String sta = ss[ssIndex++];
    String loc = ss[ssIndex++];
    String chan = ss[ssIndex++];
    gulpSize = Integer.parseInt(ss[ssIndex++]);
    gulpDelay = Integer.parseInt(ss[ssIndex++]);
    String wsDataSelectUrl = ss[ssIndex++];
    String wsStationUrl = ss[ssIndex++];
    configString = String.format("%s;%s:" + PARAM_FMT_TEXT, name, typeString, net, sta, loc, chan,
        gulpSize, gulpDelay, wsDataSelectUrl, wsStationUrl);
    client = new WebServicesClient(this, net, sta, loc, chan, wsDataSelectUrl, wsStationUrl);
    /*
     * try { client.getStationClient().fetch(); } catch (Exception e) { // TODO Auto-generated catch
     * block e.printStackTrace(); }
     */
    LOGGER.debug("web service started {}", count);
  }

  /**
   * Create a Web Services server source with the same parameters as the specified Web Services
   * server source.
   * 
   * @param sls the Web Services server source.
   */
  public WebServicesSource(WebServicesSource sls) {
    name = sls.name;
    parse(sls.params);
  }

  /**
   * Close the data source.
   */
  public synchronized void close() {
    LOGGER.debug("web service closed {}", count);
  }

  /**
   * Get the channels.
   * 
   * @return the list of channels.
   */
  public synchronized List<String> getChannels() {
    List<String> channels = client.getChannels();
    return Collections.unmodifiableList(channels);
  }

  /**
   * Get a copy of this data source.
   * 
   * @return a copy of this data source.
   */
  public SeismicDataSource getCopy() {
    return new WebServicesSource(this);
  }

  /**
   * Get the gulper key for the specified station.
   * 
   * @param station the station.
   * @return the gulper key.
   */
  private String getGulperKey(String station) {
    return typeString + ":" + station;
  }

  /**
   * Get the helicorder data.
   * 
   * @param station the station.
   * @param t1 the start time.
   * @param t2 the end time.
   * @param gl the gulper listener.
   * @return the helicorder data or null if none.
   */
  public synchronized HelicorderData getHelicorder(String station, double t1, double t2,
      GulperListener gl) {

    double now = J2kSec.now();
    
    
    CachedDataSource cache = CachedDataSource.getInstance();

    HelicorderData hd = cache.getHelicorder(station, t1, t2, (GulperListener) null);

    if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10)) {
      GulperList.INSTANCE.requestGulper(getGulperKey(station), gl, this, station, t1, t2, gulpSize,
          gulpDelay);
    }

    if (hd != null && hd.getEndTime() < now) { 
      Gulper g = GulperList.INSTANCE.getGulper(getGulperKey(station));
      if(g==null || g.isKilled()) {
        getWave(station, hd.getEndTime(), now); 
      } // otherwise let gulper finish first
    }
    return hd;
  }

  /**
   * Either returns the wave successfully or null if the data source could not get the wave.
   * 
   * @param station the station.
   * @param t1 the start time.
   * @param t2 the end time.
   * @return the wave or null if none.
   */
  public synchronized Wave getWave(String station, double t1, double t2) {
    CachedDataSource cache = CachedDataSource.getInstance();

    Wave sw = null;
    if (useCache) {
      sw = cache.getWave(station, t1, t2);
    }
    if (sw == null) {
      ChannelInfo channelInfo = new ChannelGroupInfo(station);
      sw = client.getRawData(channelInfo, t1, t2);
      if (sw == null) {
        return null;
      }
      if (useCache) {
        cache.cacheWaveAsHelicorder(station, sw);
        cache.putWave(station, sw);
      }
    }
    return sw;
  }

  /**
   * Is this data source active; that is, is new data being added in real-time to this data source?
   * 
   * @return whether or not this is an active data source.
   */
  public synchronized boolean isActiveSource() {
    return true;
  }

  public synchronized void notifyDataNotNeeded(String station, double t1, double t2,
      GulperListener gl) {
    GulperList.INSTANCE.killGulper(getGulperKey(station), gl);
  }

  /**
   * Get the configuration string.
   * 
   * @return the configuration string.
   */
  public String toConfigString() {
    return configString;
  }

}
