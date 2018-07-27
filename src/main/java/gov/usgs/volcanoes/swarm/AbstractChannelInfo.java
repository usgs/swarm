package gov.usgs.volcanoes.swarm;

import java.util.List;

/**
 * Abstract channel information.
 * 
 * @author Kevin Frechette (ISTI)
 */
public abstract class AbstractChannelInfo {

  /**
   * Get the channel name.
   * 
   * @return the channel name.
   */
  public abstract String getChannel();

  /**
   * Get the groups.
   * 
   * @return the list of groups.
   */
  public abstract List<String> getGroups();

  /**
   * Get the latitude.
   * 
   * @return the latitude.
   */
  public abstract double getLatitude();

  /**
   * Get the location.
   * 
   * @return the location.
   */
  public abstract String getLocation();

  /**
   * Get the longitude.
   * 
   * @return the longitude.
   */
  public abstract double getLongitude();

  /**
   * Get the elevation.
   * 
   * @return the elevation.
   */
  public abstract double getHeight();
  
  /**
   * Get the network name.
   * 
   * @return the network name.
   */
  public abstract String getNetwork();

  /**
   * Get the site name.
   * 
   * @return the site name.
   */
  public abstract String getSiteName();

  /**
   * Get the station name.
   * 
   * @return the station name.
   */
  public abstract String getStation();

  /**
   * Determines if this channel information is the same as another.
   * 
   * @return true if this channel information is the same as another, false
   *         otherwise.
   */
  public boolean equals(Object obj) {
    return obj instanceof AbstractChannelInfo
        && getFormattedSCNL().equals(
            ((AbstractChannelInfo) obj).getFormattedSCNL());
  }

  /**
   * Get the formatted SCNL.
   * 
   * @return the formatted SCNL.
   */
  public String getFormattedSCNL() {
    return ChannelUtil.getFormattedSCNL(getStation(), getChannel(), getNetwork(),
        getLocation());
  }

  /**
   * Get the hash code.
   * 
   * @return the hash code.
   */
  public int hashCode() {
    return getFormattedSCNL().hashCode();
  }

  /**
   * Get the string representation of the channel information.
   * 
   * @return the string representation of the channel information.
   */
  public String toString() {
    return getFormattedSCNL();
  }
}
