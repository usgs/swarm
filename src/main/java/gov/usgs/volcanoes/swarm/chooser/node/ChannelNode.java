/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.chooser.node;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;

import javax.swing.Icon;

public class ChannelNode extends AbstractChooserNode {
  public static final int ONE_DAY_S = 60 * 60 * 24;
  public static final String TOOL_TIP_DATE_FORMAT = "MMM dd, yyyy";

  private static final long serialVersionUID = 1L;
  private String channel;

  public ChannelNode(String c) {
    channel = c;
    label = channel;
  }

  /**
   * Get icon for channel bullet.
   * 
   * @see gov.usgs.volcanoes.swarm.chooser.node.AbstractChooserNode#getIcon()
   */
  public Icon getIcon() {
    Metadata md = SwarmConfig.getInstance().getMetadata(channel);
    if (md == null || !md.isTouched()) {
      return Icons.graybullet;
    } else if (md.hasLonLat()) {
      return Icons.bluebullet;
    } else {
      return Icons.bullet;
    }
  }

  public String getChannel() {
    return channel;
  }

  /**
   * Get tool tip for channel bullet.
   * 
   * @see gov.usgs.volcanoes.swarm.chooser.node.AbstractChooserNode#getToolTip()
   */
  public String getToolTip() {
    Metadata md = SwarmConfig.getInstance().getMetadata(channel);
    if (md == null) {
      return null;
    }
    double minTime = md.getMinTime();
    double maxTime = md.getMaxTime();
    if (Double.isNaN(minTime) || Double.isNaN(maxTime)) {
      return "No data";
    } else {
      return J2kSec.format(TOOL_TIP_DATE_FORMAT, minTime) + " - "
          + J2kSec.format(TOOL_TIP_DATE_FORMAT, maxTime);
    }
  }

  /**
   * Check to see if data is stale.
   * 
   * @return
   */
  public boolean isStale() {
    Metadata md = SwarmConfig.getInstance().getMetadata(channel);
    if (md == null) {
      return false;
    }
    double maxTime = md.getMaxTime();
    if (Double.isNaN(maxTime) || J2kSec.now() - maxTime > ONE_DAY_S) {
      return true;
    } else {
      return false;
    }
  }
}
