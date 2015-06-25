package gov.usgs.swarm.chooser.node;

import java.awt.Color;

import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.SwarmConfig;
import gov.usgs.util.Util;

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

    public Icon getIcon() {
        Metadata md = SwarmConfig.getInstance().getMetadata(channel);
        if (md == null || !md.isTouched())
            return Icons.graybullet;
        else if (md.hasLonLat())
            return Icons.bluebullet;
        else
            return Icons.bullet;
    }

    public String getChannel() {
        return channel;
    }

    public String getToolTip() {
        Metadata md = SwarmConfig.getInstance().getMetadata(channel);
        if (md == null)
            return null;
        
        double minTime = md.getMinTime();
        double maxTime = md.getMaxTime();
        if (Double.isNaN(minTime) || Double.isNaN(maxTime))
            return "No data";
        else
            return Util.j2KToDateString(minTime, TOOL_TIP_DATE_FORMAT) + " - "
                    + Util.j2KToDateString(maxTime, TOOL_TIP_DATE_FORMAT);
    }

    public boolean isStale() {
        Metadata md = SwarmConfig.getInstance().getMetadata(channel);
        if (md == null)
            return false;
        
        double maxTime = md.getMaxTime();
        if (Double.isNaN(maxTime) || Util.nowJ2K() - maxTime > ONE_DAY_S)
            return true;
        else
            return false;
    }
}
