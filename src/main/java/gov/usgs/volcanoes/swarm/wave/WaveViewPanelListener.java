package gov.usgs.volcanoes.swarm.wave;

import java.awt.event.MouseEvent;
import java.util.EventListener;

/**
 * WaveViewPanelLister.
 *
 * @author Dan Cervelli
 */
public interface WaveViewPanelListener extends EventListener {
  public void waveZoomed(WaveViewPanel src, double st, double et, double nst, double net);

  public void mousePressed(WaveViewPanel src, MouseEvent e, boolean dragging);

  public void waveClosed(WaveViewPanel src);

  public void waveTimePressed(WaveViewPanel src, MouseEvent e, double j2k);
}
