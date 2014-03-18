package gov.usgs.swarm.data.fdsnWs;

import edu.sc.seis.seisFile.stationxml.Channel;
import edu.sc.seis.seisFile.stationxml.Network;
import edu.sc.seis.seisFile.stationxml.NetworkIterator;
import edu.sc.seis.seisFile.stationxml.StaMessage;
import edu.sc.seis.seisFile.stationxml.Station;
import edu.sc.seis.seisFile.stationxml.StationEpoch;
import edu.sc.seis.seisFile.stationxml.StationIterator;
import edu.sc.seis.seisFile.stationxml.StationXMLException;
import edu.sc.seis.seisFile.stationxml.StationXMLTagNames;
import gov.usgs.swarm.StationInfo;

import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

public class WebServiceStationXmlClient extends AbstractWebServiceStationClient {
	/**
	 * Create the web service station client.
	 * 
	 * @param baseUrlText
	 *            the base URL text.
	 */
	public static WebServiceStationXmlClient createClient(String[] args) {
		String baseUrlText = getArg(args, 0, DEFAULT_WS_URL);
		String net = getArg(args, 1);
		String sta = getArg(args, 2);
		String loc = getArg(args, 3);
		String chan = getArg(args, 4);
		Date date = WebServiceUtils.parseDate(getArg(args, 5));
		return new WebServiceStationXmlClient(baseUrlText, net, sta, loc, chan,
				date);
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
	 * @param baseUrlText
	 *            the base URL text.
	 * @param net
	 *            the network or null if none.
	 * @param sta
	 *            the station or null if none.
	 * @param loc
	 *            the location or null if none.
	 * @param chan
	 *            the channel or null if none.
	 * @param date
	 *            the date or null if none.
	 */
	public WebServiceStationXmlClient(String baseUrlText, String net,
			String sta, String loc, String chan, Date date) {
		super(baseUrlText, net, sta, loc, chan, date);
	}

	/**
	 * Check the schema version.
	 * 
	 * @param staMessage
	 *            the station message.
	 * @return true if match, false otherwise.
	 */
	protected boolean checkSchemaVersion(StaMessage staMessage) {
		try {
			if (staMessage.checkSchemaVersion()) {
				return true;
			}
		} catch (Exception ex) {
		}
		String message = "XM schema of this document ("
				+ staMessage.getXmlSchemaLocation()
				+ ") does not match this code ("
				+ StationXMLTagNames.SCHEMA_VERSION
				+ ") , results may be incorrect.";
		WebServiceUtils.warning(message);
		return false;
	}

	/**
	 * Fetch the stations.
	 * 
	 * @param url
	 *            the URL.
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	protected void fetch(URL url) throws Exception {
		// likely not an error in the http layer, so assume XML is returned
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader r = factory.createXMLEventReader(url.toString(),
				conn.getInputStream());
		XMLEvent e = r.peek();
		while (!e.isStartElement()) {
			e = r.nextEvent(); // eat this one
			e = r.peek(); // peek at the next
		}
		StaMessage staMessage = new StaMessage(r);
		checkSchemaVersion(staMessage);
		final NetworkIterator it = staMessage.getNetworks();
		if (it != null) {
			while (it.hasNext()) {
				final Network n = it.next();
				final StationIterator sit = n.getStations();
				if (sit != null) {
					while (sit.hasNext()) {
						final Station s = sit.next();
						if (!n.getNetCode().equals(s.getNetCode())) {
							throw new StationXMLException(
									"Station in wrong network: "
											+ n.getNetCode() + " != "
											+ s.getNetCode() + "  "
											+ r.peek().getLocation());

						}
						List<StationEpoch> staEpochs = s.getStationEpochs();
						for (StationEpoch stationEpoch : staEpochs) {
							String network = n.getNetCode();
							String station = s.getStaCode();
							double latitude = stationEpoch.getLat();
							double longitude = stationEpoch.getLon();
							String siteName = null;
							if (stationEpoch.getSite() != null) {
								siteName = stationEpoch.getSite().getName();
							}
							switch (getLevel()) {
							case STATION:
								processStation(createStationInfo(station,
										network, latitude, longitude, siteName));
								break;
							case CHANNEL:
								List<Channel> chanList = stationEpoch
										.getChannelList();
								for (Channel chan : chanList) {
									String location = chan.getLocCode();
									String channel = chan.getChanCode();
									processChannel(createChannelInfo(station,
											channel, network, location,
											latitude, longitude, siteName,
											groupsType));
								}
								break;
							default:
								break;
							}
						}
					}
				}
			}
		}
		staMessage.closeReader();
	}
}
