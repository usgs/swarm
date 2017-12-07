package gov.usgs.volcanoes.swarm;

import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel utility methods.
 * 
 * @author Kevin Frechette (ISTI)
 */
public final class ChannelUtil {
  /** Groups map has channel information as key and groups as value. */
  private static final Map<AbstractChannelInfo, List<String>> groupsMap =
      new HashMap<AbstractChannelInfo, List<String>>();

  private ChannelUtil() {
    // uninstantiatable
  }
  /**
   * Add the channel.
   * 
   * @param channels the list of channels.
   * @param ch the channel information.
   * @param source the seismic data source.
   * @return the channel information text.
   */
  public static String addChannel(List<String> channels, AbstractChannelInfo ch,
      SeismicDataSource source) {
    final String formattedScnl = ch.getFormattedSCNL();
    if (!channels.contains(formattedScnl)) {
      Metadata md = SwarmConfig.getInstance().getMetadata(formattedScnl, true);
      md.updateLongitude(ch.getLongitude());
      md.updateLatitude(ch.getLatitude());
      for (String g : ch.getGroups()) {
        md.addGroup(g);
      }
      if (ch.getSiteName() != null && !ch.getSiteName().equals(ch.getStation())) {
        md.updateAlias(ch.getSiteName());
      }
      md.source = source;
      channels.add(formattedScnl);
    }
    return formattedScnl;
  }

  /**
   * Assign the channels.
   * 
   * @param channels the list of channels.
   * @param source the seismic data source.
   */
  public static void assignChannels(List<String> channels, SeismicDataSource source) {
    Collections.sort(channels);
    SwarmConfig.getInstance().assignMetadataSource(channels, source);
  }

  /**
   * Get the formatted SCNL.
   * 
   * @param station the station name.
   * @param channel the channel name.
   * @param network the network name.
   * @param location the location name.
   * @return the the formatted SCNL.
   */
  public static final String getFormattedSCNL(String station, String channel, String network,
      String location) {
    return station + " " + channel + " " + network
        + (location.length() > 0 ? (" " + location) : "");
  }

  /**
   * Get the network group for the specified channel information.
   * 
   * @param ch the channel information.
   * @param groupsType groups type.
   * @return the group.
   */
  private static final String getGroup(AbstractChannelInfo ch, GroupsType groupsType) {
    switch (groupsType) {
      case SITE:
        return getSiteName(ch);
      case NETWORK:
        return "Networks^" + ch.getNetwork();
      case NETWORK_AND_SITE:
        return "Networks^" + ch.getNetwork() + "^" + getSiteName(ch);
      default:
        break;
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
  public static final List<String> getGroups(AbstractChannelInfo ch, GroupsType groupsType) {
    List<String> groups = groupsMap.get(ch);
    if (groups == null) {
      groups = new ArrayList<String>(1);
      String group = getGroup(ch, groupsType);
      if (group != null) {
        groups.add(group);
      }
      groupsMap.put(ch, groups);
    }
    return groups;
  }

  /**
   * Get the site name.
   * 
   * @param ch the channel information.
   * @return the site name if available otherwise the station name.
   */
  private static final String getSiteName(AbstractChannelInfo ch) {
    String s = ch.getSiteName();
    if (s == null) {
      s = ch.getStation();
    }
    return s;
  }
}
