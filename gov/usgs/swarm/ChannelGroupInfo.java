package gov.usgs.swarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel group information extends the base channel information and adds
 * optional groups.
 * 
 * @author Kevin Frechette (ISTI)
 * 
 */
public class ChannelGroupInfo extends ChannelInfo
{
	/** The groups type. */
	public enum GroupsType
	{
		/** Group by site only. */
		SITE,
		/** Group by network only. */
		NETWORK,
		/** Group by network and site. */
		NETWORK_AND_SITE
	}

	/** Groups map has channel information as key and groups as value */
	private static final Map<ChannelInfo, List<String>> groupsMap = new HashMap<ChannelInfo, List<String>>();

	/**
	 * Get the network group for the specified channel information.
	 * 
	 * @param ch the channel information.
	 * @param groupsType groups type.
	 * @return the group.
	 */
	public static final String getGroup(ChannelInfo ch, GroupsType groupsType)
	{
		switch (groupsType)
		{
		case SITE:
			return ch.getSiteName();
		case NETWORK:
			return "Networks^" + ch.getNetwork();
		case NETWORK_AND_SITE:
			return "Networks^" + ch.getNetwork() + "^" + ch.getSiteName();
		}
		return null;
	}

	/**
	 * Get the groups for the specified channel information.
	 * 
	 * @param ch the channel information.
	 * @param groupsType groups type.
	 * @return the list of groups.
	 */
	public static final List<String> getGroups(ChannelInfo ch, GroupsType groupsType)
	{
		List<String> groups = groupsMap.get(ch);
		if (groups == null)
		{
			groups = new ArrayList<String>(1);
			String group = getGroup(ch, groupsType);
			if (group != null)
			{
				groups.add(group);
			}
			groupsMap.put(ch, groups);
		}
		return groups;
	}

	/** Groups type. */
	private final GroupsType groupsType;

	/**
	 * Create the channel information.
	 * 
	 * @param stationInfo the station information.
	 * @param latitude the latitude.
	 * @param longitude the longitude.
	 */
	public ChannelGroupInfo(StationInfo stationInfo, String channel,
			String location, GroupsType groupsType)
	{
		super(stationInfo, channel, location);
		this.groupsType = groupsType;
	}

	/**
	 * Create the channel information.
	 * 
	 * @param s the code (S C N L).
	 */
	public ChannelGroupInfo(String s)
	{
		this(s, GroupsType.SITE);
	}

	/**
	 * Create the channel information.
	 * 
	 * @param s the code (S C N L).
	 * @param groupsType groups type.
	 */
	public ChannelGroupInfo(String s, GroupsType groupsType)
	{
		super(s);
		this.groupsType = groupsType;
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
	 * @param groupsType groups type.
	 */
	public ChannelGroupInfo(String station, String channel, String network,
			String location, double latitude, double longitude,
			String siteName, GroupsType groupsType)
	{
		super(station, channel, network, location, latitude, longitude,
				siteName);
		this.groupsType = groupsType;
	}

	/**
	 * Get the groups.
	 * 
	 * @return the list of groups.
	 */
	public List<String> getGroups()
	{
		return getGroups(this, groupsType);
	}
}
