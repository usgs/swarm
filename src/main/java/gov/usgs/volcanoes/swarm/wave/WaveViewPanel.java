package gov.usgs.volcanoes.swarm.wave;

import java.awt.Color;
import java.awt.event.MouseEvent;

import gov.usgs.volcanoes.swarm.SwarmConfig;

/**
 * A component that renders a wave in either a standard wave view, a frequency
 * spectra, or spectrogram. Relies heavily on the Valve plotting package.
 * 
 * TODO: move filter method
 * 
 * 
 * @author Dan Cervelli
 */
public class WaveViewPanel extends AbstractWavePanel {
  /**
   * Constructs a WaveViewPanel with default settings.
   */
  public WaveViewPanel() {
    this(new WaveViewSettings());
  }

  /**
   * Constructs a WaveViewPanel with specified settings.
   * 
   * @param s
   *          the settings
   */
  public WaveViewPanel(WaveViewSettings s) {
    super(s);
  }

  /**
   * Constructs a WaveViewPanel set up the same as a source WaveViewPanel.
   * Used when copying a waveform to the clipboard.
   * 
   * @param p
   *          the source WaveViewPanel
   */
  public WaveViewPanel(WaveViewPanel p) {
    super(p);
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    settings.cycleType();
  }
}
