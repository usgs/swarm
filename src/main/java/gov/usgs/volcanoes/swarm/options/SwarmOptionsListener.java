package gov.usgs.volcanoes.swarm.options;

import java.util.EventListener;

/**
 * SwarmOptionsListener.
 * 
 * @author Tom Parker
 */
public interface SwarmOptionsListener extends EventListener {
  public void optionsChanged();
}
