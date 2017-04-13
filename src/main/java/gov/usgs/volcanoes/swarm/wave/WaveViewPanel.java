package gov.usgs.volcanoes.swarm.wave;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

/**
 * A component that renders a wave in either a standard wave view, a frequency spectra, or
 * spectrogram. Relies heavily on the Valve plotting package.
 * 
 * <p>TODO: move filter method
 * 
 * 
 * @author Dan Cervelli
 */
public class WaveViewPanel extends AbstractWavePanel {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a WaveViewPanel with default settings.
   */
  public WaveViewPanel() {
    this(new WaveViewSettings());
  }

  /**
   * Constructs a WaveViewPanel with specified settings.
   * 
   * @param s the settings
   */
  public WaveViewPanel(WaveViewSettings s) {
    super(s);
  }

  /**
   * Constructs a WaveViewPanel set up the same as a source WaveViewPanel. Used when copying a
   * waveform to the clipboard.
   * 
   * @param p the source WaveViewPanel
   */
  public WaveViewPanel(WaveViewPanel p) {
    super(p);
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    settings.cycleType();
  }

  private void paintMark(Graphics2D g2, double j2k) {
    if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime) {
      return;
    }

    double[] t = getTranslation();
    if (t == null) {
      return;
    }

    double x = (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

    GeneralPath gp = new GeneralPath();
    gp.moveTo((float) x, yOffset);
    gp.lineTo((float) x - 5, yOffset - 7);
    gp.lineTo((float) x + 5, yOffset - 7);
    gp.closePath();
    g2.setPaint(Color.GREEN);
    g2.fill(gp);
    g2.setColor(DARK_GREEN);
    g2.draw(gp);
  }

  @Override
  protected void annotateImage(Graphics2D g2) {
    if (!Double.isNaN(mark1)) {
      paintMark(g2, mark1);
    }

    if (!Double.isNaN(mark2)) {
      paintMark(g2, mark2);
    }

  }

  @Override
  protected void processRightMouseRelease(MouseEvent e) {
    // do nothing
  }

}
