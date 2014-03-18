package gov.usgs.swarm.data.fdsnWs;

import gov.usgs.swarm.ChannelGroupInfo;
import gov.usgs.swarm.GroupsType;
import gov.usgs.swarm.ChannelInfo;
import gov.usgs.swarm.StationInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractWebServiceStationClient
{
	/** The station service output level. */
	public enum OutputLevel
	{
		NETWORK("net"), STATION("sta"), CHANNEL("chan"), RESPONSE("resp");

		private final String s;

		OutputLevel(String s)
		{
			this.s = s;
		}

		public String toString()
		{
			return s;
		}
	}

	/** Default web services URL for station. */
//	public static final String DEFAULT_WS_URL = "http://www.iris.edu/ws/station/query";
	public static final String DEFAULT_WS_URL =  "http://service.iris.edu/fdsnws/station/1/query";

	/** Separator text. */
	private static final String separatorText = "&";
	/** Equals text */
	private static final String equalsText = "=";
	/** */
	private static final String startTimeText = "starttime";

	/**
	 * Create an empty channel list.
	 * 
	 * @return the channel list.
	 */
	public static List<String> createChannelList()
	{
		return createList();
	}

	/**
	 * Create an empty list.
	 * 
	 * @return the list.
	 */
	public static <T> List<T> createList()
	{
		return new ArrayList<T>();
	}

	/**
	 * Create an empty station list.
	 * 
	 * @return the station list.
	 */
	public static List<StationInfo> createStationList()
	{
		return createList();
	}

	/**
	 * Get the argument at the specified index.
	 * 
	 * @param args the arguments.
	 * @param index the index.
	 * @return the argument or null if none.
	 */
	protected static String getArg(String[] args, int index)
	{
		return getArg(args, index, null);
	}

	/**
	 * Get the argument at the specified index.
	 * 
	 * @param args the arguments.
	 * @param index the index.
	 * @param def the default value.
	 * @return the argument or the default value if none.
	 */
	protected static String getArg(String[] args, int index, String def)
	{
		if (args.length > index)
			return args[index];
		return def;
	}

	/** The reader. */
	private BufferedReader reader;

	/** The HTTP URL Connection. */
	protected HttpURLConnection conn;
	/** The error message. */
	protected final StringBuilder error = new StringBuilder();
	/** The base URL text. */
	private final String baseUrlText;
	/** The network name. */
	private final String net;
	/** The station name. */
	private final String sta;
	/** The location name. */
	public final String loc;
	/** The channel name. */
	private final String chan;
	/** The date. */
	private final Date date;
	/** Groups type. */
	protected final GroupsType groupsType;

	/** The output level. */
	private OutputLevel level = OutputLevel.CHANNEL;

	/** The current station. */
	private StationInfo currentStation;

	/** The station list. */
	private List<StationInfo> stationList;

	/** The channel list. */
	private List<String> channelList;

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
	public AbstractWebServiceStationClient(String baseUrlText, String net,
			String sta, String loc, String chan, Date date)
	{
		this.baseUrlText = baseUrlText;
		this.net = net;
		this.sta = sta;
		this.loc = loc;
		this.chan = chan;
		this.date = date;
		// if network contains wild card
		if (net == null || net.length() == 0 || net.matches(".*[\\*?].*"))
		{
			if (useOnlyChannels())
				groupsType = GroupsType.NETWORK;
			else
				groupsType = GroupsType.NETWORK_AND_SITE;
		}
		else
		{
			groupsType = GroupsType.SITE;
		}
	}

	/**
	 * Append the name and value to the text.
	 * 
	 * @param s the text.
	 * @param name the name.
	 * @param value the value or null if none.
	 * @return the text.
	 */
	protected String append(String s, String name, Object value)
	{
		if (value instanceof Date)
		{
			value = WebServiceUtils.getDateText((Date) value);
		}
		if (value == null || value.toString().length() == 0)
		{
			return s;
		}
		if (s.length() > 0)
		{
			s += separatorText;
		}
		s += name + equalsText + value.toString();
		return s;
	}

	/**
	 * Clear the error message.
	 */
	public void clearError()
	{
		error.setLength(0);
	}

	/**
	 * Determine if latitude and longitude should be cleared.
	 * 
	 * @return true if latitude and longitude should be cleared, false
	 *         otherwise.
	 */
	protected boolean clearLatLon()
	{
		// clear if all networks
		return isAllNetworks();
	}

	/**
	 * Close the connection.
	 */
	public void close()
	{
		if (reader != null)
		{
			try
			{
				reader.close();
			}
			catch (Exception ex)
			{
			}
			reader = null;
		}
		if (conn != null)
		{
			conn.disconnect();
			conn = null;
		}
	}

	/**
	 * Create the channel information.
	 * 
	 * @param station the station.
	 * @param channel the channel.
	 * @param network the network.
	 * @param location the location.
	 * @param latitude the latitude.
	 * @param longitude the longitude.
	 * @param siteName the site name.
	 * @param groupsType groups type.
	 * @return the channel information.
	 */
	protected ChannelInfo createChannelInfo(String station, String channel,
			String network, String location, double latitude, double longitude,
			String siteName, GroupsType groupsType)
	{
		if (currentStation == null)
		{
			if (clearLatLon())
			{
				latitude = Double.NaN;
				longitude = Double.NaN;
			}
			return new ChannelGroupInfo(station, channel, network, location,
					latitude, longitude, siteName, groupsType);
		}
		else
		{
			return new ChannelGroupInfo(currentStation, channel, location,
					groupsType);
		}
	}

	/**
	 * Create the station information.
	 * 
	 * @param station the station.
	 * @param network the network.
	 * @param latitude the latitude.
	 * @param longitude the longitude.
	 * @param siteName the site name.
	 * @return the station information.
	 */
	protected StationInfo createStationInfo(String station, String network,
			double latitude, double longitude, String siteName)
	{
		if (clearLatLon())
		{
			latitude = Double.NaN;
			longitude = Double.NaN;
		}
		return new StationInfo(station, network, latitude, longitude, siteName);
	}

	/**
	 * Fetch the stations.
	 * 
	 * @throws Exception if an error occurs.
	 */
	protected void fetch() throws Exception
	{
		final URL url = getURL();
		final URLConnection urlConn = url.openConnection();
		if (urlConn instanceof HttpURLConnection)
		{
			conn = (HttpURLConnection) urlConn;
			if (conn.getResponseCode() != 200) // if response not OK
			{
				final BufferedReader errorReader = new BufferedReader(
						new InputStreamReader(conn.getErrorStream()));
				error.append("Error in connection with url: " + url + "\n");
				for (String line; (line = readLine(errorReader)) != null;)
				{
					error.append(line + "\n");
				}
				errorReader.close();
				return;
			}
			else
			{
				fetch(url);
			}
		}
	}

	/**
	 * Fetch the stations.
	 * 
	 * @param url the URL.
	 * 
	 * @throws Exception if an error occurs.
	 */
	protected abstract void fetch(URL url) throws Exception;

	/**
	 * Fetch the channels and optionally put them in the channel list if it is
	 * set.
	 * 
	 * @return an error message or null if none.
	 * @see #getChannelList()
	 * @see #setChannelList(List)
	 */
	public String fetchChannels()
	{
		clearError();
		try
		{
			setLevel(OutputLevel.CHANNEL);
			fetch();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		close();
		String error = getError();
		clearError();
		return error.length() == 0 ? null : error;
	}

	/**
	 * Fetch the stations and optionally put them in the stations list if it is
	 * set.
	 * 
	 * @return an error message or null if none.
	 * @see #getStationList()
	 * @see #setStationList()
	 */
	public String fetchStations()
	{
		clearError();
		try
		{
			setLevel(OutputLevel.STATION);
			fetch();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		close();
		String error = getError();
		clearError();
		return error.length() == 0 ? null : error;
	}

	/**
	 * Get the base URL text.
	 * 
	 * @return the base URL text.
	 */
	protected String getBaseUrlText()
	{
		return baseUrlText + "?";
	}

	/**
	 * Get the channel list.
	 * 
	 * @return the channel list or null if none.
	 * @see #fetchChannels()
	 */
	public List<String> getChannelList()
	{
		return channelList;
	}

	/**
	 * Get the current station.
	 * 
	 * @return the current station or null if none.
	 */
	public StationInfo getCurrentStation()
	{
		return currentStation;
	}

	/**
	 * Get the error message.
	 * 
	 * @return the error message.
	 */
	public String getError()
	{
		return error.toString();
	}

	/**
	 * Get the output level.
	 * 
	 * @return the output level.
	 */
	public OutputLevel getLevel()
	{
		return level;
	}

	/**
	 * Get the reader.
	 * 
	 * @return the reader.
	 * @throws IOException if an I/O Exception occurs.
	 */
	public BufferedReader getReader() throws IOException
	{
		BufferedReader reader = this.reader;
		if (reader == null)
		{
			reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			this.reader = reader;
		}
		return reader;
	}

	/**
	 * Get the station list.
	 * 
	 * @return the station list or null if none.
	 * @see #fetchStations()
	 */
	public List<StationInfo> getStationList()
	{
		return stationList;
	}

	/**
	 * Get the URL.
	 * 
	 * @return the URL.
	 * @throws MalformedURLException if the URL text is invalid.
	 */
	protected URL getURL() throws MalformedURLException
	{
		return new URL(getUrlTextWithTime());
	}

	/**
	 * Get the URL text.
	 * 
	 * @return the URL text.
	 */
	protected String getUrlText()
	{
		String urlText = getBaseUrlText();
		urlText = append(urlText, "level", level);
		if (currentStation == null)
		{
			urlText = append(urlText, "net", net);
			urlText = append(urlText, "sta", sta);
		}
		else
		{
			urlText = append(urlText, "net", currentStation.getNetwork());
			urlText = append(urlText, "sta", currentStation.getStation());
		}
		urlText = append(urlText, "loc", loc);
		urlText = append(urlText, "chan", chan);
		urlText = append(urlText, startTimeText, date);
		return urlText;
	}

	/**
	 * Get the URL text with time.
	 * 
	 * @return the URL text with time.
	 */
	protected String getUrlTextWithTime()
	{
		String urlText = getUrlText();
		if (!urlText.contains(startTimeText))
		{
			urlText = append(urlText, startTimeText, new Date());
		}
		return urlText;
	}

	/**
	 * Determine if all networks.
	 * 
	 * @return true if all networks, false otherwise.
	 */
	protected boolean isAllNetworks()
	{
		return net == null || net.length() == 0 || net.equals("*");
	}

	/**
	 * Process the channel.
	 * 
	 * @param ch the channel information.
	 */
	public void processChannel(ChannelInfo ch)
	{
		if (channelList == null)
			System.out.println(ch);
		else
			channelList.add(ch.toString());
	}

	/**
	 * Process the station.
	 * 
	 * @param si the station information.
	 */
	public void processStation(StationInfo si)
	{
		if (stationList == null)
			System.out.println(si);
		else
			stationList.add(si);
	}

	/**
	 * Read a line from the reader.
	 * 
	 * @param reader the reader.
	 * @return the line or null if none or error.
	 */
	protected String readLine(BufferedReader reader)
	{
		try
		{
			return reader.readLine();
		}
		catch (IOException ex)
		{
		}
		return null;
	}

	/**
	 * Set the channel list.
	 * 
	 * @param channelList the channel list.
	 */
	public void setChannelList(List<String> channelList)
	{
		this.channelList = channelList;
	}

	/**
	 * Set the current station.
	 * 
	 * @param station the station or null if none.
	 */
	public void setCurrentStation(StationInfo station)
	{
		currentStation = station;
	}

	/**
	 * Set the output level.
	 * 
	 * @param level the output level.
	 */
	public void setLevel(OutputLevel level)
	{
		this.level = level;
	}

	/**
	 * Set the station list.
	 * 
	 * @param stationList the station list.
	 */
	public void setStationList(List<StationInfo> stationList)
	{
		this.stationList = stationList;
	}

	/**
	 * Determines if only channels should be fetched.
	 * 
	 * @return true if only channels should be fetched.
	 */
	protected boolean useOnlyChannels()
	{
		return isAllNetworks();
	}
}
