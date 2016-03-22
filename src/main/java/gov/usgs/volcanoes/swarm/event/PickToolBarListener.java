package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.swarm.wave.WaveViewToolBarListener;

public interface PickToolBarListener extends WaveViewToolBarListener {
  public void setWaveHeight(int height);

  public void gotoTime(String t);
}
