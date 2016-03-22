package gov.usgs.volcanoes.swarm.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gov.usgs.volcanoes.swarm.wave.WaveViewToolBarListener;

public interface PickToolBarListener extends WaveViewToolBarListener {
  public void setWaveHeight(int height);

  public void scaleTime(final double pct);

  public void back();

  public void shiftTime(final double pct);

  public void writeImage();
}
