package gov.usgs.swarm.options;

import java.util.EventListener;

/**
 * 
 * @author Tom Parker
 */
public interface SwarmOptionsListener extends EventListener
{
    public void optionsChanged();
}
