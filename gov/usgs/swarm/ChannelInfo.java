package gov.usgs.swarm;

import java.util.Collections;
import java.util.List;

/**
 * Base channel information without groups. This class should be extended and
 * the 'getGroups()' method should be over-ridden if group support is desired.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class ChannelInfo
{
	/** Empty string. */
	public static final String EMPTY = "";

	/**
	 * Get the formatted SCNL.
	 * 
	 * @param station the station name.
	 * @param channel the channel name.
	 * @param network the network name.
	 * @param location the location name.
	 * @return the the formatted SCNL.
	 */
	public static final String getFormattedSCNL(String station, String channel,
			String network, String location)
	{
		return station + " " + channel + " " + network
				+ (location.length() > 0 ? (" " + location) : EMPTY);
	}

	/** The station information. */
	private final StationInfo stationInfo;

	/** The channel name. */
	private final String channel;

	/** The location name. */
	private final String location;
	/** The the formatted SCNL. */
	private final String formattedSCNL;

	/**
	 * Create the channel information.
	 * 
	 * @param stationInfo the station information.
	 * @param latitude the latitude.
	 * @param longitude the longitude.
	 */
	public ChannelInfo(StationInfo stationInfo, String channel, String location)
	{
		this.stationInfo = stationInfo;
		this.channel = channel;
		this.location = location;
		formattedSCNL = getFormattedSCNL(stationInfo.getStation(), channel,
				stationInfo.getNetwork(), location);
	}

	/**
	 * Create the channel information.
	 * 
	 * @param s the code (S C N L).
	 */
	public ChannelInfo(String s)
	{
		String station;
		String channel = EMPTY;
		String network = EMPTY;
		String location = EMPTY;
		double latitude = Double.NaN;
		double longitude = Double.NaN;
		String[] ss = s.split(" ");
		station = ss[0];
		if (ss.length > 2)
		{
			channel = ss[1];
			network = ss[2];
			if (ss.length > 3)
			{
				location = ss[3];
				if (ss.length > 4)
				{
					latitude = StationInfo.parseDouble(ss[4]);
					if (ss.length > 5)
					{
						longitude = StationInfo.parseDouble(ss[5]);
					}
				}
			}
		}
		stationInfo = new StationInfo(station, network, latitude, longitude);
		this.channel = channel;
		this.location = location;
		formattedSCNL = getFormattedSCNL(station, channel, network, location);
	}

	/**
	 * Create the channel information.
	 * 
	 * @param station the station name.
	 * @param channel the channel name.
	 * @param network the network name.
	 * @param location the location name.
	 * @param latitude the latitude.
	 * @param longitude the longitude.
	 * @param siteName the site name.
	 */
	public ChannelInfo(String station, String channel, String network,
			String location, double latitude, double longitude, String siteName)
	{
		this(new StationInfo(station, network, latitude, longitude, siteName),
				channel, location);
	}

	/**
	 * Determines if this channel information is the same as another.
	 * 
	 * @return true if this channel information is the same as another, false
	 *         otherwise.
	 */
	public boolean equals(Object obj)
	{
		return obj instanceof ChannelInfo
				&& getFormattedSCNL().equals(
						((ChannelInfo) obj).getFormattedSCNL());
	}

	/**
	 * Get the channel name.
	 * 
	 * @return the channel name.
	 */
	public String getChannel()
	{
		return channel;
	}

	/**
	 * Get the formatted SCNL.
	 * 
	 * @return the formatted SCNL.
	 */
	public String getFormattedSCNL()
	{
		return formattedSCNL;
	}

	/**
	 * Get the groups.
	 * 
	 * @return the list of groups.
	 */
	public List<String> getGroups()
	{
		return Collections.emptyList();
	}

	/**
	 * Get the latitude.
	 * 
	 * @return the latitude.
	 */
	public double getLatitude()
	{
		return stationInfo.getLatitude();
	}

	/**
	 * Get the location.
	 * 
	 * @return the location.
	 */
	public String getLocation()
	{
		return location;
	}

	/**
	 * Get the longitude.
	 * 
	 * @return the longitude.
	 */
	public double getLongitude()
	{
		return stationInfo.getLongitude();
	}

	/**
	 * Get the network name.
	 * 
	 * @return the network name.
	 */
	public String getNetwork()
	{
		return stationInfo.getNetwork();
	}

	/**
	 * Get the site name.
	 * 
	 * @return the site name.
	 */
	public String getSiteName()
	{
		return stationInfo.getSiteName();
	}

	/**
	 * Get the station name.
	 * 
	 * @return the station name.
	 */
	public String getStation()
	{
		return stationInfo.getStation();
	}

	/**
	 * Get the station information.
	 * 
	 * @return the station information.
	 */
	public StationInfo getStationInfo()
	{
		return stationInfo;
	}

	/**
	 * Get the hash code.
	 * 
	 * @return the hash code.
	 */
	public int hashCode()
	{
		return getFormattedSCNL().hashCode();
	}

	/**
	 * Get the string representation of the channel information.
	 * 
	 * @return the string representation of the channel information.
	 */
	public String toString()
	{
		return formattedSCNL;
	}
}
