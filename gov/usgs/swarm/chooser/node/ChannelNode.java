package gov.usgs.swarm.chooser.node;

import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.SwarmConfig;
import gov.usgs.util.Util;

import javax.swing.Icon;

public class ChannelNode extends ChooserNode {
    public static final String TOOL_TIP_DATE_FORMAT = "MMM dd, yyyy";

    private static final long serialVersionUID = 1L;
    private String channel;

    public ChannelNode(String c) {
        channel = c;
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

    public String getLabel() {
        return channel;
    }

    public String getChannel() {
        return channel;
    }
    
    public String getToolTip() {
        Metadata md = SwarmConfig.getInstance().getMetadata(channel);
        double minTime = md.getMinTime();
        double maxTime = md.getMaxTime();
        if (Double.isNaN(minTime) || Double.isNaN(maxTime))
            return null;
        else
            return Util.j2KToDateString(minTime, TOOL_TIP_DATE_FORMAT) + " - " + Util.j2KToDateString(maxTime, TOOL_TIP_DATE_FORMAT);
    }
}
