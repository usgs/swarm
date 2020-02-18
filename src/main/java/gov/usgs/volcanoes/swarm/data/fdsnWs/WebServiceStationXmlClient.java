package gov.usgs.volcanoes.swarm.data.fdsnWs;

import gov.usgs.volcanoes.swarm.StationInfo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.seisFile.fdsnws.stationxml.FDSNStationXML;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.NetworkIterator;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;
import edu.sc.seis.seisFile.fdsnws.stationxml.StationIterator;
import edu.sc.seis.seisFile.fdsnws.stationxml.StationXMLException;
import edu.sc.seis.seisFile.fdsnws.stationxml.StationXMLTagNames;

public class WebServiceStationXmlClient extends AbstractWebServiceStationClient {

  private final static Logger LOGGER = LoggerFactory.getLogger(WebServiceStationXmlClient.class);

  /**
   * Create the web service station client.
   * 
   * @param baseUrlText the base URL text.
   */
  public static WebServiceStationXmlClient createClient(String[] args) {
    String baseUrlText = getArg(args, 0, DEFAULT_WS_URL);
    String net = getArg(args, 1);
    String sta = getArg(args, 2);
    String loc = getArg(args, 3);
    String chan = getArg(args, 4);
    Date date = WebServiceUtils.parseDate(getArg(args, 5));
    return new WebServiceStationXmlClient(baseUrlText, net, sta, loc, chan, date);
  }

  public static void main(String[] args) {
    String error = null;
    WebServiceStationXmlClient client = createClient(args);
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
   * @param baseUrlText the base URL text.
   * @param net the network or null if none.
   * @param sta the station or null if none.
   * @param loc the location or null if none.
   * @param chan the channel or null if none.
   * @param date the date or null if none.
   */
  public WebServiceStationXmlClient(String baseUrlText, String net, String sta, String loc,
      String chan, Date date) {
    super(baseUrlText, net, sta, loc, chan, date);
  }

  public WebServiceStationXmlClient(String baseUrlText) {
    super(baseUrlText);
  }

  /**
   * Check the schema version.
   * 
   * @param staMessage the station message.
   * @return true if match, false otherwise.
   */
  protected boolean checkSchemaVersion(FDSNStationXML staMessage) {
    try {
      if (staMessage.checkSchemaVersion()) {
        return true;
      }
    } catch (Exception ex) {
      //
    }
    LOGGER.warn(
        "XM schema of this document ({}) does not match this code ({}) , results may be incorrect.",
        staMessage.getXmlSchemaLocation(), StationXMLTagNames.CURRENT_SCHEMA_VERSION);
    return false;
  }

  /**
   * Fetch the stations.
   * 
   * @param url the URL.
   * 
   * @throws Exception if an error occurs.
   */
  protected void fetch(URL url) throws Exception {
    // likely not an error in the http layer, so assume XML is returned
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLEventReader r = factory.createXMLEventReader(url.toString(), conn.getInputStream());
    XMLEvent e = r.peek();
    while (!e.isStartElement()) {
      e = r.nextEvent(); // eat this one
      e = r.peek(); // peek at the next
    }
    FDSNStationXML staMessage = new FDSNStationXML(r);
    checkSchemaVersion(staMessage);
    final NetworkIterator it = staMessage.getNetworks();
    if (it != null) {
      while (it.hasNext()) {
        final Network n = it.next();
        final StationIterator sit = n.getStations();
        if (sit != null) {
          while (sit.hasNext()) {
            final Station s = sit.next();
            // if (!n.toString().equals(s.getNetworkCode())) {
            if (!n.getCode().equals(s.getCode())) {
              throw new StationXMLException("Station in wrong network: " + n.getCode() + " != "
                  + s.getCode() + "  " + r.peek().getLocation());

            }
            // List<StationEpoch> staEpochs = s.getStationEpochs();
            // for (StationEpoch stationEpoch : staEpochs) {
            // String network = n.toString();
            // String station = s.getCode();
            // FloatType latitude = s.getLatitude();
            // FloatType longitude = s.getLongitude();
            // String siteName = null;
            // if (s.getSite() != null) {
            // siteName = s.getSite().getName();
            // }
            // switch (getLevel()) {
            // case STATION:
            // processStation(createStationInfo(station, network, latitude.getValue(),
            String network = n.getCode();
            String station = s.getCode();
            double latitude = s.getLatitude().getValue();
            double longitude = s.getLongitude().getValue();
            double elevation = s.getElevation().getValue();

            String siteName = null;
            if (s.getSite() != null) {
              siteName = s.getSite().getName();
            }
            switch (getLevel()) {
              case STATION:
                processStation(
                    createStationInfo(station, network, latitude, longitude, elevation, siteName));
                // break;
                // case CHANNEL:
                // List<Channel> chanList = s.getChannelList();
                // for (Channel chan : chanList) {
                // String location = chan.getLocCode();
                // String channel = chan.getCode();
                // processChannel(createChannelInfo(station,
                break;
              case CHANNEL:
                List<Channel> chanList = s.getChannelList();
                for (Channel chan : chanList) {
                  String location = chan.getLocCode();
                  String channel = chan.getCode();
                  processChannel(createChannelInfo(station, channel, network, location, latitude,
                      longitude, elevation, siteName, groupsType));
                }
                // break;
                // default:
                // break;
                break;
              default:
                break;

            }
            // }
          }
        }
      }
    }
    staMessage.closeReader();
  }

  /**
   * Fetch the networks.
   *
   * @param url the URL.
   *
   * @throws Exception if an error occurs.
   */
  protected void fetchNetworks(URL url) throws Exception {
    // likely not an error in the http layer, so assume XML is returned
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLEventReader r = factory.createXMLEventReader(url.toString(), conn.getInputStream());
    XMLEvent e = r.peek();
    while (!e.isStartElement()) {
      e = r.nextEvent(); // eat this one
      e = r.peek(); // peek at the next
    }
    FDSNStationXML staMessage = new FDSNStationXML(r);
    checkSchemaVersion(staMessage);
    List<String> networks = new ArrayList<String>();
    final NetworkIterator it = staMessage.getNetworks();
    if (it != null) {
      while (it.hasNext()) {
        final Network n = it.next();
        networks.add(n.getCode() + "," + n.getDescription());
      }
    }
    staMessage.closeReader();
    setNetworkList(networks);
  }

}
