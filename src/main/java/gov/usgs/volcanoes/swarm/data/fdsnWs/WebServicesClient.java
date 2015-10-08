package gov.usgs.volcanoes.swarm.data.fdsnWs;

import edu.sc.seis.seisFile.mseed.DataRecord;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.swarm.ChannelInfo;
import gov.usgs.volcanoes.swarm.StationInfo;
import gov.usgs.volcanoes.swarm.data.DataSelectReader;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import java.util.Date;
import java.util.List;

public class WebServicesClient extends AbstractDataRecordClient {
  /**
   * Test flag to use XML instead of text for station details. XML is more
   * robust at the cost of a lot of speed. This will crawl if there are many
   * networks. To use add "-DSWARM_WS_USE_XML=TRUE" to the Java command line.
   */
  private static boolean useXmlClientFlag = Boolean
      .valueOf(WebServiceUtils.getProperty(WebServiceUtils.SWARM_WS_PROP_KEY_PREFIX + "USE_XML"));

  /**
   * Get the default web services data select URL text.
   * 
   * @return the default web services data select URL text.
   */
  public static String getDefaultWsDataSelectUrl() {
    return DataSelectReader.DEFAULT_WS_URL;
  }

  /**
   * Get the default web services station URL text.
   * 
   * @return the default web services station URL text.
   */
  public static String getDefaultWsStationUrl() {
    return AbstractWebServiceStationClient.DEFAULT_WS_URL;
  }

  /** The station client. */
  private final AbstractWebServiceStationClient stationClient;

  /** The web services data select URL text. */
  private final String wsDataSelectUrl;

  private String last_sta = "";
  private int num_stations = 0;
  private int station_cnt = 0;
  private final String progress_id = "channels";

  /**
   * Creates the web services client.
   * 
   * @param net the network filter or empty if none.
   * @param sta the station filter or empty if none.
   * @param loc the location filter or empty if none.
   * @param chan the channel filter or empty if none.
   */
  public WebServicesClient(final SeismicDataSource source, String net, String sta, String loc,
      String chan) {
    this(source, net, sta, loc, chan, getDefaultWsDataSelectUrl(), getDefaultWsStationUrl());
  }

  /**
   * Creates the web services client.
   * 
   * @param net the network filter or empty if none.
   * @param sta the station filter or empty if none.
   * @param loc the location filter or empty if none.
   * @param chan the channel filter or empty if none.
   * @param wsDataSelectUrl the web services data select URL text.
   * @param wsStationUrl the web services station URL text.
   * @see #getDefaultWsDataSelectUrl()
   * @see #getDefaultWsStationUrl()
   */
  public WebServicesClient(final SeismicDataSource source, String net, String sta, String loc,
      String chan, String wsDataSelectUrl, String wsStationUrl) {
    super(source);
    Date date = null; // use current date
    final List<String> channelList = WebServiceStationXmlClient.createChannelList();
    if (useXmlClientFlag) {
      stationClient = new WebServiceStationXmlClient(wsStationUrl, net, sta, loc, chan, date) {
        public void processChannel(ChannelInfo ch) {
          WebServiceUtils.addChannel(channelList, ch, source);
          if (last_sta.compareTo(ch.getStation()) != 0) {
            last_sta = ch.getStation();
            if (num_stations > 0)
              getSource().fireChannelsProgress(progress_id,
                  (double) station_cnt / (double) num_stations);
            station_cnt++;
          }

        }
      };
    } else {
      stationClient = new WebServiceStationTextClient(wsStationUrl, net, sta, loc, chan, date) {
        public void processChannel(ChannelInfo ch) {
          WebServiceUtils.addChannel(channelList, ch, source);
          if (last_sta.compareTo(ch.getStation()) != 0) {
            last_sta = ch.getStation();
            if (num_stations > 0)
              getSource().fireChannelsProgress(progress_id,
                  (double) station_cnt / (double) num_stations);
            station_cnt++;
          }

        }
      };
    }
    stationClient.setStationList(AbstractWebServiceStationClient.createStationList());
    stationClient.setChannelList(channelList);
    this.wsDataSelectUrl = wsDataSelectUrl;
  }

