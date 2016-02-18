package gov.usgs.volcanoes.swarm.picker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

public class PickerWavePanel extends AbstractWavePanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(PickerWavePanel.class);

  public PickerWavePanel(WaveViewPanel insetWavePanel) {
    super(insetWavePanel);
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    PhasePopup phasePopup;

    double[] t = getTranslation();
    int x = e.getX();
    double cursorTime = x * t[0] + t[1];
    LOGGER.debug("New phase: {} @ {}", channel, J2kSec.toDateString(cursorTime));

    phasePopup = new PhasePopup(channel, J2kSec.asEpoch(cursorTime));
    phasePopup.show(e.getComponent(), e.getX(), e.getY());
  }
}
