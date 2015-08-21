package gov.usgs.volcanoes.swarm;

/**
 * Station information.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class StationInfo
{
	/**
	 * Get the double value for the specified text.
	 * 
	 * @param s the text or null or empty if none.
	 * @return the double or NaN if none.
	 */
	public static double parseDouble(String s)
	{
		if (s != null && s.length() != 0)
		{
			try
			{
				return Double.parseDouble(s);
			}
			catch (Exception ex)
			{
			}
		}
		return Double.NaN;
	}

	/** The station name. */
	private final String station;

	/** The network name. */
	private final String network;

	/** The latitude. */
	private final double latitude;

	/** The longitude. */
	private final double longitude;

	/** The site name. */
	private final String siteName;

	public StationInfo(String station, String network, double latitude,
			double longitude)
	{
		this(station, network, latitude, longitude, (String) null);
	}

	public StationInfo(String station, String network, double latitude,
			double longitude, String siteName)
	{
		this.station = station;
		this.network = network;
		this.latitude = latitude;
		this.longitude = longitude;
		this.siteName = siteName != null ? siteName : station;
	}

	/**
	 * Get the latitude.
	 * 
	 * @return the latitude.
	 */
	public double getLatitude()
	{
		return latitude;
	}

	/**
	 * Get the longitude.
	 * 
	 * @return the longitude.
	 */
	public double getLongitude()
	{
		return longitude;
	}

	/**
	 * Get the network name.
	 * 
	 * @return the network name.
	 */
	public String getNetwork()
	{
		return network;
	}

	/**
	 * Get the site name.
	 * 
	 * @return the site name.
	 */
	public String getSiteName()
	{
		return siteName;
	}

	/**
	 * Get the station name.
	 * 
	 * @return the station name.
	 */
	public String getStation()
	{
		return station;
	}

	/**
	 * Get the string representation of the station information.
	 * 
	 * @return the string representation of the station information.
	 */
	public String toString()
	{
		return station + " " + network + " " + latitude + " " + longitude + " "
				+ siteName;
	}
}
