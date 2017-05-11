package gov.usgs.volcanoes.swarm.wave;

import java.awt.event.MouseEvent;

/**
 * Wave View Panel Adapter.
 * 
 * @author Dan Cervelli
 */
public abstract class WaveViewPanelAdapter implements WaveViewPanelListener {
  public void waveZoomed(WaveViewPanel src, double st, double et, double nst, double net) {}

  public void mousePressed(WaveViewPanel src, MouseEvent e, boolean dragging) {}

  public void waveClosed(WaveViewPanel src) {}

  public void waveTimePressed(WaveViewPanel src, MouseEvent e, double j2k) {}
}
