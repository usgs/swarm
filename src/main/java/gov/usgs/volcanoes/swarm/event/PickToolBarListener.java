package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.swarm.wave.WaveViewToolBarListener;

/**
 * Listener interface for PickToolBar objects.
 * 
 * @author Tom Parker
 *
 */
public interface PickToolBarListener extends WaveViewToolBarListener {
  public void setWaveHeight(int height);

  public void scaleTime(final double pct);

  public void back();

  public void shiftTime(final double pct);

  public void writeImage();

  public void sortChannelsByNearest();
}
