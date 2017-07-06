package gov.usgs.volcanoes.swarm;

import java.util.List;

/**
 * Channel group information extends the base channel information and adds optional groups.
 * 
 * @author Kevin Frechette (ISTI)
 * 
 */
public class ChannelGroupInfo extends ChannelInfo {
  /** Groups type. */
  private final GroupsType groupsType;

  /**
   * @param stationInfo station information.
   * @param channel channel name
   * @param location location 
   * @param groupsType group type
   */
  public ChannelGroupInfo(StationInfo stationInfo, String channel, String location,
      GroupsType groupsType) {
    super(stationInfo, channel, location);
    this.groupsType = groupsType;
  }

  /**
   * Create the channel information.
   * 
   * @param s the code (S C N L).
   */
  public ChannelGroupInfo(String s) {
    this(s, GroupsType.SITE);
  }

  /**
   * Create the channel information.
   * 
   * @param s the code (S C N L).
   * @param groupsType groups type.
   */
  public ChannelGroupInfo(String s, GroupsType groupsType) {
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
  public ChannelGroupInfo(String station, String channel, String network, String location,
      double latitude, double longitude, String siteName, GroupsType groupsType) {
    super(station, channel, network, location, latitude, longitude, siteName);
    this.groupsType = groupsType;
  }

  /**
   * Get the groups.
   * 
   * @return the list of groups.
   */
  public List<String> getGroups() {
    return getGroups(this, groupsType);
  }
}
