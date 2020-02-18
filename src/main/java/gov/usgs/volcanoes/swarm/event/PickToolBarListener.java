/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
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
