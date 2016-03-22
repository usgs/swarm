package gov.usgs.volcanoes.swarm.wave;

import javax.swing.AbstractButton;

import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;

public interface WaveViewToolBarListener {
  public void displaySettingsDialog();
  public void mapKeyStroke(String keyStroke,String name, AbstractButton button);
  public void setType(ViewType viewType);
}
