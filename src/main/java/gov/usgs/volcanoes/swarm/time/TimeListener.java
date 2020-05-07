package gov.usgs.volcanoes.swarm.time;

import java.util.EventListener;

/**
 * TimeListener.
 * 
 * @author Dan Cervelli
 */
public interface TimeListener extends EventListener {
  public void timeChanged(double j2k);
}