  /**
   * Get the channel information.
   * 
   * @return the list of channel information.
   */
  public List<String> getChannels() {
    final List<String> channelList = stationClient.getChannelList();
    if (channelList.size() != 0) {
      WebServiceUtils.info("channel list is not empty");
    } else {
      String error = null;
      long start = System.currentTimeMillis();
      if (stationClient.isAllNetworks()) {
        stationClient.setCurrentStation(null);
        // error = stationClient.fetchChannels();
        error = stationClient.fetchStations();
        if (error == null) {
          getSource().fireChannelsProgress(progress_id, 0.);
          num_stations = stationClient.getStationList().size();
          error = stationClient.fetchChannels();
        }

      } else {
        // final String id = "channels";
        getSource().fireChannelsProgress(progress_id, 0.);
        // getSource().fireChannelsProgress(id, 0.);
        error = stationClient.fetchStations();
        num_stations = stationClient.getStationList().size();
        if (error == null) {
          int cnt = 0;
          /*
           * final List<StationInfo> stationList = stationClient.getStationList();
           * final int ns = stationList.size();
           * for (StationInfo station : stationList) {
           * getSource().fireChannelsProgress(id, (double) cnt / (double) ns);
           * cnt++;
           * stationClient.setCurrentStation(station);
           * error = stationClient.fetchChannels();
           * if (error != null) {
           * break;
           * }
           * }
           */
          stationClient.setCurrentStation(null);
          error = stationClient.fetchChannels();

        }
        // getSource().fireChannelsProgress(id, 1.);
        getSource().fireChannelsProgress(progress_id, 1.);
      }
      long end = System.currentTimeMillis();
      if (WebServiceUtils.isDebug()) {
        WebServiceUtils.debug("getChannels(" + (useXmlClientFlag ? "XML" : "Text") + "): "
            + ((end - start) / 1000.) + " seconds");
      }
      if (error != null) {
        WebServiceUtils.warning("could not get channels: " + error);
      }
      assignChannels(channelList);
    }
    return channelList;
  }

  /**
   * Get the raw data.
   * 
   * @param channelInfo the channel information.
   * @param t1 the start time.
   * @param t2 the end time.
   * @return the raw data.
   */
  public Wave getRawData(final ChannelInfo channelInfo, final double t1, final double t2) {
    final Date begin = getDate(t1);
    final Date end = getDate(t2);
    final List<Wave> waves = createWaves();
    final DataSelectReader reader = new DataSelectReader(wsDataSelectUrl) {
      /**
       * Process a data record.
       * 
       * @param dr the data record.
       * @return true if data record should be added to the list, false
       *         otherwise.
       */
      public boolean processRecord(DataRecord dr) {
        try {
          addWaves(waves, dr);
        } catch (Exception ex) {
          WebServiceUtils.warning(
              "could not get web service raw data (" + channelInfo + "): " + ex.getMessage());
        }
        return true;
      }
    };
    try {
      final String query = reader.createQuery(channelInfo.getNetwork(), channelInfo.getStation(),
          channelInfo.getLocation(), channelInfo.getChannel(), begin, end);
      reader.read(query, (List<DataRecord>) null);
    } catch (Exception ex) {
      WebServiceUtils
          .warning("could not get web service raw data (" + channelInfo + "): " + ex.getMessage());
    }
    Wave wave = join(waves);
    if (wave != null && WebServiceUtils.isDebug()) {
      WebServiceUtils.debug("web service raw data (" + getDateText(wave.getStartTime()) + ", "
          + getDateText(wave.getEndTime()) + ")");
    }
    return wave;
  }
}
