package gov.usgs.volcanoes.swarm;

import java.util.List;

/**
 * This interface defines the channel information.
 * 
 * @author Kevin Frechette (ISTI)
 */
public interface IChannelInfo
{
	/**
	 * Get the channel name.
	 * 
	 * @return the channel name.
	 */
	public String getChannel();

	/**
	 * Get the formatted SCNL.
	 * 
	 * @return the formatted SCNL.
	 */
	public String getFormattedSCNL();

	/**
	 * Get the groups.
	 * 
	 * @return the list of groups.
	 */
	public List<String> getGroups();

	/**
	 * Get the latitude.
	 * 
	 * @return the latitude.
	 */
	public double getLatitude();

	/**
	 * Get the location.
	 * 
	 * @return the location.
	 */
	public String getLocation();

	/**
	 * Get the longitude.
	 * 
	 * @return the longitude.
	 */
	public double getLongitude();

	/**
	 * Get the network name.
	 * 
	 * @return the network name.
	 */
	public String getNetwork();

	/**
	 * Get the site name.
	 * 
	 * @return the site name.
	 */
	public String getSiteName();

	/**
	 * Get the station name.
	 * 
	 * @return the station name.
	 */
	public String getStation();
}
