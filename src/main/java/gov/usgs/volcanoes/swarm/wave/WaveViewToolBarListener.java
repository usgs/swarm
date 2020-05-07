package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;
import javax.swing.AbstractButton;

public interface WaveViewToolBarListener {
  public void displaySettingsDialog();

  public void mapKeyStroke(String keyStroke, String name, AbstractButton button);

  public void setType(ViewType viewType);
}
