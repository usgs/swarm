package gov.usgs.volcanoes.swarm.heli;

import java.util.EventListener;

/**
 * HelicorderViewPanelListener interface.
 * 
 * @author Dan Cervelli
 */
public interface HelicorderViewPanelListener extends EventListener {
  public void insetCreated(double st, double et);
}
