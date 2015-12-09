package gov.usgs.volcanoes.swarm.data.fdsnWs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Date;
import java.util.List;

import gov.usgs.volcanoes.swarm.StationInfo;

public class WebServiceStationTextClient extends AbstractWebServiceStationClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceStationTextClient.class);
  
  /**
   * Create the web service station client.
   * 
   * @param baseUrlText
   *          the base URL text.
   */
  public static WebServiceStationTextClient createClient(String[] args) {
    String baseUrlText = getArg(args, 0, DEFAULT_WS_URL);
    String net = getArg(args, 1);
    String sta = getArg(args, 2);
    String loc = getArg(args, 3);
    String chan = getArg(args, 4);
    Date date = WebServiceUtils.parseDate(getArg(args, 5));
    return new WebServiceStationTextClient(baseUrlText, net, sta, loc, chan, date);
  }

  public static void main(String[] args) {
    String error = null;
    WebServiceStationTextClient client = createClient(args);
    try {
      client.setStationList(createStationList());
      error = client.fetchStations();
      if (error == null) {
        List<StationInfo> stationList = client.getStationList();
        System.out.println("station count: " + stationList.size());
        for (StationInfo station : stationList) {
          System.out.println("station: " + station);
          client.setCurrentStation(station);
          error = client.fetchChannels();
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    if (error != null) {
      System.out.println(error);
    } else {
      System.out.println("done");
    }
  }

  /**
   * Create the web service station client.
   * 
   * @param baseUrlText
   *          the base URL text.
   * @param net
   *          the network or null if none.
   * @param sta
   *          the station or null if none.
   * @param loc
   *          the location or null if none.
   * @param chan
   *          the channel or null if none.
   * @param date
   *          the date or null if none.
   */
  public WebServiceStationTextClient(String baseUrlText, String net, String sta, String loc,
      String chan, Date date) {
    super(baseUrlText, net, sta, loc, chan, date);
  }

  /**
   * Fetch the stations.
   * 
   * @param url
   *          the URL.
   * 
   * @throws Exception
   *           if an error occurs.
   */
  protected void fetch(URL url) throws Exception {
    for (String line; (line = getReader().readLine()) != null;) {
      processLine(line);
    }
  }

  /**
   * Get the base URL text.
   * 
   * @return the base URL text.
   */
  protected String getBaseUrlText() {
    String urlText = super.getBaseUrlText();
    urlText = append(urlText, "format", "text");
    return urlText;
  }

  /**
   * Get the column text.
   * 
   * @param columns
   *          the columns.
   * @param index
   *          the column index or -1 if none.
   * @return the column text or null if none.
   */
  protected String getColumnText(String[] columns, int index) {
    String s = null;
    if (index >= 0 && index < columns.length) {
      s = columns[index];
    }
    return s;
  }

  /**
   * Get the line split text.
   * 
   * @return the line split text.
   */
  protected String getLineSplitText() {
    return "\\s*\\|\\s*";
  }

  /**
   * Process the line.
   * 
   * @param line
   *          the line of text containing the channel information.
   */
  protected void processLine(String line) {
    // skip comment line
    if (line.startsWith("#")) {
      return;
    }

    // skip line if it starts with the separator
    if (line.matches("^" + getLineSplitText() + ".*")) {
      LOGGER.info("skipping line ({})", line);
      return;
    }

    final String[] columns = split(line);
    int minNumColumns;
    switch (getLevel()) {
      case STATION:
        minNumColumns = 6;
        if (columns.length < minNumColumns) {
          error.append("invalid line (" + line + ")\n");
        } else {
          String network = getColumnText(columns, 0);
          String station = getColumnText(columns, 1);
          double latitude = StationInfo.parseDouble(getColumnText(columns, 2));
          double longitude = StationInfo.parseDouble(getColumnText(columns, 3));
          String siteName = getColumnText(columns, 5);
          processStation(createStationInfo(station, network, latitude, longitude, siteName));
        }
        break;
      case CHANNEL:
        minNumColumns = 6;
        if (columns.length < minNumColumns) {
          error.append("invalid line (" + line + ")\n");
        } else {
          String network = getColumnText(columns, 0);
          String station = getColumnText(columns, 1);
          String location = getColumnText(columns, 2);
          String channel = getColumnText(columns, 3);
          String siteName = null; // site name is not available
          double latitude = StationInfo.parseDouble(getColumnText(columns, 4));
          double longitude = StationInfo.parseDouble(getColumnText(columns, 5));
          processChannel(createChannelInfo(station, channel, network, location, latitude, longitude,
              siteName, groupsType));
        }
        break;
      default:
        break;
    }
  }

  /**
   * Split the line.
   * 
   * @param line
   *          the line of text containing the channel information.
   * @return the channel information.
   */
  protected String[] split(String line) {
    return line.split(getLineSplitText());
  }
}
